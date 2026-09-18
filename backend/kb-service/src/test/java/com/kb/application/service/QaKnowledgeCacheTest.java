package com.kb.application.service;

import com.kb.domain.chat.BookingSlots;
import com.kb.domain.chat.ChatSession;
import com.kb.domain.chat.ChatSessionRepository;
import com.kb.domain.conversation.Conversation;
import com.kb.domain.conversation.ConversationRepository;
import com.kb.domain.rag.LlmService;
import com.kb.domain.rag.RerankerService;
import com.kb.domain.rag.RetrievalResult;
import com.kb.domain.rag.SearchService;
import com.kb.infrastructure.cache.QaCacheService;
import com.kb.infrastructure.cache.SemanticCacheService;
import com.kb.infrastructure.client.CasClient;
import com.kb.infrastructure.metrics.BusinessMetrics;
import com.kb.infrastructure.rag.graph.GraphAssistedRetriever;
import com.kb.infrastructure.rag.rewrite.ContextualQueryRewriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 问答缓存读写策略测试。
 * <p>
 * 规则：只有纯知识类问题（不命中预约意图、会话无预约槽位）才查写缓存；
 * 预约实时类问题必须完全绕开缓存，否则用户会看到过期余量。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("知识问答缓存策略测试")
class QaKnowledgeCacheTest {

    @Mock private SearchService searchService;
    @Mock private RerankerService rerankerService;
    @Mock private LlmService llmService;
    @Mock private ConversationRepository conversationRepository;
    @Mock private ContextualQueryRewriter contextualRewriter;
    @Mock private GraphAssistedRetriever graphRetriever;
    @Mock private BusinessMetrics metrics;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ChatSessionRepository chatSessionRepository;
    @Mock private CasClient casClient;
    @Mock private QaCacheService qaCacheService;
    @Mock private SemanticCacheService semanticCacheService;

    private QaApplicationService service;

    @BeforeEach
    void setUp() {
        AnswerPipeline pipeline = new AnswerPipeline(searchService, rerankerService, llmService,
                graphRetriever, conversationRepository, metrics);
        CacheGuard cacheGuard = new CacheGuard(pipeline, qaCacheService, semanticCacheService,
                conversationRepository, metrics);
        PendingBookingExecutor pendingExecutor = new PendingBookingExecutor(casClient,
                chatSessionRepository, conversationRepository, metrics);
        service = new QaApplicationService(contextualRewriter, conversationRepository,
                chatSessionRepository, metrics, redisTemplate, pipeline, cacheGuard, pendingExecutor);
    }

    private static final String QUERY = "什么是向量检索？";

    private void stubEmptySession() {
        when(chatSessionRepository.loadForUser(anyString(), any()))
                .thenAnswer(inv -> ChatSession.create(inv.getArgument(0), 1L));
    }

    private void stubRewrite(String query) {
        when(contextualRewriter.rewrite(eq(query), anyList(), any(BookingSlots.class)))
                .thenReturn(new ContextualQueryRewriter.RewriteResult(query, new BookingSlots(), false));
    }

    @Test
    @DisplayName("精确缓存命中：直接返回缓存答案，不再检索/调用 LLM，并推送答案与落库")
    void exactCacheHit_skipsRetrievalAndLlm() {
        stubEmptySession();
        stubRewrite(QUERY);
        List<Conversation.CitationRef> citations = List.of();
        when(qaCacheService.getCachedAnswer(QUERY))
                .thenReturn(Optional.of(new QaCacheService.QaCacheEntry("缓存的答案", citations, 1L)));
        when(conversationRepository.saveWithReferences(
                anyString(), anyString(), eq("缓存的答案"), any(), any())).thenReturn(99L);

        List<String> tokens = new ArrayList<>();
        long[] messageId = {0};
        String answer = service.askStreaming(QUERY, "s1", tokens::add, c -> {}, id -> messageId[0] = id);

        assertThat(answer).isEqualTo("缓存的答案");
        assertThat(tokens).containsExactly("缓存的答案");
        assertThat(messageId[0]).isEqualTo(99L);
        verify(graphRetriever, never()).retrieve(anyString());
        verify(rerankerService, never()).rerank(anyString(), anyList());
        verify(semanticCacheService, never()).lookup(anyString());
        verify(llmService, never()).generateAnswer(anyString(), anyList(), anyList());
        verify(metrics).recordCacheHit();
        verify(metrics, never()).recordCacheMiss();
    }

    @Test
    @DisplayName("精确未命中、语义命中：返回语义缓存答案且跳过检索")
    void semanticCacheHit_skipsRetrieval() {
        stubEmptySession();
        stubRewrite(QUERY);
        when(qaCacheService.getCachedAnswer(QUERY)).thenReturn(Optional.empty());
        when(semanticCacheService.lookup(QUERY))
                .thenReturn(new SemanticCacheService.SemanticCacheHit("语义相似问题的答案", List.of()));
        when(conversationRepository.saveWithReferences(
                anyString(), anyString(), anyString(), any(), any())).thenReturn(1L);

        String answer = service.askStreaming(QUERY, "s1", t -> {}, c -> {}, id -> {});

        assertThat(answer).isEqualTo("语义相似问题的答案");
        verify(graphRetriever, never()).retrieve(anyString());
        verify(metrics).recordCacheHit();
    }

