package com.laoliu.cas.server;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * 演示模式数据库重建开关（默认全关，仅纯演示环境显式开启）
 * <p>
 * 纯演示/可再生环境开启后，每次启动先 {@code flyway.clean()} 清空整个 cas_db，
 * 再按当前 V1~V8 迁移重建——彻底消除"老库漂移 / checksum 不一致 / 手动补列"类问题。
 * </p>
 * <p>三道开关（6.2/6.3：生产安全默认，缺一不执行对应动作）：</p>
 * <ul>
 *   <li>{@code APP_DB_RESET_ON_STARTUP=true}（→ {@code app.db.reset-on-startup}）：启动时 clean+migrate；</li>
 *   <li>{@code FLYWAY_CLEAN_DISABLED=false}（→ {@code spring.flyway.clean-disabled}）：放开 Flyway clean；
 *       与上一项互为双保险，单开 reset 直接 fail-fast；</li>
 *   <li>{@code APP_REDIS_FLUSH_ON_RESET=true}（→ {@code app.redis.flush-on-reset}）：
 *       重建 DB 时是否同时 {@code FLUSHDB} 清空整个 Redis DB（6.3：限流/会话/分布式锁/缓存
 *       全部受影响，此前随 DB 重置无独立开关，现默认不执行）。</li>
 * </ul>
 *
 * @author forever-king
 */
@Slf4j
@Configuration
public class DbResetConfig {

    @Bean
    public FlywayMigrationStrategy dbResetFlywayStrategy(
            @Value("${app.db.reset-on-startup:false}") boolean resetOnStartup,
            @Value("${spring.flyway.clean-disabled:true}") boolean cleanDisabled,
            @Value("${app.redis.flush-on-reset:false}") boolean flushRedisOnReset,
            RedisConnectionFactory connectionFactory) {
        // 双开关矛盾时 fail-fast：否则启动到 Flyway 回调才抛
        // "Clean is disabled" 英文底层异常，排查者很难直接联想到两个开关的配套关系（7.3.8）。
        if (resetOnStartup && cleanDisabled) {
            throw new IllegalStateException(
                    "APP_DB_RESET_ON_STARTUP=true 必须同时设置 FLYWAY_CLEAN_DISABLED=false，"
                            + "否则 spring.flyway.clean-disabled=true 会拒绝执行 flyway.clean()，启动中止。");
        }
        return flyway -> {
            if (!resetOnStartup) {
                flyway.migrate();
                return;
            }
            flyway.clean();
            flyway.migrate();
            // 6.3：FLUSHDB 影响面是整个 Redis DB（限流计数、登录/重置锁定、会话、文档锁、
            // 业务缓存），不得由 DB 重建开关隐式触发，必须由第三道开关独立放行。
            if (flushRedisOnReset) {
                RedisConnection conn = connectionFactory.getConnection();
                try {
                    conn.flushDb();
                } finally {
                    conn.close();
                }
                log.warn("演示模式：cas_db 已重建且 Redis DB 已 FLUSHDB（APP_REDIS_FLUSH_ON_RESET=true）");
            } else {
                // DB 已重建而旧缓存残留可能导致跨版本序列化格式异常（2026-09-08），
                // 显式跳过必须在启动日志中可见，便于演示环境排查。
                log.warn("演示模式：cas_db 已重建，但未清空 Redis（APP_REDIS_FLUSH_ON_RESET 未开启）；"
                        + "如遇缓存序列化异常请开启该开关");
            }
        };
    }
}
