package com.kb;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 演示模式数据库重建开关（仅服务器开启）
 * <p>
 * 服务器当作"纯演示/可再生"环境时，每次启动先 {@code flyway.clean()} 清空整个 knowledge_base，
 * 再按当前迁移重建——消除老库漂移/checksum 不一致问题（AI 会话、上传文档等不保留）。
 * </p>
 * <ul>
 *   <li>开启：部署侧 compose 注入环境变量 {@code APP_DB_RESET_ON_STARTUP=true}；
 *   <li>关闭（默认）：本地开发不动数据，仅 migrate。
 * </ul>
 *
 * @author forever-king
 */
@Configuration
public class DbResetConfig {

    @Bean
    public FlywayMigrationStrategy dbResetFlywayStrategy(
            @Value("${app.db.reset-on-startup:false}") boolean resetOnStartup) {
        return flyway -> {
            if (resetOnStartup) {
                flyway.clean();
            }
            flyway.migrate();
        };
    }
}
