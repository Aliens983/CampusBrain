package com.kb.infrastructure.config;

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

    @Bean(name = "retrievalExecutor")
    public ExecutorService retrievalExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
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
