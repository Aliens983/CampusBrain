package com.kb.infrastructure.rag.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A-07：熔断 fallback 统一模板 {@code llmFallback(scene, consumer, t)} 行为测试。
 * <p>
 * 四条 @CircuitBreaker 链路的 fallback 方法全部收敛到该模板，这里直接验证模板：
 * 同步链路（consumer=null）只返回文案；流式链路还必须把兜底文案推给消费者；
 * 失败原因（异常类型+消息）由日志携带，面向用户的文案不含任何内部细节。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LLM 熔断 fallback 统一模板测试（A-07）")
class LlmFallbackTemplateTest {

    private static final String FALLBACK_MESSAGE = "AI 服务暂时不可用，请稍后重试。";

    private Object invokeTemplate(LangChain4jLlmService service, String scene,
                                  Consumer<String> consumer, Throwable t) throws Exception {
        Method m = LangChain4jLlmService.class.getDeclaredMethod(
                "llmFallback", String.class, Consumer.class, Throwable.class);
        m.setAccessible(true);
        return m.invoke(service, scene, consumer, t);
    }

    @Test
    @DisplayName("流式链路：返回兜底文案且推送给 token 消费者")
    void streamingPushesMessageToConsumer() throws Exception {
        LangChain4jLlmService service = new LangChain4jLlmService(null, null, null, null);
        List<String> pushed = new ArrayList<>();
        Throwable cause = new RuntimeException(
                "CircuitBreaker 'llmService' is OPEN and does not permit further calls");

        Object result = invokeTemplate(service, "rag-streaming", pushed::add, cause);

        assertThat(result).isEqualTo(FALLBACK_MESSAGE);
        assertThat(pushed).containsExactly(FALLBACK_MESSAGE);
    }

    @Test
    @DisplayName("同步链路：consumer 为 null 时只返回文案，不外泄失败原因")
    void syncReturnsMessageWithoutConsumer() throws Exception {
        LangChain4jLlmService service = new LangChain4jLlmService(null, null, null, null);
        Throwable cause = new LlmUnavailableException("LLM 主备供应商均不可用: 通义千问");

        Object result = invokeTemplate(service, "rag-sync", null, cause);

        assertThat(result).isEqualTo(FALLBACK_MESSAGE);
        assertThat((String) result).doesNotContain("通义千问").doesNotContain("LlmUnavailableException");
    }

    @Test
    @DisplayName("四处 @CircuitBreaker 的 fallbackMethod 均在类中存在（签名以 Throwable 收尾）")
    void allFallbackMethodsExist() {
        for (String name : List.of("unavailableAnswer", "streamingUnavailable",
                "toolsStreamingUnavailable", "directStreamingUnavailable")) {
            boolean found = java.util.Arrays.stream(LangChain4jLlmService.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals(name)
                            && m.getParameterTypes().length > 0
                            && m.getParameterTypes()[m.getParameterTypes().length - 1] == Throwable.class);
            assertThat(found).as("fallback 方法 %s 必须存在且末参为 Throwable", name).isTrue();
        }
    }
}
