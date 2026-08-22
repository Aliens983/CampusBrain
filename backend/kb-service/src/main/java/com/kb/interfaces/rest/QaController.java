package com.kb.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.application.service.IQaApplicationService;
import com.kb.domain.conversation.Conversation;
import com.kb.domain.conversation.Conversation.CitationRef;
import com.kb.interfaces.dto.ApiResponse;
import com.kb.interfaces.dto.FeedbackRequest;
import com.kb.interfaces.dto.QaRequest;
import com.kb.interfaces.dto.QaResponse;
import com.kb.infrastructure.ratelimit.annotations.RateLimit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * REST controller for Q&A with SSE streaming.
 * @author forever-king
 */
@Tag(name = "智能问答", description = "基于 RAG 的文档问答，支持同步和 SSE 流式两种模式")
@Slf4j
@RestController
@RequestMapping("/kb/qa")
@RequiredArgsConstructor
public class QaController {

    /** Q&A 应用服务 */
    private final IQaApplicationService qaService;

    /** JSON 序列化/反序列化工具 */
    private final ObjectMapper objectMapper;

    /**
     * Streaming Q&A via Server-Sent Events.
     * <p>
     * Frontend usage:
     * <pre>
     * const eventSource = new EventSource(
     *     '/api/qa/ask/stream?query=xxx&sessionId=yyy'
     * );
     * eventSource.onmessage = (event) => {
     *     if (event.data === '[DONE]') { eventSource.close(); return; }
     *     appendToken(event.data);
     * };
     * eventSource.addEventListener('citations', (event) => {
     *     showCitations(JSON.parse(event.data));
     * });
     * </pre>
     */
    @RateLimit(permits = 20, seconds = 60, message = "问答请求过于频繁，请稍后再试")
    @Operation(summary = "流式问答（SSE）", description = "通过 Server-Sent Events 逐字流式返回 AI 回答，支持会话上下文")
    @GetMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<?>> askStreaming(
            @Parameter(description = "用户问题") @RequestParam String query,
            @Parameter(description = "会话 ID，不传则自动生成新会话") @RequestParam(required = false, defaultValue = "") String sessionId) {

        return Flux.create(sink -> {
            try {
                qaService.askStreaming(query, sessionId,
                        token -> {
                            // 回答 token 用默认 message 事件
                            if (!sink.isCancelled()) {
                                sink.next(ServerSentEvent.builder().data(token).build());
                            }
                        },
                        citations -> {
                            if (!sink.isCancelled()) {
                                try {
                                    String json = objectMapper.writeValueAsString(citations);
                                    sink.next(ServerSentEvent.builder()
                                            .event("citations").data(json).build());
                                } catch (Exception e) {
                                    log.warn("Failed to serialize citations", e);
                                }
                            }
                        },
                        messageId -> {
                            if (!sink.isCancelled()) {
                                sink.next(ServerSentEvent.builder()
                                        .event("messageId").data(messageId).build());
                            }
                        }
                );
                sink.complete();
            } catch (Exception e) {
                log.error("SSE streaming error", e);
                sink.error(e);
            }
        });
    }

    @RateLimit(permits = 30, seconds = 60, message = "问答请求过于频繁，请稍后再试")
    @Operation(summary = "同步问答", description = "一次性返回完整 AI 回答和引用来源")
    @PostMapping("/ask")
    public ApiResponse<QaResponse> ask(@Valid @RequestBody QaRequest request) {
        long startTime = System.currentTimeMillis();

        String answer = qaService.ask(request.getQuery(), request.getSessionId());
        List<Conversation> history = qaService.getConversationHistory(
                request.getSessionId());

        // Extract citations from the last assistant message
        List<CitationRef> citations = List.of();
        if (!history.isEmpty()) {
            Conversation lastMsg = history.get(history.size() - 1);
            if (lastMsg.isAssistant() && lastMsg.getReferences() != null) {
                citations = lastMsg.getReferences();
            }
        }

        QaResponse response = QaResponse.builder()
                .sessionId(request.getSessionId())
                .query(request.getQuery())
                .answer(answer)
                .citations(citations)
                .sourcesCount(citations.size())
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();

        return ApiResponse.success(response);
    }

    @Operation(summary = "回答反馈", description = "对 AI 回答进行点赞或点踩")
    @PostMapping("/feedback")
    public ApiResponse<Void> feedback(@Valid @RequestBody FeedbackRequest request) {
        qaService.recordFeedback(request.getMessageId(), request.getFeedback());
        return ApiResponse.success();
    }

    @Operation(summary = "获取对话历史", description = "返回指定会话的完整对话记录")
    @GetMapping("/conversation/{sessionId}")
    public ApiResponse<List<Conversation>> getConversation(
            @Parameter(description = "会话 ID") @PathVariable String sessionId) {
        List<Conversation> history = qaService.getConversationHistory(sessionId);
        return ApiResponse.success(history);
    }
}
