package com.kb.infrastructure.concurrency;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link SseConcurrencyLimiter} 单元测试：许可上限、释放复用、非法配置。
 *
 * @author forever-king
 */
@DisplayName("SSE 并发限流器测试")
class SseConcurrencyLimiterTest {

    @Test
    @DisplayName("占满上限后新请求被拒绝，释放名额后可再次获取")
    void shouldRejectWhenFullAndRecoverAfterRelease() {
        SseConcurrencyLimiter limiter = new SseConcurrencyLimiter(3, new SimpleMeterRegistry());

        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.getActiveCount()).isEqualTo(3);

        assertThat(limiter.tryAcquire()).isFalse();
        assertThat(limiter.getActiveCount()).as("拒绝时不应占用名额").isEqualTo(3);

        limiter.release();
        assertThat(limiter.getActiveCount()).isEqualTo(2);
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.getActiveCount()).isEqualTo(3);
        assertThat(limiter.tryAcquire()).isFalse();
    }

    @Test
    @DisplayName("初始状态无在途连接")
    void shouldStartEmpty() {
        SseConcurrencyLimiter limiter = new SseConcurrencyLimiter(2, new SimpleMeterRegistry());
        assertThat(limiter.getActiveCount()).isZero();
        assertThat(limiter.getMaxConcurrent()).isEqualTo(2);
    }

    @Test
    @DisplayName("上限必须为正整数")
    void shouldRejectInvalidLimit() {
        assertThatThrownBy(() -> new SseConcurrencyLimiter(0, new SimpleMeterRegistry()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("指标注册不重复报错")
    void shouldRegisterGauges() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SseConcurrencyLimiter limiter = new SseConcurrencyLimiter(2, registry);
        limiter.registerMetrics();

        assertThat(registry.find("kb.sse.active.connections").gauge()).isNotNull();
        assertThat(registry.find("kb.sse.max.connections").gauge()).isNotNull();
    }
}
