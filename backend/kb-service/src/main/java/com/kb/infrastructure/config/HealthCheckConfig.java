package com.kb.infrastructure.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

/**
 * 自定义健康检查指示器。
 * <p>
 * 检查各中间件连通性，通过 Actuator {@code /actuator/health} 端点暴露。
 * 若中间件不可用时返回 {@code DOWN} 状态及错误详情。
 * </p>
 * @author forever-king
 */
@Configuration
public class HealthCheckConfig {

    @Bean
    public HealthIndicator redisHealth(RedisConnectionFactory redisConnectionFactory) {
        return () -> {
            try {
                var conn = redisConnectionFactory.getConnection();
                String pong = conn.ping();
                conn.close();
                if ("PONG".equals(pong)) {
                    return Health.up().withDetail("type", "Redis").build();
                }
                return Health.down().withDetail("type", "Redis")
                        .withDetail("error", "Unexpected ping response").build();
            } catch (Exception e) {
                return Health.down().withDetail("type", "Redis")
                        .withDetail("error", e.getMessage()).build();
            }
        };
    }

    @Bean
    public HealthIndicator rabbitMqHealth(ConnectionFactory rabbitConnectionFactory) {
        return () -> {
            try {
                var conn = rabbitConnectionFactory.createConnection();
                conn.close();
                return Health.up().withDetail("type", "RabbitMQ").build();
            } catch (Exception e) {
                return Health.down().withDetail("type", "RabbitMQ")
                        .withDetail("error", e.getMessage()).build();
            }
        };
    }

    @Bean
    public HealthIndicator diskSpaceHealth() {
        return () -> {
            var file = new java.io.File(".");
            long freeSpace = file.getFreeSpace();
            long threshold = 100 * 1024 * 1024; // 100 MB
            if (freeSpace >= threshold) {
                return Health.up().withDetail("type", "DiskSpace")
                        .withDetail("freeMB", freeSpace / (1024 * 1024)).build();
            }
            return Health.down().withDetail("type", "DiskSpace")
                    .withDetail("freeMB", freeSpace / (1024 * 1024))
                    .withDetail("thresholdMB", threshold / (1024 * 1024)).build();
        };
    }
}
