package com.kb.application.service;

import com.kb.domain.chat.AssistantEvent;
import com.kb.domain.chat.BookingSlots;
import com.kb.domain.chat.ChatContextHolder;
import com.kb.domain.chat.ChatSession;
import com.kb.domain.chat.ChatSessionRepository;
import com.kb.domain.chat.PendingBooking;
import com.kb.domain.conversation.Conversation;
import com.kb.domain.conversation.ConversationRepository;
import com.kb.domain.rag.CancellationToken;
import com.kb.domain.rag.LlmService;
import com.kb.domain.rag.RetrievalResult;
import com.kb.infrastructure.common.BusinessException;
import com.kb.infrastructure.common.ErrorCode;
import com.kb.infrastructure.metrics.BusinessMetrics;
import com.kb.domain.rag.StreamCancelledException;
import com.kb.infrastructure.rag.rewrite.ContextualQueryRewriter;
import com.kb.infrastructure.security.SecurityFrameworkUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Application service for the Q&A (RAG) workflow orchestration.
 * <p>
 * Online pipeline:
 * <ol>
 *   <li>载入会话上下文（槽位 + 待确认动作）</li>
 *   <li>上下文感知改写：把追问补全成独立问题（"换成下沙校区呢？"）</li>
 *   <li>混合检索（ES 关键词 + Qdrant 向量 → RRF 融合）与重排</li>
 *   <li>意图路由：预约相关走 Function Calling，带上下文提示</li>
 *   <li>回读工具写入的槽位 / 待确认草稿，推送给前端</li>
 *   <li>消息与引用落库</li>
 * </ol>
 * </p>
 * <p>
 * <b>预约采用两段式</b>：工具只生成草稿，本服务检测用户"确认/取消"后
 * 才真正调用 CAS 下单，保证 AI 不会擅自替用户预约。
 * </p>
 * <p>
 * <b>Q-01 拆分</b>：本服务只做编排。回答生成路由委托 {@link AnswerPipeline}，
 * 问答缓存读写委托 {@link CacheGuard}，待确认预约动作（确认/取消/丢弃）委托
 * {@link PendingBookingExecutor}，行为与拆分前完全一致（SSE 协议、断连取消、
 * 缓存判定、两段式预约均不变）。
 * </p>
 *
 * @author forever-king
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QaApplicationService implements IQaApplicationService {

    private final ContextualQueryRewriter contextualRewriter;
    private final ConversationRepository conversationRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final BusinessMetrics metrics;
    private final StringRedisTemplate redisTemplate;
    private final AnswerPipeline pipeline;
    private final CacheGuard cacheGuard;
    private final PendingBookingExecutor pendingExecutor;

    /** 每日请求计数 key 的 TTL（小时），默认 25（覆盖次日零点），B-04 配置化 */
    @Value("${kb.qa.daily-counter-ttl-hours:25}")
    private int dailyCounterTtlHours;

    // ==================== 流式问答 ====================

    /**
     * Execute a Q&A request with streaming response.
     *
     * @param query       user's question
     * @param sessionId   conversation session ID (auto-generated if empty)
     * @param onToken     callback for each generated token (SSE push)
     * @param onCitations callback with citation list after generation completes
     * @param onMessageId callback with persisted assistant message id (for feedback)
     * @param onEvent     callback for structured assistant events (slots / confirm / action)
     * @return the complete answer text
     */
    public String askStreaming(String query, String sessionId,
                                Consumer<String> onToken,
                                Consumer<List<Conversation.CitationRef>> onCitations,
                                Consumer<Long> onMessageId,
                                Consumer<AssistantEvent> onEvent) {
        return askStreaming(query, sessionId, null, onToken, onCitations, onMessageId, onEvent);
    }

    @Override
    public String askStreaming(String query, String sessionId, Long currentUserId,
                                Consumer<String> onToken,
                                Consumer<List<Conversation.CitationRef>> onCitations,
                                Consumer<Long> onMessageId,
                                Consumer<AssistantEvent> onEvent) {
        // 3.12（深度审查 P2）：本重载不带 CancellationToken，调用方拿不到断连取消能力
        // （走它仅用于无取消需求的场景/测试；生产 SSE 入口 QaController 直用 8 参含 token 版）。
        return askStreaming(query, sessionId, currentUserId, CancellationToken.none(),
                onToken, onCitations, onMessageId, onEvent);
    }

    @Override
    public String askStreaming(String query, String sessionId, Long currentUserId,
                                CancellationToken cancellationToken,
                                Consumer<String> onToken,
                                Consumer<List<Conversation.CitationRef>> onCitations,
                                Consumer<Long> onMessageId,
                                Consumer<AssistantEvent> onEvent) {
        long startTime = System.currentTimeMillis();
        String sid = ensureSessionId(sessionId);
        Long userId = currentUserId != null ? currentUserId : SecurityFrameworkUtils.getLoginUserId();
        ChatSession session = chatSessionRepository.loadForUser(sid, userId);

        // 累积本次已生成的回答：客户端中途断连时用于尽力落库已生成部分，
        // 同时所有下游回调统一走 tokenCollector（回调本身可感知 sink 已取消）。
        StringBuilder partialAnswer = new StringBuilder();
        Consumer<String> tokenCollector = token -> {
            partialAnswer.append(token);
            if (onToken != null) {
                onToken.accept(token);
            }
        };

        try {
            // ---- Step 0: 上一轮遗留的"待确认动作"优先处理 ----
            String handledAction = handlePendingBookingAction(query, sid, userId, session,
                    tokenCollector, onCitations, onMessageId, onEvent, startTime);
            if (handledAction != null) {
                return handledAction;
            }

            // ---- Step 1: 上下文感知改写（追问补全 + 槽位继承），并透传会话槽位给 @Tool ----
            RewriteOutcome outcome = rewriteWithContext(query, sid, userId, session);
            String rewritten = outcome.query();
            List<LlmService.ChatMessage> history = outcome.history();

            // ---- Step 1.5: 纯知识类问题先查问答缓存 ----
            // 预约实时类（命中预约意图或会话已带槽位）一律不查不写，避免余量/档期过期；
            // 精确缓存未命中再查语义缓存（相似度阈值 0.95）。
            String cached = tryServeFromCache(query, rewritten, sid, userId, session,
                    tokenCollector, onCitations, onMessageId, startTime);
            if (cached != null) {
                return cached;
            }

            // ---- Step 2: 混合检索 + 重排（SSE 链路走图谱增强检索）----
            List<RetrievalResult> reranked = pipeline.retrieveAndRerank(rewritten, true);

            // ---- Step 3: 保存用户消息 ----
            conversationRepository.save(sid, "user", query, userId);

            // ---- Step 4: 生成回答（流式链路在客户端断连时抛 StreamCancelledException）----
            String fullAnswer = pipeline.generate(rewritten, reranked, history, session,
                    tokenCollector, cancellationToken);
            if (cancellationToken.isCancelled()) {
                // 生成已结束但连接恰在此刻断开：安静收尾，不向前端推送任何事件
                return finishAfterCancel(sid, userId, fullAnswer, startTime);
            }

            // 本地资料命中的知识类回答写缓存（精确 + 语义），预约/兜底类不写
            populateKnowledgeCacheIfApplicable(rewritten, session, reranked, fullAnswer, userId);

            // ---- Step 5: 回读会话（工具可能更新了槽位与待确认草稿）+ 引用落库 + 回调 + 指标 ----
            return persistAndNotify(sid, userId, fullAnswer, reranked, session,
                    onCitations, onMessageId, onEvent, startTime);

        } catch (StreamCancelledException cancelled) {
            // 客户端主动断开：不是错误，不推错误文案、不落错误消息；
            // 已生成的非空部分尽力落库（刷新页面后历史不缺这一轮）。
            log.info("客户端断开，问答已中止: sid={}, 已生成 {} 字",
                    sid, partialAnswer.length());
            // 3.12：单独计数"客户端断连中止"，区别于错误与正常完成
            metrics.recordStreamCancelled();
            return finishAfterCancel(sid, userId, partialAnswer.toString(), startTime);
        } catch (Exception e) {
            log.error("Q&A failed for query: {}", query, e);
            // B-02：不向用户下发底层异常原文，改通用文案；完整堆栈已进日志
            String errorAnswer = "抱歉，处理您的问题时遇到了错误，请稍后再试。";
            try {
                conversationRepository.save(sid, "assistant", errorAnswer, userId);
            } catch (Exception saveEx) {
                // 归属拦截等场景下错误消息也无法落库，不能让二次异常吞掉原始异常
                log.warn("错误消息落库失败: sessionId={}", sid, saveEx);
            }
            if (onToken != null) {
                onToken.accept(errorAnswer);
            }
            metrics.recordQaLatency(System.currentTimeMillis() - startTime);
            return errorAnswer;
        } finally {
            ChatContextHolder.clear();
        }
    }

    /**
     * 保留旧签名以便既有调用方平滑迁移：不带结构化事件回调。
     */
    @Override
    public String askStreaming(String query, String sessionId,
                               Consumer<String> onToken,
                               Consumer<List<Conversation.CitationRef>> onCitations,
                               Consumer<Long> onMessageId) {
        return askStreaming(query, sessionId, onToken, onCitations, onMessageId, null);
    }

    // ==================== 同步问答 ====================

    /**
     * Execute a Q&A request synchronously (non-streaming).
     */
    public String ask(String query, String sessionId) {
        long startTime = System.currentTimeMillis();
        String sid = ensureSessionId(sessionId);
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        ChatSession session = chatSessionRepository.loadForUser(sid, userId);

        try {
            String handledAction = handlePendingBookingAction(query, sid, userId, session,
                    null, null, null, null, startTime);
            if (handledAction != null) {
                return handledAction;
            }

            // 同步路径无 token 回调：改写仅维持槽位 / 上下文一致性
            RewriteOutcome outcome = rewriteWithContext(query, sid, userId, session);
            String rewritten = outcome.query();
            List<LlmService.ChatMessage> history = outcome.history();

            // 纯知识类问题先查缓存（同步路径无 token 回调）
            String cached = tryServeFromCache(query, rewritten, sid, userId, session,
                    null, null, null, startTime);
            if (cached != null) {
                return cached;
            }

            // 同步路径走关键词检索（非图谱增强）
            List<RetrievalResult> reranked = pipeline.retrieveAndRerank(rewritten, false);

            conversationRepository.save(sid, "user", query, userId);
            String answer = pipeline.generate(rewritten, reranked, history, session, null,
                    CancellationToken.none());

            populateKnowledgeCacheIfApplicable(rewritten, session, reranked, answer, userId);

            return persistAndNotify(sid, userId, answer, reranked, session,
                    null, null, null, startTime);
        } finally {
            ChatContextHolder.clear();
        }
    }

    /**
     * 客户端断连后的安静收尾：已生成的非空回答尽力落库（不带前端回调），
     * 空白回答不落库；任何落库异常都不能再抛出。
     */
    private String finishAfterCancel(String sid, Long userId, String partialAnswer, long startTime) {
        if (partialAnswer != null && !partialAnswer.isBlank()) {
            try {
                conversationRepository.save(sid, "assistant", partialAnswer, userId);
            } catch (Exception saveEx) {
                log.warn("断连后部分回答落库失败: sessionId={}", sid, saveEx);
            }
        }
        metrics.recordQaLatency(System.currentTimeMillis() - startTime);
        return partialAnswer == null ? "" : partialAnswer;
    }

    // ==================== 其余接口 ====================

    /**
     * Get conversation history for a session.
     * 归属校验：仅返回当前登录用户自己的消息；他人会话一律返回空，
     * 不暴露会话是否存在（4.1.13）。
     */
    public List<Conversation> getConversationHistory(String sessionId) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return conversationRepository.findBySession(sessionId, userId);
    }

    /**
     * Record user feedback on an answer.
     * 归属校验：消息必须属于当前登录用户，否则按"不存在或无权操作"拒绝。
     */
    public void recordFeedback(Long messageId, String feedback) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        boolean updated = conversationRepository.updateFeedback(messageId, feedback, userId);
        if (!updated) {
            throw new BusinessException.QaException(ErrorCode.QA_MESSAGE_NOT_FOUND,
                    "messageId=" + messageId);
        }
    }

    /**
     * 清空会话上下文（槽位与待确认草稿），消息历史不受影响。
     * 归属校验：仅允许操作当前登录用户自己的会话（4.1.13）。
     * MySQL 中尚无消息的会话（owner 未知）也允许执行——Redis 侧会再次
     * 按归属防御，清空一个不存在的 key 是幂等操作。
     */
    public void resetSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        Long owner = conversationRepository.findOwnerIdBySessionId(sessionId);
        if (owner != null && !owner.equals(userId)) {
            log.warn("拒绝跨用户重置会话: sessionId={}, owner={}, currentUser={}",
                    sessionId, owner, userId);
            throw new BusinessException.QaException(ErrorCode.QA_SESSION_FORBIDDEN,
                    "sessionId=" + sessionId);
        }
        chatSessionRepository.clear(sessionId, userId);
    }

    // ========== 编排模板（ask / askStreaming 共用，第14轮 D-B5 收敛重复） ==========

    /** 改写结果：改写后的问题 + 会话历史（生成回答仍需历史，故随结果一并返回）。 */
    private record RewriteOutcome(String query, List<LlmService.ChatMessage> history) {
    }

    /**
     * Step 0：处理上一轮遗留的"待确认预约动作"。
     * 返回非 null 表示已代为处理完成（确认/取消已执行），调用方直接返回该结果；
     * null 表示继续主线（草稿已过期则清掉，转移话题则丢弃旧草稿）。
     */
    private String handlePendingBookingAction(String query, String sid, Long userId, ChatSession session,
                                              Consumer<String> tokenCollector,
                                              Consumer<List<Conversation.CitationRef>> onCitations,
                                              Consumer<Long> onMessageId,
                                              Consumer<AssistantEvent> onEvent,
                                              long startTime) {
        PendingBooking pending = session.getPendingBooking();
        if (pending == null) {
            return null;
        }
        if (pending.isExpired()) {
            session.setPendingBooking(null);
            chatSessionRepository.save(session);
            return null;
        }
        PendingBookingExecutor.ConfirmIntent intent = pendingExecutor.detectConfirmIntent(query);
        if (intent == PendingBookingExecutor.ConfirmIntent.CONFIRM) {
            return pendingExecutor.executePending(sid, userId, query, session, pending, true,
                    tokenCollector, onCitations, onMessageId, onEvent, startTime);
        }
        if (intent == PendingBookingExecutor.ConfirmIntent.REJECT) {
            return pendingExecutor.executePending(sid, userId, query, session, pending, false,
                    tokenCollector, onCitations, onMessageId, onEvent, startTime);
        }
        // 用户转移话题：丢弃上一份草稿，避免误确认
        pendingExecutor.discardPending(session, pending);
        return null;
    }

    /**
     * Step 1：上下文感知改写（追问补全 + 槽位继承），并透传会话槽位给 @Tool 回退使用。
     */
    private RewriteOutcome rewriteWithContext(String query, String sid, Long userId, ChatSession session) {
        List<LlmService.ChatMessage> history = pipeline.loadHistory(sid, userId);
        ContextualQueryRewriter.RewriteResult rewrite =
                contextualRewriter.rewrite(query, history, session.slotsOrEmpty());
        String rewritten = rewrite.query();
        session.setSlots(rewrite.slots());
        chatSessionRepository.save(session);

        // 供 @Tool 回退使用（LLM 漏传参数时用会话槽位兜底）
        ChatContextHolder.set(new ChatContextHolder.ChatContext(sid, userId, session.getSlots()));
        return new RewriteOutcome(rewritten, history);
    }

    /**
     * Step 1.5：纯知识类问题先查问答缓存（精确 + 语义）。
     * 命中返回缓存答案；未命中返回 null 并记录 miss 指标。预约实时类由
     * isKnowledgeOnlyQuery 排除（命中预约意图或会话已带槽位一律不查不写）。
     */
    private String tryServeFromCache(String query, String rewritten, String sid, Long userId, ChatSession session,
                                     Consumer<String> tokenCollector,
                                     Consumer<List<Conversation.CitationRef>> onCitations,
                                     Consumer<Long> onMessageId,
                                     long startTime) {
        if (!cacheGuard.isKnowledgeOnlyQuery(rewritten, session)) {
            return null;
        }
        String cached = cacheGuard.answerFromCache(query, rewritten, sid, userId,
                tokenCollector, onCitations, onMessageId, startTime);
        if (cached != null) {
            return cached;
        }
        metrics.recordCacheMiss();
        return null;
    }

    /**
     * 本地资料命中的知识类回答写缓存（精确 + 语义），预约/兜底类不写。
     */
    private void populateKnowledgeCacheIfApplicable(String rewritten, ChatSession session,
                                                    List<RetrievalResult> reranked, String answer,
                                                    Long userId) {
        if (cacheGuard.isKnowledgeOnlyQuery(rewritten, session) && reranked != null && !reranked.isEmpty()
                && answer != null && !answer.isBlank()) {
            cacheGuard.populateKnowledgeCache(rewritten, answer, pipeline.buildCitations(reranked), userId);
        }
    }

    /**
     * Step 5：回读会话（工具可能更新了槽位与待确认草稿）、引用落库、前端回调与指标收尾。
     */
    private String persistAndNotify(String sid, Long userId, String fullAnswer,
                                    List<RetrievalResult> reranked, ChatSession session,
                                    Consumer<List<Conversation.CitationRef>> onCitations,
                                    Consumer<Long> onMessageId,
                                    Consumer<AssistantEvent> onEvent,
                                    long startTime) {
        ChatSession latest = chatSessionRepository.find(sid).orElse(session);
        chatSessionRepository.save(latest);

        List<Conversation.CitationRef> citations = pipeline.buildCitations(reranked);
        Long messageId = conversationRepository.saveWithReferences(
                sid, "assistant", fullAnswer, citations, userId);
        if (onMessageId != null && messageId != null) {
            onMessageId.accept(messageId);
        }
        if (onCitations != null) {
            onCitations.accept(citations);
        }
        if (onEvent != null) {
            BookingSlots slots = latest.slotsOrEmpty();
            if (!slots.isEmpty()) {
                onEvent.accept(AssistantEvent.slots(slots));
            }
            if (latest.getPendingBooking() != null) {
                onEvent.accept(AssistantEvent.confirm(latest.getPendingBooking()));
            }
        }

        metrics.recordQaRequest();
        metrics.recordQaLatency(System.currentTimeMillis() - startTime);
        incrementDailyCounter();
        return fullAnswer;
    }

    // ========== Private Helpers ==========

    /**
     * 每日请求计数：按自然日累计，TTL 25 小时保证次日零点后自动过期。
     * 过期时间可配置（B-04：{@code kb.qa.daily-counter-ttl-hours}，默认 25）。
     */
    private void incrementDailyCounter() {
        try {
            String key = "stats:requests:" + java.time.LocalDate.now();
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, dailyCounterTtlHours, java.util.concurrent.TimeUnit.HOURS);
        } catch (Exception e) {
            log.debug("Failed to increment daily counter: {}", e.getMessage());
        }
    }

    private String ensureSessionId(String sessionId) {
        return (sessionId != null && !sessionId.isEmpty())
                ? sessionId : UUID.randomUUID().toString();
    }
}