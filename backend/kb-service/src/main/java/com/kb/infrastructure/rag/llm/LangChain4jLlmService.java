package com.kb.infrastructure.rag.llm;

import com.kb.domain.rag.CancellationToken;
import com.kb.domain.rag.LlmService;
import com.kb.domain.rag.RetrievalResult;
import com.kb.infrastructure.rag.tool.AppointmentTool;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * LLM service implementation using LangChain4j.
 * <p>
 * 支持同步与流式（SSE）两种回答生成方式。流式链路（RAG / Function Calling / 兜底）
 * 均通过 {@link StreamingModelFactory} 创建可取消模型：客户端断连时中止供应商在途请求。
 * </p>
 * <p>
 * <b>熔断语义（12-01）</b>：供应商异常不再在方法内部吞成普通字符串，
 * 主备两次尝试均失败后抛出 {@link LlmUnavailableException}，由 Resilience4j
 * 统计真实失败率；熔断打开时由 fallbackMethod 返回统一兜底文案。
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
    /** 提示词模板引擎，用于构建RAG场景下的完整提示词 */
    private final PromptTemplateEngine promptEngine;
    /** 预约实时查询工具（LangChain4j @Tool） */
    private final AppointmentTool appointmentTool;
    /** 可取消流式模型工厂（内部复用单例 OpenAiClient 连接池） */
    private final StreamingModelFactory streamingModelFactory;

    /** LLM Provider（从配置读取，默认 deepSeek），用于 fallback 路由和启动诊断日志 */
    @Value("${llm.provider:deepseek}")
    private String llmProvider;

    // ========== 跨供应商兜底密钥（Q-06：统一走 Spring 配置注入，不再 System.getenv 直取） ==========
    // 与主模型密钥（StreamingModelFactory 的 @Value）同一配置入口，缺失时为空串而非 NPE；
    // 可用 application.yml / 环境变量 / 配置中心任一渠道覆盖。

    @Value("${llm.fallback.deepseek-api-key:${DEEPSEEK_API_KEY:}}")
    private String fallbackDeepseekApiKey;

    @Value("${llm.fallback.qwen-api-key:${QWEN_API_KEY:}}")
    private String fallbackQwenApiKey;

    @Value("${llm.fallback.openai-api-key:${OPENAI_API_KEY:}}")
    private String fallbackOpenaiApiKey;

    /**
     * 硅基流动兜底聊天密钥。优先独立配置 {@code llm.fallback.siliconflow-api-key} /
     * 环境变量 {@code SILICONFLOW_API_KEY}；历史实现直接复用 {@code EMBEDDING_API_KEY}
     * （本部署 embedding 亦托管在硅基流动、同一账号 key 可用于 chat），保留该回退仅为向后兼容，
     * 新部署应配置独立聊天 key（见 application.yml 注释"chat/embedding key 分开"）。
     */
    @Value("${llm.fallback.siliconflow-api-key:${SILICONFLOW_API_KEY:${EMBEDDING_API_KEY:}}}")
    private String fallbackSiliconflowApiKey;

    /** 注入 LLM 的历史消息上限（超出部分丢弃最早的记录） */
    private static final int MAX_HISTORY_MESSAGES = 6;

    /**
     * 流式生成的最长等待时间（秒）。
     * <p>
     * 需大于 LLM 自身的超时，正常回答不会被截断；但一旦供应商连接挂起导致
     * onComplete/onError 都不回调，join() 必须有上限，否则 Tomcat 线程永久泄漏。
     */
    private static final int LLM_STREAM_TIMEOUT_SECONDS = 90;

    /** 熔断打开/主备耗尽时面向用户的统一兜底文案（A-07；不含任何内部失败细节） */
    private static final String LLM_FALLBACK_MESSAGE = "AI 服务暂时不可用，请稍后重试。";

    /** 绑定实时查询工具的 AI 助手（AiServices，同步 Function Calling） */
    private ToolAssistant toolAssistant;

    /** AI 助手接口（同步）：LLM 可在回答时自主调用 @Tool 获取实时数据 */
    @FunctionalInterface
    public interface ToolAssistant {
        String chat(String userMessage);
    }

    /** AI 助手接口（流式）：Function Calling 期间也逐 token 返回 */
    interface StreamingToolAssistant {
        TokenStream chat(String userMessage);
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

    // ==================== 同步 RAG ====================

    @Override
    @CircuitBreaker(name = "llmService", fallbackMethod = "unavailableAnswer")
    public String generateAnswer(String query, List<RetrievalResult> retrievedDocs,
                                  List<ChatMessage> conversationHistory) {
        List<dev.langchain4j.data.message.ChatMessage> messages = toLangChainMessages(
                promptEngine.buildFullPrompt(query, retrievedDocs, conversationHistory));
        try {
            return chatModel.generate(messages).content().text();
        } catch (Exception e) {
            log.error("LLM generation failed with primary model, trying fallback", e);
            return tryFallback(messages);
        }
    }

    /** 熔断打开 / 主备均失败时的同步兜底 */
    @SuppressWarnings("unused")
    private String unavailableAnswer(String query, List<RetrievalResult> docs,
                                     List<ChatMessage> history, Throwable t) {
        log.warn("Circuit breaker OPEN for llmService (rag-sync), returning fallback");
        return LLM_FALLBACK_MESSAGE;
    }

    // ==================== 流式 RAG ====================

    @Override
    public String generateAnswerStreaming(String query, List<RetrievalResult> retrievedDocs,
                                           List<ChatMessage> conversationHistory,
                                           Consumer<String> tokenConsumer) {
        return generateAnswerStreaming(query, retrievedDocs, conversationHistory,
                tokenConsumer, CancellationToken.none());
    }

    @Override
    @CircuitBreaker(name = "llmService", fallbackMethod = "streamingUnavailable")
    public String generateAnswerStreaming(String query, List<RetrievalResult> retrievedDocs,
                                           List<ChatMessage> conversationHistory,
                                           Consumer<String> tokenConsumer,
                                           CancellationToken cancellationToken) {
        List<dev.langchain4j.data.message.ChatMessage> messages = toLangChainMessages(
                promptEngine.buildFullPrompt(query, retrievedDocs, conversationHistory));
        try {
            return streamRag(messages, tokenConsumer, cancellationToken);
        } catch (StreamCancelledException e) {
            throw e;
        } catch (Exception e) {
            log.error("RAG streaming failed, trying fallback model", e);
            return tryFallbackStreaming(messages, tokenConsumer, cancellationToken);
        }
    }

    /**
     * RAG 真流式：token 直推消费者；客户端取消时中止在途供应商请求。
     */
    private String streamRag(List<dev.langchain4j.data.message.ChatMessage> ragMessages,
                             Consumer<String> tokenConsumer,
                             CancellationToken cancellationToken) {
        StringBuilder full = new StringBuilder();
        StreamingChatLanguageModel model = streamingModelFactory.create(cancellationToken);
        CompletableFuture<Void> future = new CompletableFuture<>();
        model.generate(ragMessages, new dev.langchain4j.model.StreamingResponseHandler<>() {
            @Override
            public void onNext(String token) {
                if (!cancellationToken.isCancelled()) {
                    full.append(token);
                    tokenConsumer.accept(token);
                }
            }

            @Override
            public void onComplete(dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> r) {
                future.complete(null);
            }

            @Override
            public void onError(Throwable error) {
                if (cancellationToken.isCancelled()) {
                    // 客户端主动取消引发的 onError 不算失败
                    future.complete(null);
                } else {
                    future.completeExceptionally(error);
                }
            }
        });
        awaitStream(future, cancellationToken);
        return full.toString();
    }

    @SuppressWarnings("unused")
    private String streamingUnavailable(String query, List<RetrievalResult> docs,
                                         List<ChatMessage> history, Consumer<String> consumer,
                                         CancellationToken token, Throwable t) {
        log.warn("Circuit breaker OPEN for llmService (rag-streaming), returning fallback");
        consumer.accept(LLM_FALLBACK_MESSAGE);
        return LLM_FALLBACK_MESSAGE;
    }

    // ==================== Function Calling 流式 ====================

    @Override
    @CircuitBreaker(name = "llmService", fallbackMethod = "toolsStreamingUnavailable")
    public String generateAnswerWithToolsStreaming(String query, List<RetrievalResult> retrievedDocs,
                                                    List<ChatMessage> conversationHistory,
                                                    Consumer<String> tokenConsumer,
                                                    String contextHint,
                                                    CancellationToken cancellationToken) {
        String userMessage = buildToolUserMessage(query, retrievedDocs, conversationHistory, contextHint);
        StreamingToolAssistant assistant = AiServices.builder(StreamingToolAssistant.class)
                .streamingChatLanguageModel(streamingModelFactory.create(cancellationToken))
                .tools(appointmentTool)
                .build();

        StringBuilder full = new StringBuilder();
        CompletableFuture<String> future = new CompletableFuture<>();
        assistant.chat(userMessage)
                .onNext(token -> {
                    if (!cancellationToken.isCancelled()) {
                        full.append(token);
                        tokenConsumer.accept(token);
                    }
                })
                .onComplete(response -> future.complete(full.toString()))
                .onError(error -> {
                    if (cancellationToken.isCancelled()) {
                        future.complete(full.toString());
                    } else {
                        future.completeExceptionally(error);
                    }
                })
                .start();

        try {
            return awaitFuture(future, cancellationToken);
        } catch (StreamCancelledException e) {
            throw e;
        } catch (Exception e) {
            log.error("Tool-enhanced streaming failed, fallback to plain RAG", e);
            // 与同步链路一致：工具链路失败时退化为纯 RAG（同步生成一次，保证有答案）
            String fallback = generateAnswer(query, retrievedDocs, conversationHistory);
            if (!cancellationToken.isCancelled()) {
                tokenConsumer.accept(fallback);
            }
            return fallback;
        }
    }

    @SuppressWarnings("unused")
    private String toolsStreamingUnavailable(String query, List<RetrievalResult> docs,
                                              List<ChatMessage> history, Consumer<String> consumer,
                                              String hint, CancellationToken token, Throwable t) {
        log.warn("Circuit breaker OPEN for llmService (tools-streaming), returning fallback");
        consumer.accept(LLM_FALLBACK_MESSAGE);
        return LLM_FALLBACK_MESSAGE;
    }

    // ==================== 同步 Function Calling（保留给非流式/测试入口） ====================

    @Override
    public String generateAnswerWithTools(String query, List<RetrievalResult> retrievedDocs,
                                           List<ChatMessage> conversationHistory,
                                           Consumer<String> tokenConsumer, String contextHint) {
        try {
            String userMessage = buildToolUserMessage(query, retrievedDocs, conversationHistory, contextHint);
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

    // ==================== 直接对话（无 RAG 上下文） ====================

    @Override
    public String generateAnswerDirect(String query, List<ChatMessage> conversationHistory) {
        List<dev.langchain4j.data.message.ChatMessage> messages = buildDirectMessages(query, conversationHistory);
        try {
            return chatModel.generate(messages).content().text();
        } catch (Exception e) {
            log.error("Direct LLM generation failed, trying fallback", e);
            return tryFallback(messages);
        }
    }

    @Override
    public String generateAnswerDirectStreaming(String query, List<ChatMessage> conversationHistory,
                                                Consumer<String> tokenConsumer) {
        return generateAnswerDirectStreaming(query, conversationHistory, tokenConsumer,
                CancellationToken.none());
    }

    @Override
    @CircuitBreaker(name = "llmService", fallbackMethod = "directStreamingUnavailable")
    public String generateAnswerDirectStreaming(String query, List<ChatMessage> conversationHistory,
                                                Consumer<String> tokenConsumer,
                                                CancellationToken cancellationToken) {
        List<dev.langchain4j.data.message.ChatMessage> messages = buildDirectMessages(query, conversationHistory);
        StringBuilder full = new StringBuilder();
        try {
            StreamingChatLanguageModel model = streamingModelFactory.create(cancellationToken);
            CompletableFuture<Void> future = new CompletableFuture<>();
            model.generate(messages, new dev.langchain4j.model.StreamingResponseHandler<>() {
                @Override
                public void onNext(String token) {
                    if (cancellationToken.isCancelled()) {
                        return;
                    }
                    full.append(token);
                    tokenConsumer.accept(token);
                }

                @Override
                public void onComplete(dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> r) {
                    future.complete(null);
                }

                @Override
                public void onError(Throwable error) {
                    future.completeExceptionally(error);
                }
            });
            awaitStream(future, cancellationToken);
        } catch (StreamCancelledException e) {
            throw e;
        } catch (Exception e) {
            log.error("Direct streaming failed, trying fallback model", e);
            if (!cancellationToken.isCancelled()) {
                tokenConsumer.accept(tryFallback(messages));
            }
        }
        return full.toString();
    }

    @SuppressWarnings("unused")
    private String directStreamingUnavailable(String query, List<ChatMessage> history,
                                               Consumer<String> consumer,
                                               CancellationToken token, Throwable t) {
        log.warn("Circuit breaker OPEN for llmService (direct-streaming), returning fallback");
        consumer.accept(LLM_FALLBACK_MESSAGE);
        return LLM_FALLBACK_MESSAGE;
    }

    // ==================== 跨供应商兜底 ====================

    /**
     * Fallback: 使用备选 Provider 同步重试一次。两次尝试都失败时抛
     * {@link LlmUnavailableException}，让熔断器统计失败而非永久返回假成功。
     */
    private String tryFallback(List<dev.langchain4j.data.message.ChatMessage> messages) {
        ModelProvider primary = ModelProvider.fromName(llmProvider);
        ModelProvider fallbackProvider = (primary == ModelProvider.DEEPSEEK)
                ? ModelProvider.QWEN
                : ModelProvider.DEEPSEEK;

        log.warn("Primary LLM [{}] failed, attempting fallback to [{}]",
                primary.getDisplayName(), fallbackProvider.getDisplayName());

        String fallbackApiKey = resolveFallbackApiKey(fallbackProvider);
        if (fallbackProvider != ModelProvider.OLLAMA
                && (fallbackApiKey == null || fallbackApiKey.isBlank())) {
            // 密钥缺失属配置问题：直接抛出让熔断器统计失败，而不是带着 null key 请求供应商再产生难定位的 401
            log.error("Fallback LLM [{}] api-key 未配置（llm.fallback.*-api-key / 对应环境变量）",
                    fallbackProvider.getDisplayName());
            throw new LlmUnavailableException(
                    "LLM 兜底供应商密钥未配置: " + fallbackProvider.getDisplayName());
        }

        try {
            ChatLanguageModel fallbackModel = OpenAiChatModel.builder()
                    .baseUrl(fallbackProvider.getBaseUrl())
                    .apiKey(fallbackApiKey)
                    .modelName(defaultModelName(fallbackProvider))
                    .temperature(0.3)
                    .maxTokens(2048)
                    .timeout(Duration.ofSeconds(60))
                    .build();
            return fallbackModel.generate(messages).content().text();
        } catch (Exception e2) {
            log.error("Fallback LLM [{}] also failed", fallbackProvider.getDisplayName(), e2);
            throw new LlmUnavailableException(
                    "LLM 主备供应商均不可用: " + fallbackProvider.getDisplayName(), e2);
        }
    }

    /**
     * 按兜底供应商解析聊天密钥（Q-06：唯一出口，配置注入）。
     */
    private String resolveFallbackApiKey(ModelProvider provider) {
        return switch (provider) {
            case DEEPSEEK -> fallbackDeepseekApiKey;
            case QWEN -> fallbackQwenApiKey;
            case OPENAI -> fallbackOpenaiApiKey;
            case OLLAMA -> "ollama";
            case SILICONFLOW -> fallbackSiliconflowApiKey;
        };
    }

    /**
     * 流式场景的跨供应商兜底：同步调用备选模型后一次性下发。
     * 客户端已取消则不再请求、不下发。
     */
    private String tryFallbackStreaming(List<dev.langchain4j.data.message.ChatMessage> messages,
                                        Consumer<String> tokenConsumer,
                                        CancellationToken cancellationToken) {
        if (cancellationToken.isCancelled()) {
            throw new StreamCancelledException();
        }
        String answer = tryFallback(messages);
        if (!cancellationToken.isCancelled()) {
            tokenConsumer.accept(answer);
        }
        return answer;
    }

    // ==================== 流式等待与取消辅助 ====================

    /**
     * 等待流式完成：服务端 90s 硬超时会中止在途请求；客户端取消会抛
     * {@link StreamCancelledException}，上层不推错误文案。
     */
    private void awaitStream(CompletableFuture<Void> future, CancellationToken cancellationToken) {
        try {
            future.orTimeout(LLM_STREAM_TIMEOUT_SECONDS, TimeUnit.SECONDS).join();
        } catch (java.util.concurrent.CompletionException ce) {
            if (cancellationToken.isCancelled()) {
                throw new StreamCancelledException();
            }
            Throwable cause = ce.getCause() != null ? ce.getCause() : ce;
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new LlmUnavailableException("流式生成失败", cause);
        } catch (Exception e) {
            if (cancellationToken.isCancelled()) {
                throw new StreamCancelledException();
            }
            throw e;
        }
    }

    private String awaitFuture(CompletableFuture<String> future, CancellationToken cancellationToken) {
        try {
            return future.orTimeout(LLM_STREAM_TIMEOUT_SECONDS, TimeUnit.SECONDS).join();
        } catch (java.util.concurrent.CompletionException ce) {
            if (cancellationToken.isCancelled()) {
                throw new StreamCancelledException();
            }
            Throwable cause = ce.getCause() != null ? ce.getCause() : ce;
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new LlmUnavailableException("流式生成失败", cause);
        }
    }

    // ==================== 消息与提示词构建 ====================

    private List<dev.langchain4j.data.message.ChatMessage> toLangChainMessages(List<ChatMessage> fullPrompt) {
        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
        for (ChatMessage cm : fullPrompt) {
            messages.add(toLangChainMessage(cm));
        }
        return messages;
    }

    private dev.langchain4j.data.message.ChatMessage toLangChainMessage(ChatMessage cm) {
        return switch (cm.role()) {
            case "system" -> dev.langchain4j.data.message.SystemMessage.from(cm.content());
            case "assistant" -> dev.langchain4j.data.message.AiMessage.from(cm.content());
            default -> dev.langchain4j.data.message.UserMessage.from(cm.content());
        };
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
        appendRecentHistory(messages, conversationHistory);
        messages.add(dev.langchain4j.data.message.UserMessage.from(query));
        return messages;
    }

    /**
     * 追加最近若干条历史消息。只取最近 6 条，避免串题与 token 浪费。
     */
    private void appendRecentHistory(List<dev.langchain4j.data.message.ChatMessage> messages,
                                     List<ChatMessage> conversationHistory) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return;
        }
        List<ChatMessage> recent = conversationHistory.size() > MAX_HISTORY_MESSAGES
                ? conversationHistory.subList(conversationHistory.size() - MAX_HISTORY_MESSAGES,
                                              conversationHistory.size())
                : conversationHistory;
        for (ChatMessage cm : recent) {
            messages.add(toLangChainMessage(cm));
        }
    }

    /** 构建 Function Calling 链路的用户消息（历史 + 问题 + RAG 上下文 + 行为约束） */
    private String buildToolUserMessage(String query, List<RetrievalResult> retrievedDocs,
                                        List<ChatMessage> conversationHistory, String contextHint) {
        String context = buildRagContext(retrievedDocs);
        String historyText = buildHistoryText(conversationHistory);
        return "对话历史：\n" + historyText
                + "\n\n用户问题：" + query
                + "\n\n知识库参考内容：\n" + context
                + (contextHint == null || contextHint.isBlank()
                    ? "" : "\n\n【当前已知的预约条件】" + contextHint
                         + "\n调用工具时若用户未重新说明，请沿用这些条件；用户本轮明确改变了某项则以本轮为准。")
                + "\n\n你是校园预约助手。**仅当**用户询问「当前/今天有哪些服务可预约、预约余量、会议室/设备/咨询是否可用」这类需要实时预约数据的问题时，"
                + "才调用预约查询工具获取实时数据回答；其他问题（自我介绍、能力介绍、闲聊、知识问答等）请直接回答，不要调用任何工具。"
                + "\n\n用户要求预约或取消时，只能调用 prepareBooking / requestCancelBooking 生成待确认草稿，"
                + "并明确询问用户「是否确认预约？」。**严禁**在用户明确答复之前做任何写操作。";
    }

    /** 各 Provider 的默认兜底模型名（不能共用一个名字，否则请求必然失败） */
    private static String defaultModelName(ModelProvider provider) {
        return switch (provider) {
            case DEEPSEEK -> "deepseek-chat";
            case QWEN -> "qwen-plus";
            case OPENAI -> "gpt-4o-mini";
            case OLLAMA -> "qwen2.5";
            case SILICONFLOW -> "Qwen/Qwen2.5-7B-Instruct";
        };
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
