package com.kb.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 自定义业务指标收集器
 * <p>
 * 通过 Micrometer 暴露给 Prometheus，可在 Grafana 中可视化
 * </p>
 * <p>
 * 关键指标：
 * <ul>
 *   <li>{@code kb.qa.requests} — 问答请求计数</li>
 *   <li>{@code kb.qa.latency} — 问答处理延迟</li>
 *   <li>{@code kb.qa.cache.hits} — 缓存命中次数</li>
 *   <li>{@code kb.document.uploads} — 文档上传次数</li>
 *   <li>{@code kb.document.processing} — 文档处理耗时</li>
 *   <li>{@code kb.retrieval.latency} — 检索延迟</li>
 *   <li>{@code kb.token.consumption} — Token 消耗统计</li>
 * </ul>
 * </p>
 * @author forever-king
 */
@Component
public class BusinessMetrics {

    private final MeterRegistry registry;

    // ---- Counters ----
    private final Counter qaRequestCounter;
    private final Counter qaCacheHitCounter;
    private final Counter qaCacheMissCounter;
    private final Counter documentUploadCounter;
    private final Counter documentFailureCounter;
    private final Counter tokenInputCounter;
    private final Counter tokenOutputCounter;

    // ---- Timers ----
    private final Timer qaLatencyTimer;
    private final Timer retrievalLatencyTimer;
    private final Timer documentProcessingTimer;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;

        this.qaRequestCounter = Counter.builder("kb.qa.requests")
                .description("Q&A request count")
                .tag("type", "total")
                .register(registry);

        this.qaCacheHitCounter = Counter.builder("kb.qa.cache")
                .description("Q&A cache hit count")
                .tag("result", "hit")
                .register(registry);

        this.qaCacheMissCounter = Counter.builder("kb.qa.cache")
                .description("Q&A cache miss count")
                .tag("result", "miss")
                .register(registry);

        this.documentUploadCounter = Counter.builder("kb.document.uploads")
                .description("Document upload count")
                .register(registry);

        this.documentFailureCounter = Counter.builder("kb.document.failures")
                .description("Document processing failure count")
                .register(registry);

        this.tokenInputCounter = Counter.builder("kb.token.consumption")
                .description("Token consumption")
                .tag("direction", "input")
                .register(registry);

        this.tokenOutputCounter = Counter.builder("kb.token.consumption")
                .description("Token consumption")
                .tag("direction", "output")
                .register(registry);

        this.qaLatencyTimer = Timer.builder("kb.qa.latency")
                .description("Q&A end-to-end latency")
                .register(registry);

        this.retrievalLatencyTimer = Timer.builder("kb.retrieval.latency")
                .description("Hybrid retrieval latency")
                .register(registry);

        this.documentProcessingTimer = Timer.builder("kb.document.processing")
                .description("Document processing pipeline latency")
                .register(registry);
    }

    // ---- QA Metrics ----

    public void recordQaRequest() {
        qaRequestCounter.increment();
    }

    public void recordCacheHit() {
        qaCacheHitCounter.increment();
    }

    public void recordCacheMiss() {
        qaCacheMissCounter.increment();
    }

    public void recordQaLatency(long durationMs) {
        qaLatencyTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    // ---- Document Metrics ----

    public void recordDocumentUpload() {
        documentUploadCounter.increment();
    }

    public void recordDocumentProcessing(long durationMs) {
        documentProcessingTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordDocumentFailure() {
        documentFailureCounter.increment();
    }

    // ---- Retrieval Metrics ----

    public void recordRetrievalLatency(long durationMs) {
        retrievalLatencyTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    // ---- Token Metrics ----

    public void recordTokenInput(int count) {
        tokenInputCounter.increment(count);
    }

    public void recordTokenOutput(int count) {
        tokenOutputCounter.increment(count);
    }
}
