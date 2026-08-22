package com.kb.application.service;

import com.kb.domain.conversation.Conversation;
import com.kb.domain.conversation.ConversationRepository;
import com.kb.domain.rag.LlmService;
import com.kb.domain.rag.RerankerService;
import com.kb.domain.rag.RetrievalResult;
import com.kb.domain.rag.SearchService;
import com.kb.infrastructure.cache.QaCacheService;
import com.kb.infrastructure.cache.SemanticCacheService;
import com.kb.infrastructure.metrics.BusinessMetrics;
import com.kb.infrastructure.rag.graph.GraphAssistedRetriever;
import com.kb.infrastructure.rag.rewrite.QueryRewriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Function Calling 意图路由测试。
 * <p>
 * 验证：
 * 1. 预约类问题 → 走 {@code generateAnswerWithTools}（Function Calling 链路）；
 * 2. 非预约类问题 → 走 {@code generateAnswerStreaming}（纯 RAG 流式），不触发工具；
 * 3. {@code isAppointmentQuery} 关键词边界。
 * </p>
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QaApplicationService 工具意图路由测试")
class QaAppServiceToolRoutingTest {

    @Mock private SearchService searchService;
    @Mock private RerankerService rerankerService;
    @Mock private LlmService llmService;
    @Mock private ConversationRepository conversationRepository;
    @Mock private QaCacheService qaCacheService;
    @Mock private SemanticCacheService semanticCacheService;
    @Mock private QueryRewriter queryRewriter;
    @Mock private GraphAssistedRetriever graphRetriever;
    @Mock private BusinessMetrics metrics;
    @Mock private StringRedisTemplate redisTemplate;

    @InjectMocks private QaApplicationService service;

    private void stubPipeline(String query) {
        when(qaCacheService.getCachedAnswer(query)).thenReturn(Optional.empty());
        when(semanticCacheService.lookup(query)).thenReturn(null);
        when(conversationRepository.getRecentMessages(anyString(), anyInt())).thenReturn(List.of());
        when(queryRewriter.rewrite(query, List.of())).thenReturn(query);
        when(graphRetriever.retrieve(query)).thenReturn(List.<RetrievalResult>of());
        when(rerankerService.rerank(query, List.of())).thenReturn(List.<RetrievalResult>of());
        when(conversationRepository.saveWithReferences(anyString(), anyString(), anyString(), any()))
                .thenReturn(1L);
    }

    @Nested
    @DisplayName("意图路由分流")
    class Routing {

        @Test
        @DisplayName("预约类问题应走 Function Calling 工具链路")
        void shouldRouteAppointmentQueryToTools() {
            String query = "有哪些服务可以预约？";
            stubPipeline(query);
            when(llmService.generateAnswerWithTools(anyString(), anyList(), anyList(), any()))
                    .thenReturn("实时答案");

            service.askStreaming(query, "s1", t -> {}, c -> {}, id -> {});

            verify(llmService).generateAnswerWithTools(anyString(), anyList(), anyList(), any());
            verify(llmService, never()).generateAnswerStreaming(anyString(), anyList(), anyList(), any());
        }

        @Test
        @DisplayName("非预约类问题不应走工具链路")
        void shouldNotRouteNonAppointmentQueryToTools() {
            String query = "什么是 RAG 检索？";
            stubPipeline(query);
            when(llmService.generateAnswerStreaming(anyString(), anyList(), anyList(), any()))
                    .thenReturn("RAG 答案");

            service.askStreaming(query, "s1", t -> {}, c -> {}, id -> {});

            verify(llmService, never()).generateAnswerWithTools(anyString(), anyList(), anyList(), any());
            verify(llmService).generateAnswerStreaming(anyString(), anyList(), anyList(), any());
        }

        @Test
        @DisplayName("工具链路应回传 messageId")
        void shouldPassMessageIdOnToolPath() {
            String query = "会议室还有多少余量？";
            stubPipeline(query);
            when(llmService.generateAnswerWithTools(anyString(), anyList(), anyList(), any()))
                    .thenReturn("实时答案");

            long[] captured = {0L};
            service.askStreaming(query, "s1", t -> {}, c -> {}, id -> captured[0] = id);

            assertTrue(captured[0] == 1L);
        }
    }

    @Nested
    @DisplayName("isAppointmentQuery 关键词边界")
    class KeywordBoundary {

        @Test
        @DisplayName("预约场景关键词应命中")
        void shouldHitForAppointmentKeywords() {
            assertTrue(invokeIsAppointment("有哪些服务可预约"));
            assertTrue(invokeIsAppointment("会议室还能约吗"));
            assertTrue(invokeIsAppointment("怎么借用设备"));
            assertTrue(invokeIsAppointment("预约心理咨询"));
            assertTrue(invokeIsAppointment("自习室还有名额吗"));
        }

        @Test
        @DisplayName("普通知识问答不应命中")
        void shouldNotHitForGeneralQueries() {
            assertFalse(invokeIsAppointment("什么是向量检索"));
            assertFalse(invokeIsAppointment("介绍一下知识库功能"));
            assertFalse(invokeIsAppointment(null));
            assertFalse(invokeIsAppointment(""));
        }
    }

    private boolean invokeIsAppointment(String q) {
        return ReflectionTestUtils.invokeMethod(service, "isAppointmentQuery", (Object) q);
    }
}
