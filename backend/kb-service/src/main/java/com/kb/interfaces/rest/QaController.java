package com.kb.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.application.service.IQaApplicationService;
import com.kb.domain.conversation.Conversation;
import com.kb.domain.conversation.Conversation.CitationRef;
import com.kb.interfaces.dto.ApiResponse;
import com.kb.interfaces.dto.FeedbackRequest;
import com.kb.interfaces.dto.QaRequest;
import com.kb.interfaces.dto.QaResponse;
import com.kb.infrastructure.concurrency.SseConcurrencyLimiter;
import com.kb.infrastructure.ratelimit.annotations.RateLimit;
import com.kb.infrastructure.security.SecurityFrameworkUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * REST controller for Q&A with SSE streaming.
 * @author forever-king
 */
@Tag(name = "智能问答", description = "基于 RAG 的文档问答，支持同步和 SSE 流式两种模式")
@Slf4j
@RestController
@RequestMapping("/qa")
@RequiredArgsConstructor
public class QaController {

    /** 流式结束信号：前端收到后才主动 close，避免 EventSource 走 error 并自动重连 */
    private static final String DONE_SIGNAL = "[DONE]";

    /** Q&A 应用服务 */
    private final IQaApplicationService qaService;

    /** JSON 序列化/反序列化工具 */
    private final ObjectMapper objectMapper;

    /** SSE 在途连接的全局限流器：并发问答数超出上限时快速失败，防止慢 LLM 拖垮实例 */
    private final SseConcurrencyLimiter sseConcurrencyLimiter;

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
     * eventSource.addEventListener('slots',   (e) => renderSlots(JSON.parse(e.data)));
     * eventSource.addEventListener('confirm', (e) => renderConfirmCard(JSON.parse(e.data)));
     * eventSource.addEventListener('action',  (e) => renderActionResult(JSON.parse(e.data)));
     * </pre>
     * 事件说明：
     * <ul>
     *   <li>{@code message}（默认）—— 回答 token 流</li>
     *   <li>{@code citations} —— 引用来源列表</li>
     *   <li>{@code messageId} —— 助手消息 ID，供点赞/点踩</li>
     *   <li>{@code slots} —— 当前会话累积的预约条件（校区/分类/日期/时段）</li>
     *   <li>{@code confirm} —— 待用户确认的预约草稿，前端应渲染确认/取消按钮</li>
     *   <li>{@code action} —— 预约或取消动作的执行结果</li>
     * </ul>
     */
    @RateLimit(permits = 20, seconds = 60, message = "问答请求过于频繁，请稍后再试")
    @Operation(summary = "流式问答（SSE）", description = "通过 Server-Sent Events 逐字流式返回 AI 回答，支持会话上下文")
    @GetMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<?>> askStreaming(
            @Parameter(description = "用户问题") @RequestParam String query,
            @Parameter(description = "会话 ID，不传则自动生成新会话") @RequestParam(required = false, defaultValue = "") String sessionId) {

        // 在 Servlet 线程内解析身份：SSE 的 Flux 会在异步线程执行，
        // 不能依赖 SecurityContext 的线程继承
        Long userId = SecurityFrameworkUtils.getLoginUserId();

        // 实例级 SSE 并发兜底：在响应头未提交前拒绝，直接返回 HTTP 503
        // （此时还未建立 text/event-stream 响应，前端可按普通错误提示重试）
        if (!sseConcurrencyLimiter.tryAcquire()) {
            log.warn("SSE 问答并发已达上限 {}，拒绝新连接", sseConcurrencyLimiter.getMaxConcurrent());
            return Flux.error(new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "当前问答用户较多，请稍后再试"));
        }

        // 显式指定泛型：链式调用 subscribeOn 后编译器无法从目标类型反推 T
        return Flux.<ServerSentEvent<?>>create(sink -> {
            try {
                qaService.askStreaming(query, sessionId, userId,
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
                        },
                        event -> {
                            // 结构化事件：槽位更新 / 待确认预约 / 预约动作结果
                            if (sink.isCancelled() || event == null) {
                                return;
                            }
                            try {
                                sink.next(ServerSentEvent.builder()
                                        .event(event.getType())
                                        .data(objectMapper.writeValueAsString(event.getPayload()))
                                        .build());
                            } catch (Exception e) {
                                log.warn("Failed to serialize assistant event: {}", event.getType(), e);
                            }
                        }
                );
                // 显式发一个结束信号再关闭。
                // 此前直接 complete()：浏览器 EventSource 收到的是"连接被关闭"，
                // 会触发 error 并准备自动重连，前端只能靠 onerror 里的 es.close() 兜底——
                // 任何未 close 的错误路径都会把同一轮问答再跑一遍（重复落库用户消息、
                // 重复调 LLM，甚至重复执行待确认预约）。前端的 [DONE] 分支也一直是死代码。
                sink.next(ServerSentEvent.builder().data(DONE_SIGNAL).build());
                sink.complete();
            } catch (Exception e) {
                log.error("SSE streaming error", e);
                sink.error(e);
            }
        })
        // 订阅到 boundedElastic：本服务是 Servlet 栈，Flux 默认在容器线程上订阅，
        // 而 askStreaming 全程同步阻塞（检索 + LLM + Feign），会一直占住 Tomcat 工作线程。
        // 并发问答数因此约等于可用线程数，LLM 变慢时少量请求即可打满、健康检查也挂。
        .subscribeOn(Schedulers.boundedElastic())
        // 无论正常结束、异常还是客户端断开（cancel），都归还并发名额。
        // 许可在控制器方法体内、Flux 订阅前获取，doFinally 恰好触发一次，保证不漏不重。
        .doFinally(signal -> sseConcurrencyLimiter.release());
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

    @Operation(summary = "清空会话上下文",
            description = "清除多轮对话累积的预约条件与待确认草稿；消息历史保留，便于回看")
    @PostMapping("/session/{sessionId}/reset")
    public ApiResponse<Void> resetSession(
            @Parameter(description = "会话 ID") @PathVariable String sessionId) {
        qaService.resetSession(sessionId);
        return ApiResponse.success();
    }
}
