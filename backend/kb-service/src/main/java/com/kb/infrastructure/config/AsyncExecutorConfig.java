package com.kb.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;

/**
 * Spring 管理的线程池配置，替代项目中手动创建的 ExecutorService
 * @author forever-king
 */
@Configuration
public class AsyncExecutorConfig {

    /**
     * 检索线程池（4.4 深度审查 P1 修正）。
     * <p>
     * 旧配置 core=2 / max=4 / queue=100：ThreadPoolTaskExecutor 只有队列<b>打满</b>才扩容到 max，
     * 100 容量队列几乎永不填满 → 实际常驻仅 2 线程；每次问答检索向池提交 2 个任务
     * （关键词 + 向量），真实检索并发 ≈ 1，远低于 {@code kb.sse.max-concurrent=50}，成为 SSE 吞吐瓶颈。
     * </p>
     * <p>
     * 现按需求重配：每个流式问答在检索阶段瞬时占用 2 个池线程，而占用时长仅为
     * ES/Qdrant 单次查询 RTT（远短于 SSE 连接存活时长），故线程数 ≈ 在途流式问答数。
     * 默认把 core=max= 与 SSE 并发上限对齐（一次问答最多同时占 2× 线程，但两任务几乎同吞同吐），
     * 不设大队列以免排队放大时延；突发超额任务被拒绝时由 HybridRetriever 的
     * 超时/降级路径兜底（该边返回空结果，问答链路不因此中断）。
     * </p>
     *
     * @param sseMaxConcurrent {@code kb.sse.max-concurrent} 单实例在途 SSE 上限
     * @param poolSize         {@code kb.retrieval.pool-size} 显式覆盖；0=跟随 SSE 上限
     */
    @Bean(name = "retrievalExecutor")
    public ExecutorService retrievalExecutor(
            @Value("${kb.sse.max-concurrent:50}") int sseMaxConcurrent,
            @Value("${kb.retrieval.pool-size:0}") int poolSize) {
        int size = poolSize > 0 ? poolSize : sseMaxConcurrent;
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(size);
        executor.setMaxPoolSize(size);
        executor.setQueueCapacity(128);
        executor.setThreadNamePrefix("retrieval-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor.getThreadPoolExecutor();
    }

    /**
     * 文档向量化专用线程池：同一文档的多个 embedding 批次在该池上并发调用模型 API。
     * 大小固定（按对 embedding API 的并发预算配置），队列仅需容纳单文档的批次数，
     * 消费端多文档并发已由 RabbitMQ listener 的 concurrency 控制，此处不做无界缓冲。
     */
    @Bean(name = "embeddingExecutor")
    public ExecutorService embeddingExecutor(
            @org.springframework.beans.factory.annotation.Value(
                    "${kb.embedding.parallelism:4}") int parallelism) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(parallelism);
        executor.setMaxPoolSize(parallelism);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("embedding-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor.getThreadPoolExecutor();
    }
}
