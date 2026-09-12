package com.kb.application.service;

import com.kb.domain.chat.AssistantEvent;
import com.kb.domain.chat.BookingSlots;
import com.kb.domain.chat.ChatContextHolder;
import com.kb.domain.chat.ChatSession;
import com.kb.domain.chat.ChatSessionRepository;
import com.kb.domain.chat.PendingBooking;
import com.kb.domain.conversation.Conversation;
import com.kb.domain.conversation.ConversationRepository;
import com.kb.domain.rag.LlmService;
import com.kb.domain.rag.RetrievalResult;
import com.kb.domain.rag.SearchService;
import com.kb.domain.rag.RerankerService;
import com.kb.infrastructure.client.CasClient;
import com.kb.infrastructure.client.CasResult;
import com.kb.infrastructure.client.dto.CasBookingResult;
import com.kb.infrastructure.metrics.BusinessMetrics;
import com.kb.infrastructure.rag.graph.GraphAssistedRetriever;
import com.kb.infrastructure.rag.rewrite.ContextualQueryRewriter;
import com.kb.infrastructure.security.SecurityFrameworkUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
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
 *
 * @author forever-king
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QaApplicationService implements IQaApplicationService {

    private final SearchService searchService;
    private final RerankerService rerankerService;
    private final LlmService llmService;
    private final ConversationRepository conversationRepository;
    private final ContextualQueryRewriter contextualRewriter;
    private final GraphAssistedRetriever graphRetriever;
    private final BusinessMetrics metrics;
    private final StringRedisTemplate redisTemplate;
    private final ChatSessionRepository chatSessionRepository;
    private final CasClient casClient;

    /** 注入 LLM 的历史消息条数（对话表读取上限） */
    private static final int HISTORY_LIMIT = 8;

    /** 用户对本轮"是否确认"的答复 */
    private enum ConfirmIntent { CONFIRM, REJECT, NONE }

    /**
     * 判定"确认"的关键词。刻意只收多字词：单字（如"行""对"）在中文里歧义太大，
     * 容易把"换个时间行不行"误判成确认。
     */
    private static final List<String> POSITIVE_WORDS = List.of(
            "确认", "确定", "是的", "好的", "可以", "没问题", "就这样", "就这个", "就按",
            "提交", "预约吧", "帮我约", "帮我订", "ok", "yes");

    /**
     * 判定"取消"的关键词，其中"不确认/不确定/不行"必须排在肯定词之前判断。
     */
    private static final List<String> NEGATIVE_WORDS = List.of(
            "不确认", "不确定", "不用", "不要", "不是", "不行", "取消", "算了", "不约",
            "放弃", "先别", "别了", "再想想", "拒绝", "no");

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
        long startTime = System.currentTimeMillis();
        String sid = ensureSessionId(sessionId);
        Long userId = currentUserId != null ? currentUserId : SecurityFrameworkUtils.getLoginUserId();
        ChatSession session = chatSessionRepository.loadOrCreate(sid, userId);

        try {
            // ---- Step 0: 上一轮遗留的"待确认动作"优先处理 ----
            PendingBooking pending = session.getPendingBooking();
            if (pending != null) {
                if (pending.isExpired()) {
                    session.setPendingBooking(null);
                    chatSessionRepository.save(session);
                } else {
                    ConfirmIntent intent = detectConfirmIntent(query);
                    if (intent == ConfirmIntent.CONFIRM) {
                        return executePending(sid, query, session, pending, true,
                                onToken, onCitations, onMessageId, onEvent, startTime);
                    }
                    if (intent == ConfirmIntent.REJECT) {
                        return executePending(sid, query, session, pending, false,
                                onToken, onCitations, onMessageId, onEvent, startTime);
                    }
                    // 用户转移话题：丢弃上一份草稿，避免误确认
                    discardPending(session, pending);
                }
            }

            // ---- Step 1: 上下文感知改写（追问补全 + 槽位继承）----
            List<LlmService.ChatMessage> history = loadHistory(sid);
            ContextualQueryRewriter.RewriteResult rewrite =
                    contextualRewriter.rewrite(query, history, session.slotsOrEmpty());
            String rewritten = rewrite.query();
            session.setSlots(rewrite.slots());
            chatSessionRepository.save(session);

            // 供 @Tool 回退使用（LLM 漏传参数时用会话槽位兜底）
            ChatContextHolder.set(new ChatContextHolder.ChatContext(sid, userId, session.getSlots()));

            // ---- Step 2: 混合检索 + 重排 ----
            long retrievalStart = System.currentTimeMillis();
            List<RetrievalResult> retrieved = graphRetriever.retrieve(rewritten);
            metrics.recordRetrievalLatency(System.currentTimeMillis() - retrievalStart);
            List<RetrievalResult> reranked = rerankerService.rerank(rewritten, retrieved);

            // ---- Step 3: 保存用户消息 ----
            conversationRepository.save(sid, "user", query);

            // ---- Step 4: 生成回答 ----
            String fullAnswer = generate(rewritten, reranked, history, session, onToken);

            // ---- Step 5: 回读会话（工具可能更新了槽位与待确认草稿）----
            ChatSession latest = chatSessionRepository.find(sid).orElse(session);
            chatSessionRepository.save(latest);

            List<Conversation.CitationRef> citations = buildCitations(reranked);

            Long messageId = conversationRepository.saveWithReferences(
                    sid, "assistant", fullAnswer, citations);
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

        } catch (Exception e) {
            log.error("Q&A failed for query: {}", query, e);
            String errorAnswer = "抱歉，处理您的问题时遇到了错误：" + e.getMessage();
            conversationRepository.save(sid, "assistant", errorAnswer);
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
        ChatSession session = chatSessionRepository.loadOrCreate(sid, userId);

        try {
            PendingBooking pending = session.getPendingBooking();
            if (pending != null && !pending.isExpired()) {
                ConfirmIntent intent = detectConfirmIntent(query);
                if (intent == ConfirmIntent.CONFIRM) {
                    return executePending(sid, query, session, pending, true,
                            null, null, null, null, startTime);
                }
                if (intent == ConfirmIntent.REJECT) {
                    return executePending(sid, query, session, pending, false,
                            null, null, null, null, startTime);
                }
                discardPending(session, pending);
            } else if (pending != null) {
                session.setPendingBooking(null);
                chatSessionRepository.save(session);
            }

            List<LlmService.ChatMessage> history = loadHistory(sid);
            ContextualQueryRewriter.RewriteResult rewrite =
                    contextualRewriter.rewrite(query, history, session.slotsOrEmpty());
            String rewritten = rewrite.query();
            session.setSlots(rewrite.slots());
            chatSessionRepository.save(session);

            ChatContextHolder.set(new ChatContextHolder.ChatContext(sid, userId, session.getSlots()));

            long retrievalStart = System.currentTimeMillis();
            List<RetrievalResult> retrieved = searchService.search(rewritten);
            metrics.recordRetrievalLatency(System.currentTimeMillis() - retrievalStart);
            List<RetrievalResult> reranked = rerankerService.rerank(rewritten, retrieved);

            conversationRepository.save(sid, "user", query);
            String answer = generate(rewritten, reranked, history, session, null);

            ChatSession latest = chatSessionRepository.find(sid).orElse(session);
            chatSessionRepository.save(latest);

            List<Conversation.CitationRef> citations = buildCitations(reranked);
            conversationRepository.saveWithReferences(sid, "assistant", answer, citations);

            metrics.recordQaRequest();
            metrics.recordQaLatency(System.currentTimeMillis() - startTime);
            incrementDailyCounter();
            return answer;
        } finally {
            ChatContextHolder.clear();
        }
    }

    // ==================== 生成路由 ====================

    /**
     * 意图路由：
     * <ul>
     *   <li>预约相关（含会话中已累积槽位）→ Function Calling 链路；</li>
     *   <li>本地资料有结果 → RAG；本地无法回答 → DeepSeek 兜底；</li>
     *   <li>本地无召回 → 直接流式兜底。</li>
     * </ul>
     */
    private String generate(String query, List<RetrievalResult> reranked,
                            List<LlmService.ChatMessage> history,
                            ChatSession session, Consumer<String> onToken) {
        boolean appointmentRelated = isAppointmentQuery(query) || !session.slotsOrEmpty().isEmpty();
        if (appointmentRelated) {
            String contextHint = session.slotsOrEmpty().describe();
            return llmService.generateAnswerWithTools(query, reranked, history, onToken, contextHint);
        }
        if (reranked != null && !reranked.isEmpty()) {
            // 先同步生成 RAG 回答判断是否能回答，避免"无法回答"推流后又推兜底导致拼接
            String ragAnswer = llmService.generateAnswer(query, reranked, history);
            if (looksLikeNoAnswer(ragAnswer)) {
                ragAnswer = llmService.generateAnswerDirect(query, history);
            }
            if (onToken != null) {
                onToken.accept(ragAnswer);
            }
            return ragAnswer;
        }
        return llmService.generateAnswerDirectStreaming(query, history, onToken);
    }

    // ==================== 待确认动作的执行 ====================

    private String executePending(String sid, String query, ChatSession session, PendingBooking pending,
                                  boolean confirmed,
                                  Consumer<String> onToken,
                                  Consumer<List<Conversation.CitationRef>> onCitations,
                                  Consumer<Long> onMessageId,
                                  Consumer<AssistantEvent> onEvent,
                                  long startTime) {
        conversationRepository.save(sid, "user", query);

        String answer;
        CasResult<CasBookingResult> result = null;
        if (!confirmed) {
            // 放弃：BOOK 动作需要通知 CAS 丢弃草稿，CANCEL 动作本来就还没执行
            if (PendingBooking.ACTION_BOOK.equals(pending.getAction()) && pending.getDraftId() != null) {
                try {
                    casClient.discardBookingDraft(pending.getDraftId());
                } catch (Exception e) {
                    log.warn("丢弃预约草稿失败: draftId={}", pending.getDraftId(), e);
                }
            }
            answer = "好的，已取消本次操作，没有产生任何变更。"
                    + "如果你想换时间、换校区或换其他服务，直接告诉我就行。";
        } else {
            try {
                result = PendingBooking.ACTION_CANCEL.equals(pending.getAction())
                        ? casClient.cancelBooking(pending.getOrderId())
                        : casClient.confirmBookingDraft(pending.getDraftId());
            } catch (Exception e) {
                log.error("执行预约动作失败", e);
                answer = "操作失败：预约服务暂时不可用，请稍后再试。";
            }
            if (result == null) {
                answer = "操作失败：预约服务暂时不可用，请稍后再试。";
            } else if (result.isSuccess() && result.getData() != null) {
                answer = result.getData().getMessage();
            } else {
                answer = "操作失败：" + (result.getMessage() == null ? "预约服务返回异常" : result.getMessage());
            }
        }

        session.setPendingBooking(null);
        // 预约完成后清空槽位，避免下一次提问沿用已完成的预约条件
        if (confirmed) {
            session.setSlots(new BookingSlots());
        }
        chatSessionRepository.save(session);

        if (onToken != null) {
            onToken.accept(answer);
        }
        if (onEvent != null) {
            if (confirmed && result != null && result.getData() != null) {
                onEvent.accept(AssistantEvent.action(result.getData()));
            }
            onEvent.accept(AssistantEvent.slots(session.slotsOrEmpty()));
        }
        if (onCitations != null) {
            onCitations.accept(List.of());
        }

        Long messageId = conversationRepository.saveWithReferences(sid, "assistant", answer, List.of());
        if (onMessageId != null && messageId != null) {
            onMessageId.accept(messageId);
        }

        metrics.recordQaRequest();
        metrics.recordQaLatency(System.currentTimeMillis() - startTime);
        return answer;
    }

    private void discardPending(ChatSession session, PendingBooking pending) {
        if (PendingBooking.ACTION_BOOK.equals(pending.getAction()) && pending.getDraftId() != null) {
            try {
                casClient.discardBookingDraft(pending.getDraftId());
            } catch (Exception e) {
                log.warn("丢弃过期预约草稿失败: draftId={}", pending.getDraftId(), e);
            }
        }
        session.setPendingBooking(null);
        chatSessionRepository.save(session);
    }

    // ==================== 上下文 ====================

    private List<LlmService.ChatMessage> loadHistory(String sessionId) {
        List<Conversation> messages = conversationRepository.getRecentMessages(sessionId, HISTORY_LIMIT);
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        return messages.stream()
                .filter(m -> m.getContent() != null && !m.getContent().isBlank())
                .map(m -> "user".equalsIgnoreCase(m.getRole())
                        ? LlmService.ChatMessage.user(m.getContent())
                        : LlmService.ChatMessage.assistant(m.getContent()))
                .toList();
    }

    private ConfirmIntent detectConfirmIntent(String query) {
        if (query == null || query.isBlank()) {
            return ConfirmIntent.NONE;
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        // 长句基本不是简单的确认/取消，交给 LLM 正常理解
        if (q.length() > 30) {
            return ConfirmIntent.NONE;
        }
        // 先判否定：避免"不确认""不用了"被肯定词命中
        for (String w : NEGATIVE_WORDS) {
            if (q.contains(w)) {
                return ConfirmIntent.REJECT;
            }
        }
        for (String w : POSITIVE_WORDS) {
            if (q.contains(w)) {
                return ConfirmIntent.CONFIRM;
            }
        }
        return ConfirmIntent.NONE;
    }

    // ==================== 其余接口 ====================

    /**
     * Get conversation history for a session.
     */
    public List<Conversation> getConversationHistory(String sessionId) {
        return conversationRepository.findBySessionId(sessionId);
    }

    /**
     * Record user feedback on an answer.
     */
    public void recordFeedback(Long messageId, String feedback) {
        conversationRepository.updateFeedback(messageId, feedback);
    }

    /**
     * 清空会话上下文（槽位与待确认草稿），消息历史不受影响
     */
    public void resetSession(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            chatSessionRepository.clear(sessionId);
        }
    }

    // ========== Private Helpers ==========

    /**
     * 意图路由（Intent Routing）：判断用户问题是否可能涉及"实时预约数据"
     *
     * <p>命中关键词的问题会进入带工具集的 Function Calling 链路；
     * 在工具链路内，<b>是否真正调用工具由 LLM 自主决定</b>。
     *
     * <p>关键词覆盖：可预约 / 余量 / 会议室 / 设备借用 / 咨询 / 自习室 / 场地等预约场景
     */
    private static final List<String> APPOINTMENT_KEYWORDS = List.of(
            "可预约", "预约", "余量", "会议室", "设备", "咨询", "自习室", "场地", "借用",
            "有哪些服务", "还有哪些", "能不能约", "怎么约", "怎么预约", "空闲", "名额",
            "仓前", "下沙", "教室", "教师", "老师", "取消预约", "我的预约");

    private boolean isAppointmentQuery(String q) {
        if (q == null || q.isEmpty()) {
            return false;
        }
        return APPOINTMENT_KEYWORDS.stream().anyMatch(q::contains);
    }

    /** RAG 回答包含"无法回答"信号时，判定本地资料未真正回答问题，触发 DeepSeek 兜底 */
    private boolean looksLikeNoAnswer(String answer) {
        if (answer == null || answer.isBlank()) {
            return true;
        }
        String[] markers = {
                "无法回答", "没有相关", "未包含", "暂无法", "没有找到", "未找到",
                "无法根据", "无法为您", "没有足够的", "文档中未"
        };
        return java.util.Arrays.stream(markers).anyMatch(answer::contains);
    }

    private void incrementDailyCounter() {
        try {
            String key = "stats:requests:" + java.time.LocalDate.now();
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 25, java.util.concurrent.TimeUnit.HOURS);
        } catch (Exception e) {
            log.debug("Failed to increment daily counter: {}", e.getMessage());
        }
    }

    private String ensureSessionId(String sessionId) {
        return (sessionId != null && !sessionId.isEmpty())
                ? sessionId : UUID.randomUUID().toString();
    }

    private List<Conversation.CitationRef> buildCitations(List<RetrievalResult> results) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        return results.stream()
                .map(r -> Conversation.CitationRef.builder()
                        .documentId(r.getDocumentId())
                        .documentTitle(r.getDocumentTitle())
                        .chunkId(r.getChunkId())
                        .chunkIndex(r.getChunkIndex())
                        .snippet(r.getSnippet(120))
                        .score(r.getScore())
                        .build())
                .toList();
    }
}
