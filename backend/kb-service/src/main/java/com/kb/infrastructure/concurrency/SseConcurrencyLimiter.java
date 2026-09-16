package com.kb.infrastructure.concurrency;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SSE 流式问答的全局并发上限。
 * <p>
 * 每路流式问答在 boundedElastic 线程上同步执行"检索 + LLM + Feign"，
 * 单轮耗时可达数十秒。{@code @RateLimit} 限制的是单用户请求频次，
 * 不限制同时挂起的连接总数——用户规模放大后，慢 LLM 会让大量长连接
 * 同时占住线程与下游配额。这里用公平信号量给实例级在途 SSE 数量兜底：
 * 拿不到许可的请求直接返回 503，由前端提示稍后重试，避免级联拖垮。
 *
 * @author forever-king
 */
@Component
public class SseConcurrencyLimiter {

    /** 同时在途的 SSE 问答连接上限，超出后新请求快速失败（503） */
    @Getter
    private final int maxConcurrent;

    private final Semaphore semaphore;

    /** 当前在途连接数，供 Micrometer 指标观测 */
    private final AtomicInteger activeCount = new AtomicInteger(0);

    private final MeterRegistry meterRegistry;

    public SseConcurrencyLimiter(
            @Value("${kb.sse.max-concurrent:50}") int maxConcurrent,
            MeterRegistry meterRegistry) {
        if (maxConcurrent <= 0) {
            throw new IllegalArgumentException("kb.sse.max-concurrent 必须为正整数，当前值: " + maxConcurrent);
        }
        this.maxConcurrent = maxConcurrent;
        // 公平模式：高并发下先到先得，避免个别请求长期饥饿
        this.semaphore = new Semaphore(maxConcurrent, true);
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void registerMetrics() {
        Gauge.builder("kb.sse.active.connections", activeCount, AtomicInteger::doubleValue)
                .description("当前在途的 SSE 流式问答连接数")
                .register(meterRegistry);
        Gauge.builder("kb.sse.max.connections", () -> (Number) maxConcurrent)
                .description("SSE 流式问答连接数上限")
                .register(meterRegistry);
    }

    /**
     * 尝试占用一个在途名额。
     *
     * @return true 表示获得名额，调用方最终必须调用一次 {@link #release()}；
     *         false 表示并发已满，应快速失败，禁止调用 release
     */
    public boolean tryAcquire() {
        if (semaphore.tryAcquire()) {
            activeCount.incrementAndGet();
            return true;
        }
        return false;
    }

    /** 归还一个在途名额，与 {@link #tryAcquire()} 成功一一对应 */
    public void release() {
        semaphore.release();
        activeCount.updateAndGet(v -> Math.max(0, v - 1));
    }

    public int getActiveCount() {
        return activeCount.get();
    }
}
