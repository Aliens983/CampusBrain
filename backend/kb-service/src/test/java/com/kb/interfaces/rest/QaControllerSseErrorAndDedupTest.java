package com.kb.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.application.service.IQaApplicationService;
import com.kb.domain.rag.CancellationToken;
import com.kb.infrastructure.concurrency.SseConcurrencyLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SSE 问答：终态错误发流内 error 事件（不触发 EventSource 自动重连）与 requestId 在途幂等。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SSE 终态错误与在途幂等")
class QaControllerSseErrorAndDedupTest {

    @Mock private IQaApplicationService qaService;
    @Mock private SseConcurrencyLimiter limiter;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("问答服务抛异常：发命名 error 事件 + DONE 后正常 complete，不走传输层 error")
    @SuppressWarnings("unchecked")
    void shouldEmitTerminalErrorEventInsteadOfTransportError() {
        when(limiter.tryAcquire()).thenReturn(true);
        when(qaService.askStreaming(anyString(), anyString(), any(), any(CancellationToken.class),
                any(Consumer.class), any(Consumer.class), any(Consumer.class), any(Consumer.class)))
                .thenThrow(new RuntimeException("LLM 供应商 500"));
        QaController controller = new QaController(qaService, new ObjectMapper(), limiter, redisTemplate);

        List<ServerSentEvent<?>> events = controller
                .askStreaming("什么是 RAG？", "s1", null)
                .collectList()
                .block(Duration.ofSeconds(5));

        assertThat(events).isNotNull();
        assertThat(events).extracting(ServerSentEvent::event)
                .contains("error");
        assertThat(String.valueOf(events.get(events.size() - 1).data())).isEqualTo("[DONE]");
        verify(limiter).release();
    }

    @Test
    @DisplayName("同 requestId 在途重复请求：返回 409，不调问答服务、不占用/释放并发名额")
    void shouldRejectDuplicatedInflightRequest() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class)))
                .thenReturn(false);
        QaController controller = new QaController(qaService, new ObjectMapper(), limiter, redisTemplate);

        // 重复检测在 Flux 订阅前同步抛出（响应头尚未提交，按普通 409 错误处理）
        assertThatThrownBy(() -> controller.askStreaming("什么是 RAG？", "s1", "req-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        verify(qaService, never()).askStreaming(
                anyString(), anyString(), any(), any(), any(), any(), any(), any());
        verify(limiter, never()).tryAcquire();
        verify(limiter, never()).release();
    }

    @Test
    @DisplayName("Redis 幂等闸故障时 fail-open：正常问答不受影响，流终止后释放名额")
    @SuppressWarnings("unchecked")
    void shouldFailOpenWhenDedupRedisDown() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("redis down"));
        when(limiter.tryAcquire()).thenReturn(true);
        when(qaService.askStreaming(anyString(), anyString(), any(), any(CancellationToken.class),
                any(Consumer.class), any(Consumer.class), any(Consumer.class), any(Consumer.class)))
                .thenAnswer(inv -> {
                    Consumer<String> onToken = inv.getArgument(4);
                    onToken.accept("答案");
                    return "答案";
                });
        QaController controller = new QaController(qaService, new ObjectMapper(), limiter, redisTemplate);

        List<ServerSentEvent<?>> events = controller
                .askStreaming("什么是 RAG？", "s1", "req-2")
                .collectList()
                .block(Duration.ofSeconds(5));

        assertThat(String.valueOf(events.get(0).data())).isEqualTo("答案");
        verify(limiter).release();
    }
}
