package com.laoliu.cas.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步执行配置
 * <p>
 * 启用 {@code @EnableAsync} 后 {@code @Async} 才真正生效。此前全仓库没有该注解，
 * {@link com.laoliu.cas.infra.application.service.EmailService#sendEmail} 上的
 * {@code @Async} 形同虚设，方法在调用方线程同步执行——而调用方是
 * {@code auditPass} / {@code auditReject} 这类 {@code @Transactional} 方法，
 * 等于在数据库事务里做 SMTP(SSL 465) 网络调用，SMTP 慢会成倍拉长事务并长期占用连接。
 * <p>
 * 这里显式声明线程池而非使用默认的 {@code SimpleAsyncTaskExecutor}：
 * 后者每次提交都新建线程且无上限，异常流量下会直接打爆线程数。
 *
 * @author forever-king
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /** 线程池名前缀，便于日志与线程转储定位 */
    private static final String THREAD_NAME_PREFIX = "cas-async-";

    @Bean(name = "taskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 邮件发送是 IO 密集型，核心线程按 CPU 核数留余量即可
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
        // 队列满且线程数达上限时由调用方线程执行，配合下面的拒绝策略避免静默丢任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待在途任务结束再关闭，避免应用停机瞬间丢掉"审核通过"通知邮件
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
