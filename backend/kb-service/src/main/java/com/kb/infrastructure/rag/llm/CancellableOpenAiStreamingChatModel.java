package com.kb.infrastructure.rag.llm;

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

        ResponseHandle responseHandle = client.chatCompletion(requestBuilder.build())
                .onPartialResponse(partial -> onPartialResponse(partial, responseBuilder, handler))
                .onComplete(() -> handler.onComplete(responseBuilder.build(null, false)))
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
}
