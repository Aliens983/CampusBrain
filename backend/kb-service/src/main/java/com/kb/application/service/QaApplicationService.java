package com.kb.application.service;

import com.kb.domain.conversation.Conversation;
import com.kb.domain.conversation.ConversationRepository;
import com.kb.domain.rag.*;
import com.kb.infrastructure.cache.QaCacheService;
import com.kb.infrastructure.cache.SemanticCacheService;
import com.kb.infrastructure.metrics.BusinessMetrics;
import com.kb.infrastructure.rag.graph.GraphAssistedRetriever;
import com.kb.infrastructure.rag.rewrite.QueryRewriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Application service for the Q&A (RAG) workflow orchestration.
 * <p>
 * Full online pipeline:
 * <ol>
 *   <li>Hybrid Retrieval (ES keyword + Qdrant vector → RRF fusion)</li>
 *   <li>Re-ranking (optional, CrossEncoder refinement)</li>
 *   <li>Context Assembly (retrieved chunks + conversation history)</li>
 *   <li>LLM Generation (streaming with citation annotations)</li>
 *   <li>Conversation Persistence</li>
 * </ol>
 * </p>
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
    private final QaCacheService qaCacheService;
    private final SemanticCacheService semanticCacheService;
    private final QueryRewriter queryRewriter;
    private final GraphAssistedRetriever graphRetriever;
    private final BusinessMetrics metrics;
    private final StringRedisTemplate redisTemplate;

    /**
     * Execute a Q&A request with streaming response.
     *
     * @param query       user's question
     * @param sessionId   conversation session ID (auto-generated if empty)
     * @param onToken     callback for each generated token (SSE push)
     * @param onCitations callback with citation list after generation completes
     * @return the complete answer text
     */
    public String askStreaming(String query, String sessionId,
                                Consumer<String> onToken,
                                Consumer<List<Conversation.CitationRef>> onCitations,
                                Consumer<Long> onMessageId) {
        long startTime = System.currentTimeMillis();
        String sid = ensureSessionId(sessionId);

        // Step 0: 缓存已全部禁用，每次都实时检索 + LLM 回答，保证最新最准
        // var exactCached = qaCacheService.getCachedAnswer(query);
        var exactCached = java.util.Optional.<com.kb.infrastructure.cache.QaCacheService.QaCacheEntry>empty();
        if (exactCached.isPresent()) {
            log.debug("Exact cache hit for query: {}", query);
            metrics.recordCacheHit();
            onToken.accept(exactCached.get().answer());
            onCitations.accept(exactCached.get().citations());
            metrics.recordQaLatency(System.currentTimeMillis() - startTime);
            return exactCached.get().answer();
        }

        // 语义缓存（向量相似度匹配）容易误命中导致答非所问，已禁用
        String semanticCached = null; // semanticCacheService.lookup(query);
        if (semanticCached != null) {
            log.debug("Semantic cache hit for query: {}", query);
            metrics.recordCacheHit();
            onToken.accept(semanticCached);
            onCitations.accept(List.of());
            metrics.recordQaLatency(System.currentTimeMillis() - startTime);
            return semanticCached;
        }

        metrics.recordCacheMiss();

        try {
            // Step 1: Load conversation history for context
            List<Conversation> history = conversationRepository.getRecentMessages(sid, 10);
            List<LlmService.ChatMessage> chatHistory = history.stream()
                    .map(c -> new LlmService.ChatMessage(c.getRole(), c.getContent()))
                    .toList();

            // Step 2: Query rewrite (coreference resolution)
            String rewrittenQuery = queryRewriter.rewrite(query, chatHistory);
            if (!rewrittenQuery.equals(query)) {
                log.debug("Query rewritten: [{}] -> [{}]", query, rewrittenQuery);
            }

            // Step 3: Hybrid retrieval
            long retrievalStart = System.currentTimeMillis();
            List<RetrievalResult> retrieved = graphRetriever.retrieve(rewrittenQuery);
            metrics.recordRetrievalLatency(System.currentTimeMillis() - retrievalStart);
            log.debug("Retrieved {} chunks for query: {}", retrieved.size(),
                    rewrittenQuery.substring(0, Math.min(50, rewrittenQuery.length())));

            // Step 4: Re-ranking
            List<RetrievalResult> reranked = rerankerService.rerank(rewrittenQuery, retrieved);

            // Step 5: Save user message
            conversationRepository.save(sid, "user", query);

            // Step 6: LLM generation
            // 意图路由：预约类 → Function Calling；非预约类 → 本地资料有结果走 RAG，无结果或 RAG 无法回答 → DeepSeek 兜底
            String fullAnswer;
            if (isAppointmentQuery(rewrittenQuery)) {
                fullAnswer = llmService.generateAnswerWithTools(
                        rewrittenQuery, reranked, chatHistory, onToken);
            } else if (reranked != null && !reranked.isEmpty()) {
                // 先同步生成 RAG 回答判断是否能回答，避免"无法回答"推流后又推兜底导致拼接
                String ragAnswer = llmService.generateAnswer(rewrittenQuery, reranked, chatHistory);
                if (looksLikeNoAnswer(ragAnswer)) {
                    fullAnswer = llmService.generateAnswerDirect(rewrittenQuery, chatHistory);
                } else {
                    fullAnswer = ragAnswer;
                }
                if (onToken != null) {
                    onToken.accept(fullAnswer);
                }
            } else {
                fullAnswer = llmService.generateAnswerDirectStreaming(
                        rewrittenQuery, chatHistory, onToken);
            }

            // Step 7: Build citations
            List<Conversation.CitationRef> citations = buildCitations(reranked);

            // Step 8: Save assistant message with citations，回传 messageId 供前端反馈
            Long messageId = conversationRepository.saveWithReferences(
                    sid, "assistant", fullAnswer, citations);
            if (onMessageId != null && messageId != null) {
                onMessageId.accept(messageId);
            }

            onCitations.accept(citations);

            // Step 9: 缓存已全部禁用，不写入
            // qaCacheService.cacheAnswer(query, fullAnswer, citations);
            // semanticCacheService.store(query, fullAnswer);

            // Step 10: Record metrics
            metrics.recordQaRequest();
            metrics.recordQaLatency(System.currentTimeMillis() - startTime);
            incrementDailyCounter();

            return fullAnswer;

        } catch (Exception e) {
            log.error("Q&A failed for query: {}", query, e);
            String errorAnswer = "抱歉，处理您的问题时遇到了错误：" + e.getMessage();
            conversationRepository.save(sid, "assistant", errorAnswer);
            onToken.accept(errorAnswer);
            metrics.recordQaLatency(System.currentTimeMillis() - startTime);
            return errorAnswer;
        }
    }

    /**
     * Execute a Q&A request synchronously (non-streaming).
     */
    public String ask(String query, String sessionId) {
        long startTime = System.currentTimeMillis();
        String sid = ensureSessionId(sessionId);

        // 缓存已全部禁用，每次都实时检索 + LLM 回答
        // var exactCached = qaCacheService.getCachedAnswer(query);
        var exactCached = java.util.Optional.<com.kb.infrastructure.cache.QaCacheService.QaCacheEntry>empty();
        if (exactCached.isPresent()) {
            metrics.recordCacheHit();
            return exactCached.get().answer();
        }
        // 语义缓存（向量相似度匹配）容易误命中导致答非所问，已禁用
        String semanticCached = null; // semanticCacheService.lookup(query);
        if (semanticCached != null) {
            metrics.recordCacheHit();
            return semanticCached;
        }
        metrics.recordCacheMiss();

        // Load history and rewrite query
        List<Conversation> history = conversationRepository.getRecentMessages(sid, 10);
        List<LlmService.ChatMessage> chatHistory = history.stream()
                .map(c -> new LlmService.ChatMessage(c.getRole(), c.getContent()))
                .toList();
        String rewrittenQuery = queryRewriter.rewrite(query, chatHistory);

        // Retrieve + rerank
        long retrievalStart = System.currentTimeMillis();
        List<RetrievalResult> retrieved = searchService.search(rewrittenQuery);
        metrics.recordRetrievalLatency(System.currentTimeMillis() - retrievalStart);
        List<RetrievalResult> reranked = rerankerService.rerank(rewrittenQuery, retrieved);

        conversationRepository.save(sid, "user", query);

        String answer;
        if (isAppointmentQuery(rewrittenQuery)) {
            answer = llmService.generateAnswerWithTools(
                    rewrittenQuery, reranked, chatHistory, null);
        } else if (reranked != null && !reranked.isEmpty()) {
            answer = llmService.generateAnswer(rewrittenQuery, reranked, chatHistory);
            // 本地资料检索到了但 LLM 认为无法回答 → 用 DeepSeek 兜底重新回答
            if (looksLikeNoAnswer(answer)) {
                answer = llmService.generateAnswerDirect(rewrittenQuery, chatHistory);
            }
        } else {
            // 本地无相关资料 → DeepSeek 大模型兜底
            answer = llmService.generateAnswerDirect(rewrittenQuery, chatHistory);
        }

        List<Conversation.CitationRef> citations = buildCitations(reranked);
        conversationRepository.saveWithReferences(sid, "assistant", answer, citations);

        // Cache 已全部禁用，不写入（metrics 保留）
        // qaCacheService.cacheAnswer(query, answer, citations);
        // semanticCacheService.store(query, answer);
        metrics.recordQaRequest();
        metrics.recordQaLatency(System.currentTimeMillis() - startTime);
        incrementDailyCounter();

        return answer;
    }

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

    // ========== Private Helpers ==========

    /**
     * 意图路由（Intent Routing）：判断用户问题是否可能涉及"实时预约数据"
     *
     * <p>命中关键词的问题会进入带 {@code AppointmentTool} 的 Function Calling 链路；
     * 在工具链路内，<b>是否真正调用工具由 LLM 自主决定</b>（LangChain4j AiServices
     * 会把工具描述交给模型，模型按需触发）。因此这是"意图路由 + LLM 自主调用"两层设计，
     * 而非应用层强制调用工具</p>
     *
     * <p>关键词覆盖：可预约 / 余量 / 会议室 / 设备借用 / 咨询 / 自习室 / 场地等预约场景</p>
     */
    private static final List<String> APPOINTMENT_KEYWORDS = List.of(
            "可预约", "预约", "余量", "会议室", "设备", "咨询", "自习室", "场地", "借用",
            "有哪些服务", "还有哪些", "能不能约", "怎么约", "怎么预约", "空闲", "名额");

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
