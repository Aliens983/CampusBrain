package com.kb.infrastructure.rag.llm;

import com.kb.domain.rag.LlmService;
import com.kb.domain.rag.RetrievalResult;
import com.kb.infrastructure.rag.tool.AppointmentTool;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * LLM service implementation using LangChain4j.
 * <p>
 * Supports both synchronous and streaming (SSE) answer generation.
 * The streaming mode pushes each token to the frontend via callback,
 * enabling a ChatGPT-like typing effect.
 * </p>
 *
 * @author forever-king
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LangChain4jLlmService implements LlmService {

    /** 同步聊天语言模型，用于生成完整的LLM回答 */
    private final ChatLanguageModel chatModel;
    /** 流式聊天语言模型，支持逐token推送的SSE流式输出 */
    private final StreamingChatLanguageModel streamingChatModel;
    /** 提示词模板引擎，用于构建RAG场景下的完整提示词 */
    private final PromptTemplateEngine promptEngine;
    /** 预约实时查询工具（LangChain4j @Tool） */
    private final AppointmentTool appointmentTool;

    /** LLM Provider（从配置读取，默认 deepseek）
     *  主模型由 LangChain4j Spring Boot auto-config 通过 yml 构建；
     *  此字段用于 fallback 路由和启动诊断日志 */
    @Value("${llm.provider:deepseek}")
    private String llmProvider;

    /** 绑定实时查询工具的 AI 助手（AiServices） */
    private ToolAssistant toolAssistant;

    /** AI 助手接口：LLM 可在回答时自主调用 @Tool 获取实时数据 */
    @FunctionalInterface
    public interface ToolAssistant {
        String chat(String userMessage);
    }

    @PostConstruct
    void init() {
        ModelProvider mp = ModelProvider.fromName(llmProvider);
        log.info("LLM provider configured: {} (base={})", mp.getDisplayName(), mp.getBaseUrl());
        toolAssistant = AiServices.builder(ToolAssistant.class)
                .chatLanguageModel(chatModel)
                .tools(appointmentTool)
                .build();
        log.info("Tool assistant initialized with AppointmentTool (Function Calling)");
    }

    @Override
    @CircuitBreaker(name = "llmService", fallbackMethod = "generateAnswerFallback")
    public String generateAnswer(String query, List<RetrievalResult> retrievedDocs,
                                  List<ChatMessage> conversationHistory) {
        List<ChatMessage> fullPrompt = promptEngine.buildFullPrompt(
                query, retrievedDocs, conversationHistory);

        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
        for (ChatMessage cm : fullPrompt) {
            messages.add(toLangChainMessage(cm));
        }
        // 只取最近 6 条历史，避免历史过长导致 LLM 串题
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            List<ChatMessage> recent = conversationHistory.size() > 6
                    ? conversationHistory.subList(conversationHistory.size() - 6, conversationHistory.size())
                    : conversationHistory;
            for (ChatMessage cm : recent) {
                messages.add(toLangChainMessage(cm));
            }
        }

        try {
            var response = chatModel.generate(messages);
            return response.content().text();
        } catch (Exception e) {
            log.error("LLM generation failed with primary model, trying fallback", e);
            return tryFallback(messages);
        }
    }

    /** Circuit breaker fallback: 返回友善错误提示 */
    @SuppressWarnings("unused")
    private String generateAnswerFallback(String query, List<RetrievalResult> docs,
                                          List<ChatMessage> history, Throwable t) {
        log.warn("Circuit breaker OPEN for llmService, returning fallback response");
        return "AI 服务暂时不可用，请稍后重试。";
    }

    @Override
    @CircuitBreaker(name = "llmService", fallbackMethod = "generateStreamingFallback")
    public String generateAnswerStreaming(String query,
                                           List<RetrievalResult> retrievedDocs,
                                           List<ChatMessage> conversationHistory,
                                           Consumer<String> tokenConsumer) {
        List<ChatMessage> fullPrompt = promptEngine.buildFullPrompt(
                query, retrievedDocs, conversationHistory);

        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
        for (ChatMessage cm : fullPrompt) {
            messages.add(toLangChainMessage(cm));
        }

        if (conversationHistory != null) {
            for (ChatMessage cm : conversationHistory) {
                messages.add(toLangChainMessage(cm));
            }
        }

        StringBuilder fullAnswer = new StringBuilder();

        try {
            CompletableFuture<Void> future = new CompletableFuture<>();

            streamingChatModel.generate(messages, new dev.langchain4j.model.StreamingResponseHandler<>() {
                @Override
                public void onNext(String token) {
                    tokenConsumer.accept(token);
                    fullAnswer.append(token);
                }

                @Override
                public void onComplete(
                        dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> response) {
                    future.complete(null);
                }

                @Override
                public void onError(Throwable error) {
                    log.error("Streaming error", error);
                    tokenConsumer.accept("\n\n[生成出错，请重试]");
                    future.completeExceptionally(error);
                }
            });

            future.join();
        } catch (Exception e) {
            log.error("Streaming generation failed, trying fallback", e);
            String fallback = tryFallback(messages);
            tokenConsumer.accept(fallback);
        }

        return fullAnswer.toString();
    }

    /**
     * Fallback: 使用备选 Provider 重试
     * 如果主模型不是 DeepSeek，则 fallback 到 DeepSeek；
     * 如果主模型就是 DeepSeek，则 fallback 到 Qwen
     */
    private String tryFallback(List<dev.langchain4j.data.message.ChatMessage> messages) {
        ModelProvider primary = ModelProvider.fromName(llmProvider);
        ModelProvider fallbackProvider = (primary == ModelProvider.DEEPSEEK)
                ? ModelProvider.QWEN
                : ModelProvider.DEEPSEEK;

        log.warn("Primary LLM [{}] failed, attempting fallback to [{}]",
                primary.getDisplayName(), fallbackProvider.getDisplayName());

        String fallbackApiKey = switch (fallbackProvider) {
            case DEEPSEEK -> System.getenv("DEEPSEEK_API_KEY");
            case QWEN -> System.getenv("QWEN_API_KEY");
            case OPENAI -> System.getenv("OPENAI_API_KEY");
            case OLLAMA -> "ollama";
            case SILICONFLOW -> System.getenv("EMBEDDING_API_KEY");
        };

        try {
            ChatLanguageModel fallbackModel = OpenAiChatModel.builder()
                    .baseUrl(fallbackProvider.getBaseUrl())
                    .apiKey(fallbackApiKey)
                    .modelName("deepseek-chat")
                    .temperature(0.3)
                    .maxTokens(2048)
                    .timeout(Duration.ofSeconds(60))
                    .build();
            var response = fallbackModel.generate(messages);
            return response.content().text();
        } catch (Exception e2) {
            log.error("Fallback LLM [{}] also failed", fallbackProvider.getDisplayName(), e2);
            return "抱歉，生成答案时遇到了问题，请稍后重试。";
        }
    }

    /** Streaming fallback: 推送错误提示并返回 */
    @SuppressWarnings("unused")
    private String generateStreamingFallback(String query, List<RetrievalResult> docs,
                                              List<ChatMessage> history,
                                              Consumer<String> tokenConsumer, Throwable t) {
        log.warn("Circuit breaker OPEN for llmService (streaming), returning fallback");
        String msg = "AI 服务暂时不可用，请稍后重试。";
        tokenConsumer.accept(msg);
        return msg;
    }

    private dev.langchain4j.data.message.ChatMessage toLangChainMessage(ChatMessage cm) {
        return switch (cm.role()) {
            case "system" -> dev.langchain4j.data.message.SystemMessage.from(cm.content());
            case "assistant" -> dev.langchain4j.data.message.AiMessage.from(cm.content());
            default -> dev.langchain4j.data.message.UserMessage.from(cm.content());
        };
    }

    @Override
    public String generateAnswerWithTools(String query, List<RetrievalResult> retrievedDocs,
                                           List<ChatMessage> conversationHistory,
                                           Consumer<String> tokenConsumer) {
        try {
            String context = buildRagContext(retrievedDocs);
            String historyText = buildHistoryText(conversationHistory);
            String userMessage = "对话历史：\n" + historyText
                    + "\n\n用户问题：" + query
                    + "\n\n知识库参考内容：\n" + context
                    + "\n\n你是校园预约助手。**仅当**用户询问「当前/今天有哪些服务可预约、预约余量、会议室/设备/咨询是否可用」这类需要实时预约数据的问题时，"
                    + "才调用预约查询工具获取实时数据回答；其他问题（自我介绍、能力介绍、闲聊、知识问答等）请直接回答，不要调用任何工具。";
            String answer = toolAssistant.chat(userMessage);
            if (tokenConsumer != null) {
                tokenConsumer.accept(answer);
            }
            return answer;
        } catch (Exception e) {
            log.error("Tool-enhanced generation failed, fallback to plain RAG", e);
            String fallback = generateAnswer(query, retrievedDocs, conversationHistory);
            if (tokenConsumer != null) {
                tokenConsumer.accept(fallback);
            }
            return fallback;
        }
    }

    @Override
    public String generateAnswerDirect(String query, List<ChatMessage> conversationHistory) {
        List<dev.langchain4j.data.message.ChatMessage> messages = buildDirectMessages(query, conversationHistory);
        try {
            var response = chatModel.generate(messages);
            return response.content().text();
        } catch (Exception e) {
            log.error("Direct LLM generation failed, trying fallback", e);
            return tryFallback(messages);
        }
    }

    @Override
    public String generateAnswerDirectStreaming(String query, List<ChatMessage> conversationHistory,
                                                Consumer<String> tokenConsumer) {
        List<dev.langchain4j.data.message.ChatMessage> messages = buildDirectMessages(query, conversationHistory);
        StringBuilder fullAnswer = new StringBuilder();
        try {
            CompletableFuture<Void> future = new CompletableFuture<>();
            streamingChatModel.generate(messages, new dev.langchain4j.model.StreamingResponseHandler<>() {
                @Override
                public void onNext(String token) {
                    tokenConsumer.accept(token);
                    fullAnswer.append(token);
                }
                @Override
                public void onComplete(
                        dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> response) {
                    future.complete(null);
                }
                @Override
                public void onError(Throwable error) {
                    log.error("Direct streaming error", error);
                    tokenConsumer.accept("\n\n[生成出错，请重试]");
                    future.completeExceptionally(error);
                }
            });
            future.join();
        } catch (Exception e) {
            log.error("Direct streaming failed, trying fallback", e);
            String fallback = tryFallback(messages);
            tokenConsumer.accept(fallback);
        }
        return fullAnswer.toString();
    }

    /** 构建"直接对话"消息列表（不带 RAG 上下文，DeepSeek 兜底回答） */
    private List<dev.langchain4j.data.message.ChatMessage> buildDirectMessages(
            String query, List<ChatMessage> conversationHistory) {
        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
        messages.add(dev.langchain4j.data.message.SystemMessage.from(
                "你是一个智能校园助手。请用中文直接、自然、友好地回答用户的问题。"
                        + "请只回答用户当前最后提出的这个问题；历史对话仅供理解上下文，"
                        + "不要重复回答历史中已出现过的问题。"
                        + "如果不知道答案，请诚实说明，不要编造。"));
        // 只取最近 6 条历史，避免历史过长导致 LLM 串题
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            List<ChatMessage> recent = conversationHistory.size() > 6
                    ? conversationHistory.subList(conversationHistory.size() - 6, conversationHistory.size())
                    : conversationHistory;
            for (ChatMessage cm : recent) {
                messages.add(toLangChainMessage(cm));
            }
        }
        messages.add(dev.langchain4j.data.message.UserMessage.from(query));
        return messages;
    }

    /** 把多轮对话历史拼成文本，供 Tool 增强链路保留上下文 */
    private String buildHistoryText(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return "（无）";
        }
        StringBuilder sb = new StringBuilder();
        for (ChatMessage cm : history) {
            sb.append(cm.role()).append("：").append(cm.content()).append("\n");
        }
        return sb.toString().trim();
    }

    /** 将检索结果拼成简短的 RAG 上下文，供 Tool 增强链路使用 */
    private String buildRagContext(List<RetrievalResult> docs) {
        if (docs == null || docs.isEmpty()) {
            return "（无）";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(docs.size(), 5); i++) {
            RetrievalResult r = docs.get(i);
            sb.append("[").append(i + 1).append("] ").append(r.getContent()).append("\n");
        }
        return sb.toString().trim();
    }
}
