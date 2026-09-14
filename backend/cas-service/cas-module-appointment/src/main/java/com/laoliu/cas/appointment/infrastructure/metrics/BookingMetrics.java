package com.laoliu.cas.appointment.infrastructure.metrics;

import com.laoliu.cas.appointment.application.service.AuditSource;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 预约业务指标（5.2.2）。
 * <p>
 * 承载全部预约写操作的 CAS 此前没有任何业务指标，无法回答
 * 「待审核积压多少」「超卖拦截几次」「审核平均时长」「邮件成功率」。
 * 本类统一收口预约生命周期指标，经 /actuator/prometheus 暴露：
 * </p>
 * <ul>
 *   <li>{@code booking_created_total} — 创建成功的预约单数量；</li>
 *   <li>{@code booking_audit_total} — 审核终态数量（tag: result/source）；</li>
 *   <li>{@code booking_cancelled_total} — 用户取消数量；</li>
 *   <li>{@code booking_conflict_blocked_total} — 容量满/时段冲突拦截次数（tag: reason）；</li>
 *   <li>{@code booking_audit_duration_seconds} — 审核耗时分布（tag: result/source）。</li>
 * </ul>
 *
 * @author forever-king
 */
@Component
public class BookingMetrics {

    /** 冲突原因：服务容量已满（乐观锁扣减失败） */
    public static final String REASON_CAPACITY_FULL = "capacity_full";

    /** 冲突原因：时段刚被他人占用/不可用 */
    public static final String REASON_SLOT_UNAVAILABLE = "slot_unavailable";

    private static final String RESULT_APPROVED = "approved";
    private static final String RESULT_REJECTED = "rejected";

    private final MeterRegistry registry;

    private final Counter createdCounter;
    private final Counter cancelledCounter;

    public BookingMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.createdCounter = Counter.builder("booking.created")
                .description("Successfully created bookings")
                .register(registry);
        this.cancelledCounter = Counter.builder("booking.cancelled")
                .description("Bookings cancelled by users")
                .register(registry);
    }

    /** 预约下单成功：按真实新建订单数计数（一单多服务时大于 1） */
    public void recordCreated(int count) {
        if (count > 0) {
            createdCounter.increment(count);
        }
    }

    public void recordCancelled(int count) {
        if (count > 0) {
            cancelledCounter.increment(count);
        }
    }

    /**
     * 记录一次审核终态：计数 + 耗时。
     *
     * @param approved       是否通过
     * @param source         审核发起方（管理员/教师）
     * @param durationNanos  审核处理耗时（纳秒）
     */
    public void recordAudit(boolean approved, AuditSource source, long durationNanos) {
        String result = approved ? RESULT_APPROVED : RESULT_REJECTED;
        String reviewer = source == AuditSource.TEACHER ? "teacher" : "admin";
        Counter.builder("booking.audit")
                .description("Booking audit decisions")
                .tag("result", result)
                .tag("source", reviewer)
                .register(registry)
                .increment();
        Timer.builder("booking.audit.duration")
                .description("Booking audit processing duration")
                .tag("result", result)
                .tag("source", reviewer)
                .publishPercentileHistogram()
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    /** 容量满或时段冲突导致的下单拦截 */
    public void recordConflictBlocked(String reason) {
        Counter.builder("booking.conflict.blocked")
                .description("Booking attempts blocked by capacity or slot conflicts")
                .tag("reason", reason)
                .register(registry)
                .increment();
    }
}
