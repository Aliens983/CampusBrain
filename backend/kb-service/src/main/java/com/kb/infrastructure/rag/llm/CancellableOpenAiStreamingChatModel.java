package com.kb.infrastructure.rag.llm;

import com.kb.domain.chat.ChatContextHolder;
import com.kb.domain.rag.CancellationToken;
import dev.ai4j.openai4j.OpenAiClient;
import dev.ai4j.openai4j.ResponseHandle;
import dev.ai4j.openai4j.chat.ChatCompletionChoice;
import dev.ai4j.openai4j.chat.ChatCompletionRequest;
import dev.ai4j.openai4j.chat.ChatCompletionResponse;
import dev.ai4j.openai4j.chat.Delta;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.InternalOpenAiHelper;
import dev.langchain4j.model.openai.OpenAiStreamingResponseBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 可取消的 OpenAI 兼容流式聊天模型。
 * <p>
 * langchain4j 0.34 的 {@code OpenAiStreamingChatModel} 内部把 openai4j
 * {@code execute()} 返回的 {@link ResponseHandle} 丢弃，外部无法取消在途请求。
 * 本类直接使用 {@link OpenAiClient} 发起 SSE 调用并保留该句柄：
 * 客户端断连时通过 {@link CancellationToken} 调用 {@code ResponseHandle.cancel()}，
 * 下一个 SSE 事件到达时 OkHttp EventSource 即被关闭，供应商侧不再继续生成/计费。
 * </p>
 * <p>
 * 本对象为<b>每次流式请求新建</b>（轻量对象，{@link OpenAiClient} 及其连接池由
 * {@link StreamingModelFactory} 单例复用），因此一个实例同一时刻只服务一轮对话，
 * AiServices 工具调用循环中对本模型的二次调用也能各自注册取消动作。
 * </p>
 *
 * @author forever-king
 */
@Slf4j
public class CancellableOpenAiStreamingChatModel implements StreamingChatLanguageModel {

    private final OpenAiClient client;
    private final String modelName;
    private final Double temperature;
    private final Integer maxTokens;
    private final CancellationToken cancellationToken;

    public CancellableOpenAiStreamingChatModel(OpenAiClient client,
                                                String modelName,
                                                Double temperature,
                                                Integer maxTokens,
                                                CancellationToken cancellationToken) {
        this.client = client;
        this.modelName = modelName;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.cancellationToken = cancellationToken;
    }

    @Override
    public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
        stream(messages, null, handler);
    }

    @Override
    public void generate(List<ChatMessage> messages,
                         List<ToolSpecification> toolSpecifications,
                         StreamingResponseHandler<AiMessage> handler) {
        stream(messages, toolSpecifications, handler);
    }

    /**
     * 发起流式请求并注册取消动作。
     */
    public void stream(List<ChatMessage> messages,
                       List<ToolSpecification> toolSpecifications,
                       StreamingResponseHandler<AiMessage> handler) {
        ChatCompletionRequest.Builder requestBuilder = ChatCompletionRequest.builder()
                .model(modelName)
                .stream(true)
                .messages(InternalOpenAiHelper.toOpenAiMessages(messages))
                .temperature(temperature)
                .maxTokens(maxTokens);
        if (toolSpecifications != null && !toolSpecifications.isEmpty()) {
            requestBuilder.tools(InternalOpenAiHelper.toTools(toolSpecifications, false));
        }

        OpenAiStreamingResponseBuilder responseBuilder = new OpenAiStreamingResponseBuilder(null);

        // 4.3（深度审查 P1，与 4.2 联动）：@Tool 的 Function Calling 循环由 LangChain4j 在
        // OkHttp SSE 回调线程上内联执行（handler.onComplete 内），而 ChatContextHolder 绑定在
        // 请求业务线程，工具线程读不到 → prepareBooking 草稿落不进会话、Feign 身份回退成服务身份。
        // 本模型实例按请求新建，此时仍在业务线程：入口捕获上下文，onComplete 前临时绑定/后清理，
        // 让同一回调线程上的工具执行拿到会话上下文，且即时清理杜绝把上下文遗留在池化线程上。
        ChatContextHolder.ChatContext turnContext = ChatContextHolder.get();

        ResponseHandle responseHandle = client.chatCompletion(requestBuilder.build())
                .onPartialResponse(partial -> onPartialResponse(partial, responseBuilder, handler))
                .onComplete(() -> completeWithContext(responseBuilder, handler, turnContext))
                .onError(handler::onError)
                .execute();

        // 客户端断连 → 中止在途 SSE（EventSource 在下一个事件时关闭，停止供应商侧生成）。
        // ResponseHandle.cancel() 仅置 volatile 标志，幂等可重复调用。
        cancellationToken.onAbort(() -> {
            responseHandle.cancel();
            log.debug("Streaming request aborted by client: model={}", modelName);
        });
    }

    private void onPartialResponse(ChatCompletionResponse partialResponse,
                                   OpenAiStreamingResponseBuilder responseBuilder,
                                  StreamingResponseHandler<AiMessage> handler) {
        // 取消后供应商可能还有少量已到达事件：直接忽略，不再累积也不再回调
        if (cancellationToken.isCancelled()) {
            return;
        }
        List<ChatCompletionChoice> choices = partialResponse.choices();
        if (choices == null || choices.isEmpty()) {
            return;
        }
        responseBuilder.append(partialResponse);
        Delta delta = choices.get(0).delta();
        if (delta != null && delta.content() != null) {
            handler.onNext(delta.content());
        }
    }

    /**
     * 4.3：把会话上下文临时绑定到当前（OkHttp 回调）线程后再回调下游 handler，
     * 使 LangChain4j 在同一线程内联执行的 @Tool 工具循环能读到 {@link ChatContextHolder}；
     * finally 中清理，避免上下文遗留在池化回调线程上造成跨请求串号。
     */
    private void completeWithContext(OpenAiStreamingResponseBuilder responseBuilder,
                                     StreamingResponseHandler<AiMessage> handler,
                                     ChatContextHolder.ChatContext turnContext) {
        if (turnContext == null) {
            handler.onComplete(responseBuilder.build(null, false));
            return;
        }
        ChatContextHolder.set(turnContext);
        try {
            handler.onComplete(responseBuilder.build(null, false));
        } finally {
            ChatContextHolder.clear();
        }
    }
}
