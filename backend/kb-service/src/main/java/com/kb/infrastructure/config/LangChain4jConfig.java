package com.kb.infrastructure.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LLM & Embedding 模型配置
 * <p>
 * Chat 模型和 Embedding 模型可以分别使用不同的 API 提供商
 * 例如：Chat 用 DeepSeek，Embedding 用硅基流动或本地模型
 * </p>
 *
 * @author forever-king
 */
@Configuration
public class LangChain4jConfig {

    // ==================== Chat 模型配置 ====================

    /** Chat API 密钥 */
    @Value("${langchain4j.openai.chat-model.api-key:${langchain4j.openai.api-key}}")
    private String chatApiKey;

    /** Chat API 基础地址 */
    @Value("${langchain4j.openai.chat-model.base-url:https://api.openai.com}")
    private String chatBaseUrl;

    /** Chat 模型名称 */
    @Value("${langchain4j.openai.chat-model.model-name:gpt-4o}")
    private String chatModelName;

    /** 模型温度参数，控制输出随机性 */
    @Value("${langchain4j.openai.chat-model.temperature:0.3}")
    private double temperature;

    /** 最大生成 Token 数 */
    @Value("${langchain4j.openai.chat-model.max-tokens:2048}")
    private int maxTokens;

    /** Chat 请求超时时间 */
    @Value("${langchain4j.openai.chat-model.timeout:60s}")
    private Duration chatTimeout;

    // ==================== Embedding 模型配置 ====================

    /** Embedding API 密钥 */
    @Value("${langchain4j.openai.embedding-model.api-key:${langchain4j.openai.api-key}}")
    private String embeddingApiKey;

    /** Embedding API 基础地址 */
    @Value("${langchain4j.openai.embedding-model.base-url:https://api.openai.com}")
    private String embeddingBaseUrl;

    /** Embedding 模型名称 */
    @Value("${langchain4j.openai.embedding-model.model-name:text-embedding-3-small}")
    private String embeddingModelName;

    /** Embedding 请求超时时间 */
    @Value("${langchain4j.openai.embedding-model.timeout:120s}")
    private Duration embeddingTimeout;

    // ==================== Bean 定义 ====================

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(chatApiKey)
                .baseUrl(chatBaseUrl)
                .modelName(chatModelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(chatTimeout)
                .build();
    }

    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(chatApiKey)
                .baseUrl(chatBaseUrl)
                .modelName(chatModelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(chatTimeout)
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(embeddingApiKey)
                .baseUrl(embeddingBaseUrl)
                .modelName(embeddingModelName)
                .timeout(embeddingTimeout)
                .build();
    }
}
