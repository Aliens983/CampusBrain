package com.laoliu.cas.server;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DbResetConfig 三道开关护栏单元测试（6.2/6.3）。
 * <p>
 * 不启动 Spring 上下文、不连真实 Redis/Flyway，直接驱动
 * {@link FlywayMigrationStrategy}：默认安全（只 migrate）、clean 与 flushDb
 * 必须各自被显式开关放行。
 *
 * @author forever-king
 */
class DbResetConfigTest {

    private final DbResetConfig config = new DbResetConfig();

    private Flyway flyway;
    private RedisConnectionFactory connectionFactory;
    private RedisConnection redisConnection;

    private void initRedisMocks() {
        connectionFactory = mock(RedisConnectionFactory.class);
        redisConnection = mock(RedisConnection.class);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);
    }

    private void run(boolean reset, boolean cleanDisabled, boolean flushRedis) {
        initRedisMocks();
        flyway = mock(Flyway.class);
        FlywayMigrationStrategy strategy =
                config.dbResetFlywayStrategy(reset, cleanDisabled, flushRedis, connectionFactory);
        strategy.migrate(flyway);
    }

    @Test
    @DisplayName("默认（reset=false）：只 migrate，不 clean、不碰 Redis")
    void defaults_migrateOnly() {
        run(false, true, false);

        verify(flyway).migrate();
        verify(flyway, never()).clean();
        verify(connectionFactory, never()).getConnection();
        verify(redisConnection, never()).flushDb();
    }

    @Test
    @DisplayName("reset=true 但 clean-disabled 未放开：fail-fast，不执行任何动作")
    void resetWithoutCleanPermission_failsFast() {
        initRedisMocks();
        flyway = mock(Flyway.class);

        assertThatThrownBy(() -> config.dbResetFlywayStrategy(true, true, true, connectionFactory))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FLYWAY_CLEAN_DISABLED");
        verify(flyway, never()).clean();
        verify(flyway, never()).migrate();
        verify(redisConnection, never()).flushDb();
    }

    @Test
    @DisplayName("6.3 reset=true + clean 放开，但 Redis 开关未开：clean+migrate 执行，FLUSHDB 不执行")
    void resetWithoutRedisSwitch_skipsFlushDb() {
        run(true, false, false);

        verify(flyway).clean();
        verify(flyway).migrate();
        verify(connectionFactory, never()).getConnection();
        verify(redisConnection, never()).flushDb();
    }

    @Test
    @DisplayName("三道开关全开：clean+migrate 后恰好一次 FLUSHDB 且连接关闭")
    void allSwitchesOn_flushesOnceAndCloses() {
        run(true, false, true);

        verify(flyway).clean();
        verify(flyway).migrate();
        verify(redisConnection).flushDb();
        verify(redisConnection).close();
    }

    @Test
    @DisplayName("仅开 Redis 开关不开 reset：只 migrate，FLUSHDB 不执行（开关不越权）")
    void redisSwitchAlone_doesNothing() {
        run(false, true, true);

        verify(flyway).migrate();
        verify(flyway, never()).clean();
        verify(redisConnection, never()).flushDb();
    }
}
