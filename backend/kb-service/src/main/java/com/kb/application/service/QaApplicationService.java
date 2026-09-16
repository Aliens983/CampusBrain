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
import com.kb.infrastructure.cache.QaCacheService;
import com.kb.infrastructure.cache.SemanticCacheService;
import com.kb.infrastructure.common.BusinessException;
import com.kb.infrastructure.common.ErrorCode;
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
    private final QaCacheService qaCacheService;
    private final SemanticCacheService semanticCacheService;

    /** 注入 LLM 的历史消息条数（对话表读取上限） */
    private static final int HISTORY_LIMIT = 8;

    /** 用户对本轮"是否确认"的答复 */
    private enum ConfirmIntent { CONFIRM, REJECT, NONE }

    /**
     * 判定"确认"的关键词。刻意只收多字词：单字（如"行""对"）在中文里歧义太大，
     * 容易把"换个时间行不行"误判成确认。
     * <p>
     * 3.3.3：已移除"提交"——它高度歧义（"怎么提交？""提交按钮在哪"是疑问句而非确认），
     * 确认提交由"确认/确定/好的/预约吧/帮我约"等明确表达承担；{@link #detectConfirmIntent}
     * 另对"好的，帮我查余量"这类"应答词 + 新请求分句"做排除。
     */
    private static final List<String> POSITIVE_WORDS = List.of(
            "确认", "确定", "是的", "好的", "可以", "没问题", "就这样", "就这个", "就按",
            "预约吧", "帮我约", "帮我订", "ok", "yes");

    /**
     * 判定"取消"的关键词，其中"不确认/不确定/不行"必须排在肯定词之前判断。
     */
    private static final List<String> NEGATIVE_WORDS = List.of(
            "不确认", "不确定", "不用", "不要", "不是", "不行", "取消", "算了", "不约",
            "放弃", "先别", "别了", "再想想", "拒绝", "no");

    /**
     * 3.3.3：应答词之后若接上这些"新请求"信号，说明用户是在借应答口吻发起另一个问题，
     * 而不是确认当前草稿（如"好的，怎么预约？""可以，帮我查下还有多少名额"）。
     */
    private static final List<String> FOLLOW_UP_MARKERS = List.of(
            "怎么", "如何", "请问", "帮我查", "查一下", "查下", "看看", "能不能",
            "可不可以", "多少", "哪里", "哪儿", "还有", "换一个", "换个", "再说");

    /** 纯应答词（仅当它们作为开头、后面又跟了新请求时，才需要排除误判） */
    private static final List<String> ACK_PREFIXES = List.of(
            "好的", "好吧", "是的", "可以", "没问题", "确定", "确认", "ok", "yes");

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
        ChatSession session = chatSessionRepository.loadForUser(sid, userId);

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
                        return executePending(sid, userId, query, session, pending, true,
                                onToken, onCitations, onMessageId, onEvent, startTime);
                    }
                    if (intent == ConfirmIntent.REJECT) {
                        return executePending(sid, userId, query, session, pending, false,
                                onToken, onCitations, onMessageId, onEvent, startTime);
                    }
                    // 用户转移话题：丢弃上一份草稿，避免误确认
                    discardPending(session, pending);
                }
            }

            // ---- Step 1: 上下文感知改写（追问补全 + 槽位继承）----
            List<LlmService.ChatMessage> history = loadHistory(sid, userId);
            ContextualQueryRewriter.RewriteResult rewrite =
                    contextualRewriter.rewrite(query, history, session.slotsOrEmpty());
            String rewritten = rewrite.query();
            session.setSlots(rewrite.slots());
            chatSessionRepository.save(session);

            // 供 @Tool 回退使用（LLM 漏传参数时用会话槽位兜底）
            ChatContextHolder.set(new ChatContextHolder.ChatContext(sid, userId, session.getSlots()));

            // ---- Step 1.5: 纯知识类问题先查问答缓存 ----
            // 预约实时类（命中预约意图或会话已带槽位）一律不查不写，避免余量/档期过期；
            // 精确缓存未命中再查语义缓存（相似度阈值 0.95）。
            if (isKnowledgeOnlyQuery(rewritten, session)) {
                String cached = answerFromCache(query, rewritten, sid, userId,
                        onToken, onCitations, onMessageId, onEvent, startTime);
                if (cached != null) {
                    return cached;
                }
                metrics.recordCacheMiss();
            }

            // ---- Step 2: 混合检索 + 重排 ----
            long retrievalStart = System.currentTimeMillis();
            List<RetrievalResult> retrieved = graphRetriever.retrieve(rewritten);
            metrics.recordRetrievalLatency(System.currentTimeMillis() - retrievalStart);
            List<RetrievalResult> reranked = rerankerService.rerank(rewritten, retrieved);

            // ---- Step 3: 保存用户消息 ----
            conversationRepository.save(sid, "user", query, userId);

            // ---- Step 4: 生成回答 ----
            String fullAnswer = generate(rewritten, reranked, history, session, onToken);

            // 本地资料命中的知识类回答写缓存（精确 + 语义），预约/兜底类不写
            if (isKnowledgeOnlyQuery(rewritten, session) && reranked != null && !reranked.isEmpty()
                    && fullAnswer != null && !fullAnswer.isBlank()) {
                populateKnowledgeCache(rewritten, fullAnswer, buildCitations(reranked));
            }

            // ---- Step 5: 回读会话（工具可能更新了槽位与待确认草稿）----
            ChatSession latest = chatSessionRepository.find(sid).orElse(session);
            chatSessionRepository.save(latest);

            List<Conversation.CitationRef> citations = buildCitations(reranked);

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

        } catch (Exception e) {
            log.error("Q&A failed for query: {}", query, e);
            String errorAnswer = "抱歉，处理您的问题时遇到了错误：" + e.getMessage();
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
            PendingBooking pending = session.getPendingBooking();
            if (pending != null && !pending.isExpired()) {
                ConfirmIntent intent = detectConfirmIntent(query);
                if (intent == ConfirmIntent.CONFIRM) {
                    return executePending(sid, userId, query, session, pending, true,
                            null, null, null, null, startTime);
                }
                if (intent == ConfirmIntent.REJECT) {
                    return executePending(sid, userId, query, session, pending, false,
                            null, null, null, null, startTime);
                }
                discardPending(session, pending);
            } else if (pending != null) {
                session.setPendingBooking(null);
                chatSessionRepository.save(session);
            }

            List<LlmService.ChatMessage> history = loadHistory(sid, userId);
            ContextualQueryRewriter.RewriteResult rewrite =
                    contextualRewriter.rewrite(query, history, session.slotsOrEmpty());
            String rewritten = rewrite.query();
            session.setSlots(rewrite.slots());
            chatSessionRepository.save(session);

            ChatContextHolder.set(new ChatContextHolder.ChatContext(sid, userId, session.getSlots()));

            // 纯知识类问题先查缓存（同步路径无 token 回调）
            if (isKnowledgeOnlyQuery(rewritten, session)) {
                String cached = answerFromCache(query, rewritten, sid, userId,
                        null, null, null, null, startTime);
                if (cached != null) {
                    return cached;
                }
                metrics.recordCacheMiss();
            }

            long retrievalStart = System.currentTimeMillis();
            List<RetrievalResult> retrieved = searchService.search(rewritten);
            metrics.recordRetrievalLatency(System.currentTimeMillis() - retrievalStart);
            List<RetrievalResult> reranked = rerankerService.rerank(rewritten, retrieved);

            conversationRepository.save(sid, "user", query, userId);
            String answer = generate(rewritten, reranked, history, session, null);

            if (isKnowledgeOnlyQuery(rewritten, session) && reranked != null && !reranked.isEmpty()
                    && answer != null && !answer.isBlank()) {
                populateKnowledgeCache(rewritten, answer, buildCitations(reranked));
            }

            ChatSession latest = chatSessionRepository.find(sid).orElse(session);
            chatSessionRepository.save(latest);

            List<Conversation.CitationRef> citations = buildCitations(reranked);
            conversationRepository.saveWithReferences(sid, "assistant", answer, citations, userId);

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

    // ==================== 知识问答缓存 ====================

    /**
     * 是否为"纯知识类"问题：既不命中预约意图词，会话中也没有累积任何预约槽位。
     * 只有这类问题允许读写问答缓存——余量、档期、我的预约等实时数据一旦缓存
     * 就会向用户展示过期结果。
     */
    private boolean isKnowledgeOnlyQuery(String rewrittenQuery, ChatSession session) {
        return !isAppointmentQuery(rewrittenQuery)
                && (session == null || session.slotsOrEmpty().isEmpty());
    }

    /**
     * 查缓存并按正常问答的收尾方式产出一轮回答。先精确（L1/L2），再语义（0.95）。
     *
     * @return 缓存命中时的完整答案；未命中返回 null（调用方继续走检索 + LLM）
     */
    private String answerFromCache(String originalQuery, String rewrittenQuery, String sid, Long userId,
                                   Consumer<String> onToken,
                                   Consumer<List<Conversation.CitationRef>> onCitations,
                                   Consumer<Long> onMessageId,
                                   Consumer<AssistantEvent> onEvent,
                                   long startTime) {
        List<Conversation.CitationRef> citations = List.of();
        String answer = null;

        var exact = qaCacheService.getCachedAnswer(rewrittenQuery);
        if (exact.isPresent()) {
            answer = exact.get().answer();
            citations = exact.get().citations() == null ? List.of() : exact.get().citations();
        } else {
            String semantic = semanticCacheService.lookup(rewrittenQuery);
            if (semantic != null) {
                answer = semantic;
            }
        }
        if (answer == null || answer.isBlank()) {
            return null;
        }

        conversationRepository.save(sid, "user", originalQuery, userId);
        if (onToken != null) {
            onToken.accept(answer);
        }
        Long messageId = conversationRepository.saveWithReferences(
                sid, "assistant", answer, citations, userId);
        if (onMessageId != null && messageId != null) {
            onMessageId.accept(messageId);
        }
        if (onCitations != null) {
            onCitations.accept(citations);
        }
        // 知识类问题不会产生槽位/待确认事件，onEvent 无需回调

        metrics.recordCacheHit();
        metrics.recordQaRequest();
        metrics.recordQaLatency(System.currentTimeMillis() - startTime);
        incrementDailyCounter();
        return answer;
    }

    /**
     * 知识类回答写入两级缓存：精确缓存（Caffeine + Redis，含引用），
     * 语义缓存（向量，仅答案）。两者都受 kb.cache.enabled 开关控制。
     */
    private void populateKnowledgeCache(String rewrittenQuery, String answer,
                                        List<Conversation.CitationRef> citations) {
        try {
            qaCacheService.cacheAnswer(rewrittenQuery, answer, citations);
            semanticCacheService.store(rewrittenQuery, answer);
        } catch (Exception e) {
            // 缓存写入失败绝不影响主链路回答
            log.debug("问答缓存写入失败: {}", e.getMessage());
        }
    }

    // ==================== 待确认动作的执行 ====================

    private String executePending(String sid, Long userId, String query, ChatSession session,
                                  PendingBooking pending, boolean confirmed,
                                  Consumer<String> onToken,
                                  Consumer<List<Conversation.CitationRef>> onCitations,
                                  Consumer<Long> onMessageId,
                                  Consumer<AssistantEvent> onEvent,
                                  long startTime) {
        conversationRepository.save(sid, "user", query, userId);

        String answer;
        CasResult<CasBookingResult> result = null;
        if (!confirmed) {
            // 放弃：BOOK 动作需要通知 CAS 丢弃草稿，CANCEL 动作本来就还没执行。
            // CAS 不可用时 fallback 返回 503 结果（放弃草稿是尽力而为的清理动作，忽略结果）。
            if (PendingBooking.ACTION_BOOK.equals(pending.getAction()) && pending.getDraftId() != null) {
                casClient.discardBookingDraft(pending.getDraftId());
            }
            answer = "好的，已取消本次操作，没有产生任何变更。"
                    + "如果你想换时间、换校区或换其他服务，直接告诉我就行。";
        } else {
            result = PendingBooking.ACTION_CANCEL.equals(pending.getAction())
                    ? casClient.cancelBooking(pending.getOrderId())
                    : casClient.confirmBookingDraft(pending.getDraftId());
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

        Long messageId = conversationRepository.saveWithReferences(
                sid, "assistant", answer, List.of(), userId);
        if (onMessageId != null && messageId != null) {
            onMessageId.accept(messageId);
        }

        metrics.recordQaRequest();
        metrics.recordQaLatency(System.currentTimeMillis() - startTime);
        return answer;
    }

    private void discardPending(ChatSession session, PendingBooking pending) {
        if (PendingBooking.ACTION_BOOK.equals(pending.getAction()) && pending.getDraftId() != null) {
            // 尽力而为的清理：CAS 不可用时 fallbackFactory 内部已记录原因
            casClient.discardBookingDraft(pending.getDraftId());
        }
        session.setPendingBooking(null);
        chatSessionRepository.save(session);
    }

    // ==================== 上下文 ====================

    private List<LlmService.ChatMessage> loadHistory(String sessionId, Long userId) {
        List<Conversation> messages =
                conversationRepository.getRecentMessages(sessionId, HISTORY_LIMIT, userId);
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
                // 3.3.3："好的/可以 + 后续新请求小句"不判确认（形如"好的，怎么预约？"）；
                // 但"好的帮我约/确认预约吧"这类仍含明确下单动作的，保持判定为确认
                if (isAckFollowedByNewRequest(q)) {
                    return ConfirmIntent.NONE;
                }
                return ConfirmIntent.CONFIRM;
            }
        }
        return ConfirmIntent.NONE;
    }

    /**
     * 3.3.3：判断是否"应答词开头 + 另起的新请求"。
     * 条件：以纯应答词开头，其后既非空、也不是明确下单动作（约/订），
     * 且出现分句标点或新请求标志词。
     */
    private boolean isAckFollowedByNewRequest(String q) {
        for (String ack : ACK_PREFIXES) {
            if (!q.startsWith(ack)) {
                continue;
            }
            String rest = q.substring(ack.length()).trim();
            if (rest.isEmpty()) {
                return false;
            }
            // 明确下单动作仍按确认处理
            if (rest.contains("约") || rest.contains("订")) {
                return false;
            }
            boolean hasClauseBreak = rest.contains("，") || rest.contains(",")
                    || rest.contains("。") || rest.contains("?") || rest.contains("？")
                    || rest.contains("!") || rest.contains("！");
            boolean hasFollowUpWord = FOLLOW_UP_MARKERS.stream().anyMatch(rest::contains);
            return hasClauseBreak || hasFollowUpWord;
        }
        return false;
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

    // ========== Private Helpers ==========

    /**
     * 意图路由（Intent Routing）：判断用户问题是否可能涉及"实时预约数据"。
     * <p>命中关键词的问题会进入带工具集的 Function Calling 链路；在工具链路内，
     * 是否真正调用工具由 LLM 自主决定，本方法只做粗筛。
     * <p>
     * 3.3.4：路由词表收窄。此前混入了"老师/教师/咨询/设备/仓前/下沙"等可以单独成词的歧义词，
     * 像"教师招聘政策""设备处报修电话""仓前食堂在哪"这类纯知识库问题也被误路由进预约工具链路。
     * 现仅保留"单独出现也强烈指向预约动作/资源"的词；校区、人物身份等须与预约词共现，
     * 宁可不进也不要乱进。
     */
    private static final List<String> APPOINTMENT_KEYWORDS = List.of(
            "可预约", "预约", "余量", "名额", "会议室", "设备借用", "借用", "自习室",
            "场地", "空闲", "可约", "档期", "怎么预约", "怎么约", "能不能约",
            "有哪些服务", "还有哪些", "心理咨询", "教室", "取消预约", "我的预约");

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
