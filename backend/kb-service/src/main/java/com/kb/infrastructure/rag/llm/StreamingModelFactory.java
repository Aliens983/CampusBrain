package com.kb.infrastructure.rag.llm;

import com.kb.domain.rag.CancellationToken;
import dev.ai4j.openai4j.OpenAiClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 流式模型工厂：单例持有 {@link OpenAiClient}（内含 OkHttp 连接池/线程池），
 * 每次流式问答新建一个轻量的 {@link CancellableOpenAiStreamingChatModel} 绑定本次
 * 请求的 {@link CancellationToken}。
 *
 * @author forever-king
 */
@Slf4j
@Component
public class StreamingModelFactory {

    @Value("${langchain4j.openai.chat-model.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${langchain4j.openai.api-key:}")
    private String apiKey;

    @Value("${langchain4j.openai.chat-model.model-name:deepseek-chat}")
    private String modelName;

    @Value("${langchain4j.openai.chat-model.temperature:0.3}")
    private Double temperature;

    @Value("${langchain4j.openai.chat-model.max-tokens:2048}")
    private Integer maxTokens;

    @Value("${langchain4j.openai.chat-model.timeout:60s}")
    private Duration timeout;

    private OpenAiClient client;

    @PostConstruct
    void init() {
        this.client = OpenAiClient.builder()
                .baseUrl(baseUrl)
                .openAiApiKey(apiKey == null || apiKey.isBlank() ? "missing-api-key" : apiKey)
                .callTimeout(timeout)
                .connectTimeout(timeout)
                .readTimeout(timeout)
                .writeTimeout(timeout)
                .build();
        log.info("Cancellable streaming chat model factory initialized: baseUrl={}, model={}",
                baseUrl, modelName);
    }

    /**
     * 创建绑定本次请求取消令牌的流式模型。
     */
    public CancellableOpenAiStreamingChatModel create(CancellationToken token) {
        return new CancellableOpenAiStreamingChatModel(
                client, modelName, temperature, maxTokens,
                token == null ? CancellationToken.none() : token);
    }
}
