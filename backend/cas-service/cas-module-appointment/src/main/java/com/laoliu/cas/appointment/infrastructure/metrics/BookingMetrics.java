package com.laoliu.cas.appointment.infrastructure.metrics;

import com.laoliu.cas.appointment.application.service.AuditSource;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.Map;
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
    private static final String SOURCE_ADMIN = "admin";
    private static final String SOURCE_TEACHER = "teacher";

    private final MeterRegistry registry;

    private final Counter createdCounter;
    private final Counter cancelledCounter;

    /** 1.10：审核计数/耗时按 result×source 全组合构造期预建，热点路径不再 builder+register 查找 */
    private final Map<AuditKey, Counter> auditCounters;
    private final Map<AuditKey, Timer> auditTimers;

    /** 冲突拦截计数按已知 reason 预建；出现新原因时回退惰性注册（Micrometer 返回同名既有实例） */
    private final Map<String, Counter> conflictCounters;

    public BookingMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.createdCounter = Counter.builder("booking.created")
                .description("Successfully created bookings")
                .register(registry);
        this.cancelledCounter = Counter.builder("booking.cancelled")
                .description("Bookings cancelled by users")
                .register(registry);
        this.auditCounters = Map.of(
                new AuditKey(RESULT_APPROVED, SOURCE_ADMIN), auditCounter(RESULT_APPROVED, SOURCE_ADMIN),
                new AuditKey(RESULT_APPROVED, SOURCE_TEACHER), auditCounter(RESULT_APPROVED, SOURCE_TEACHER),
                new AuditKey(RESULT_REJECTED, SOURCE_ADMIN), auditCounter(RESULT_REJECTED, SOURCE_ADMIN),
                new AuditKey(RESULT_REJECTED, SOURCE_TEACHER), auditCounter(RESULT_REJECTED, SOURCE_TEACHER)
        );
        this.auditTimers = Map.of(
                new AuditKey(RESULT_APPROVED, SOURCE_ADMIN), auditTimer(RESULT_APPROVED, SOURCE_ADMIN),
                new AuditKey(RESULT_APPROVED, SOURCE_TEACHER), auditTimer(RESULT_APPROVED, SOURCE_TEACHER),
                new AuditKey(RESULT_REJECTED, SOURCE_ADMIN), auditTimer(RESULT_REJECTED, SOURCE_ADMIN),
                new AuditKey(RESULT_REJECTED, SOURCE_TEACHER), auditTimer(RESULT_REJECTED, SOURCE_TEACHER)
        );
        this.conflictCounters = Map.of(
                REASON_CAPACITY_FULL, conflictCounter(REASON_CAPACITY_FULL),
                REASON_SLOT_UNAVAILABLE, conflictCounter(REASON_SLOT_UNAVAILABLE)
        );
    }

    private Counter auditCounter(String result, String source) {
        return Counter.builder("booking.audit")
                .description("Booking audit decisions")
                .tag("result", result)
                .tag("source", source)
                .register(registry);
    }

    private Timer auditTimer(String result, String source) {
        return Timer.builder("booking.audit.duration")
                .description("Booking audit processing duration")
                .tag("result", result)
                .tag("source", source)
                .publishPercentileHistogram()
                .register(registry);
    }

    private Counter conflictCounter(String reason) {
        return Counter.builder("booking.conflict.blocked")
                .description("Booking attempts blocked by capacity or slot conflicts")
                .tag("reason", reason)
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
        AuditKey key = new AuditKey(
                approved ? RESULT_APPROVED : RESULT_REJECTED,
                source == AuditSource.TEACHER ? SOURCE_TEACHER : SOURCE_ADMIN);
        auditCounters.get(key).increment();
        auditTimers.get(key).record(durationNanos, TimeUnit.NANOSECONDS);
    }

    /** 容量满或时段冲突导致的下单拦截 */
    public void recordConflictBlocked(String reason) {
        Counter counter = conflictCounters.get(reason);
        if (counter == null) {
            // 防御：出现未预建的新原因时惰性注册（register 对同名 tag 组合返回既有实例，语义不变）
            counter = conflictCounter(reason);
        }
        counter.increment();
    }

    /** 审核指标的 tag 组合键（result + source） */
    private record AuditKey(String result, String source) {
    }
}
