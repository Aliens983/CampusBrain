package com.kb.domain.rag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CancellationToken} 协作式取消语义测试（12-07）。
 *
 * @author forever-king
 */
@DisplayName("流式取消令牌测试")
class CancellationTokenTest {

    @Test
    @DisplayName("未取消状态：isCancelled=false，动作只注册不执行")
    void notCancelledByDefault() {
        CancellationToken token = new CancellationToken();
        AtomicInteger runs = new AtomicInteger();

        token.onAbort(runs::incrementAndGet);

        assertThat(token.isCancelled()).isFalse();
        assertThat(runs.get()).isZero();
    }

    @Test
    @DisplayName("取消后：已注册动作执行一次，cancel 幂等")
    void cancelRunsRegisteredActionsOnce() {
        CancellationToken token = new CancellationToken();
        AtomicInteger a = new AtomicInteger();
        AtomicInteger b = new AtomicInteger();
        token.onAbort(a::incrementAndGet);
        token.onAbort(b::incrementAndGet);

        token.cancel();
        token.cancel();
        token.cancel();

        assertThat(token.isCancelled()).isTrue();
        assertThat(a.get()).isEqualTo(1);
        assertThat(b.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("晚注册：取消之后才注册的动作必须立即执行，避免漏中止在途 HTTP 调用")
    void lateRegistrationRunsImmediately() {
        CancellationToken token = new CancellationToken();
        token.cancel();

        AtomicInteger runs = new AtomicInteger();
        token.onAbort(runs::incrementAndGet);

        assertThat(runs.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("中止动作抛异常不影响后续动作与取消状态")
    void abortActionFailureIsSwallowed() {
        CancellationToken token = new CancellationToken();
        AtomicInteger after = new AtomicInteger();
        token.onAbort(() -> { throw new IllegalStateException("abort 失败"); });
        token.onAbort(after::incrementAndGet);

        token.cancel();

        assertThat(after.get()).isEqualTo(1);
        assertThat(token.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("none() 共享令牌永不为取消态")
    void noneTokenNeverCancelled() {
        assertThat(CancellationToken.none().isCancelled()).isFalse();
        CancellationToken.none().cancel();
        assertThat(CancellationToken.none().isCancelled()).isFalse();
    }
}
