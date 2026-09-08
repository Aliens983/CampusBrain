package com.laoliu.cas.server;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * 演示模式数据库重建开关（仅服务器开启）
 * <p>
 * 服务器当作"纯演示/可再生"环境时，每次启动先 {@code flyway.clean()} 清空整个 cas_db，
 * 再按当前 V1~V5 迁移重建——彻底消除"老库漂移 / checksum 不一致 / 手动补列"类问题。
 * </p>
 * <ul>
 *   <li>开启：部署侧 compose 注入环境变量 {@code APP_DB_RESET_ON_STARTUP=true}（→ {@code app.db.reset-on-startup}）；
 *   <li>关闭（默认）：本地开发不动数据，行为与之前一致（仅 migrate）。
 * </ul>
 *
 * @author forever-king
 */
@Configuration
public class DbResetConfig {

    @Bean
    public FlywayMigrationStrategy dbResetFlywayStrategy(
            @Value("${app.db.reset-on-startup:false}") boolean resetOnStartup,
            RedisConnectionFactory connectionFactory) {
        return flyway -> {
            if (resetOnStartup) {
                flyway.clean();
                flyway.migrate();
                // DB 每次重建后缓存里仍是旧代码/旧序列化格式的键 → 读侧 SerializationException(2026-09-08)。
                // 重置模式下一次清空 Redis，避免跨版本缓存格式残留。
                RedisConnection conn = connectionFactory.getConnection();
                try {
                    conn.flushDb();
                } finally {
                    conn.close();
                }
                return;
            }
            flyway.migrate();
        };
    }
}
