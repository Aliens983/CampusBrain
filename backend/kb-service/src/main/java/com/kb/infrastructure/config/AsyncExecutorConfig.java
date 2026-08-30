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
}
