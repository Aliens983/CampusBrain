package com.kb.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.application.service.IQaApplicationService;
import com.kb.infrastructure.concurrency.SseConcurrencyLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SSE 问答接口的全局限流行为测试。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SSE 问答接口并发限流测试")
class QaControllerSseConcurrencyTest {

    @Mock private IQaApplicationService qaService;
    @Mock private SseConcurrencyLimiter limiter;

    @Test
    @DisplayName("并发名额已满：返回 503，不调用问答服务，也不释放名额")
    void shouldReturn503WhenConcurrencyFull() {
        when(limiter.tryAcquire()).thenReturn(false);
        QaController controller = new QaController(qaService, new ObjectMapper(), limiter, null);

        Flux<ServerSentEvent<?>> flux = controller.askStreaming("什么是 RAG？", "s1", null);

        assertThatThrownBy(() -> flux.blockLast(Duration.ofSeconds(2)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("503");
        verify(qaService, never()).askStreaming(
                anyString(), anyString(), any(), any(), any(), any(), any(), any());
        verify(limiter, never()).release();
    }

    @Test
    @DisplayName("获得名额：正常产出 token 与结束信号，流终止后释放名额")
    @SuppressWarnings("unchecked")
    void shouldStreamAndReleaseWhenPermitAcquired() {
        when(limiter.tryAcquire()).thenReturn(true);
        when(qaService.askStreaming(anyString(), anyString(), any(), any(),
                any(Consumer.class), any(Consumer.class), any(Consumer.class), any(Consumer.class)))
                .thenAnswer(inv -> {
                    // 8 参重载：query(0)、sessionId(1)、userId(2)、CancellationToken(3)、onToken(4)
                    Consumer<String> onToken = inv.getArgument(4);
                    onToken.accept("答案片段");
                    return "完整答案";
                });
        QaController controller = new QaController(qaService, new ObjectMapper(), limiter, null);

        List<ServerSentEvent<?>> events = controller
                .askStreaming("什么是 RAG？", "s1", null)
                .collectList()
                .block(Duration.ofSeconds(5));

        assertThat(events).isNotNull();
        assertThat(events).hasSize(2);
        assertThat(String.valueOf(events.get(0).data())).isEqualTo("答案片段");
        assertThat(String.valueOf(events.get(1).data())).isEqualTo("[DONE]");
        // doFinally 在终止信号之后异步触发，需留等待窗口，避免与有界弹性线程调度竞态
        verify(limiter, timeout(2000)).release();
    }
}