    @Test
    @DisplayName("两级都未命中：正常走 RAG，知识类回答写回两级缓存")
    void cacheMiss_runsRagAndPopulatesCache() {
        stubEmptySession();
        stubRewrite(QUERY);
        when(qaCacheService.getCachedAnswer(QUERY)).thenReturn(Optional.empty());
        when(semanticCacheService.lookup(QUERY)).thenReturn(null);
        RetrievalResult doc = RetrievalResult.builder()
                .chunkId("c1").documentId("d1").documentTitle("向量检索")
                .content("...").chunkIndex(0).score(0.9).source("keyword").build();
        when(graphRetriever.retrieve(QUERY)).thenReturn(List.of(doc));
        when(rerankerService.rerank(QUERY, List.of(doc))).thenReturn(List.of(doc));
        // SSE 链路走真流式 RAG：模拟供应商逐 token 回调并返回完整答案
        when(llmService.generateAnswerStreaming(
                anyString(), anyList(), anyList(), any(), any())).thenAnswer(inv -> {
            java.util.function.Consumer<String> tokenConsumer = inv.getArgument(3);
            tokenConsumer.accept("新鲜 RAG 答案");
            return "新鲜 RAG 答案";
        });
        when(conversationRepository.saveWithReferences(
                anyString(), anyString(), anyString(), any(), any())).thenReturn(1L);

        String answer = service.askStreaming(QUERY, "s1", t -> {}, c -> {}, id -> {});

        assertThat(answer).isEqualTo("新鲜 RAG 答案");
        verify(qaCacheService).cacheAnswer(eq(QUERY), eq("新鲜 RAG 答案"), anyList());
        verify(semanticCacheService).store(eq(QUERY), eq("新鲜 RAG 答案"), anyList());
        verify(metrics).recordCacheMiss();
    }

    @Test
    @DisplayName("预约意图问题：不读不写缓存（即使本地无召回走兜底）")
    void appointmentQuery_neverTouchesCache() {
        String query = "自习室还有名额吗";
        stubEmptySession();
        stubRewrite(query);
        when(graphRetriever.retrieve(query)).thenReturn(List.of());
        when(rerankerService.rerank(query, List.of())).thenReturn(List.of());
        when(llmService.generateAnswerWithToolsStreaming(
                anyString(), anyList(), anyList(), any(), any(), any())).thenAnswer(inv -> {
            java.util.function.Consumer<String> tokenConsumer = inv.getArgument(3);
            tokenConsumer.accept("实时答案");
            return "实时答案";
        });
        when(conversationRepository.saveWithReferences(
                anyString(), anyString(), anyString(), any(), any())).thenReturn(1L);

        service.askStreaming(query, "s1", t -> {}, c -> {}, id -> {});

        verify(qaCacheService, never()).getCachedAnswer(anyString());
        verify(semanticCacheService, never()).lookup(anyString());
        verify(qaCacheService, never()).cacheAnswer(anyString(), anyString(), anyList());
        verify(semanticCacheService, never()).store(anyString(), anyString(), anyList());
    }

    @Test
    @DisplayName("会话已带预约槽位（追问换话题）：不读不写缓存")
    void sessionWithSlots_neverTouchesCache() {
        ChatSession session = ChatSession.create("s1", 1L);
        BookingSlots slots = BookingSlots.builder().campus("cq").build();
        session.setSlots(slots);
        when(chatSessionRepository.loadForUser(anyString(), any())).thenReturn(session);
        // 改写结果继承已累积的校区槽位（真实 rewriter 会合并会话槽位）
        when(contextualRewriter.rewrite(eq(QUERY), anyList(), any(BookingSlots.class)))
                .thenReturn(new ContextualQueryRewriter.RewriteResult(QUERY, slots, false));
        when(graphRetriever.retrieve(QUERY)).thenReturn(List.of());
        when(rerankerService.rerank(QUERY, List.of())).thenReturn(List.of());
        when(llmService.generateAnswerWithToolsStreaming(
                anyString(), anyList(), anyList(), any(), any(), any())).thenAnswer(inv -> {
            java.util.function.Consumer<String> tokenConsumer = inv.getArgument(3);
            tokenConsumer.accept("工具答案");
            return "工具答案";
        });
        when(conversationRepository.saveWithReferences(
                anyString(), anyString(), anyString(), any(), any())).thenReturn(1L);

        service.askStreaming(QUERY, "s1", t -> {}, c -> {}, id -> {});

        verify(qaCacheService, never()).getCachedAnswer(anyString());
        verify(semanticCacheService, never()).lookup(anyString());
    }
}
