package com.kb.application.service;

import com.kb.domain.chat.BookingSlots;
import com.kb.domain.chat.ChatSession;
import com.kb.domain.conversation.ConversationRepository;
import com.kb.domain.rag.CancellationToken;
import com.kb.domain.rag.LlmService;
import com.kb.domain.rag.RerankerService;
import com.kb.domain.rag.RetrievalResult;
import com.kb.domain.rag.SearchService;
import com.kb.infrastructure.metrics.BusinessMetrics;
import com.kb.infrastructure.rag.graph.GraphAssistedRetriever;
import com.kb.infrastructure.rag.intent.KeywordIntentClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 5.5（深度审查 P1）AnswerPipeline 同步/流式分支路由测试。
 * <p>
 * 重点守护 3.2（P0）与 3.3（P0）的修后行为不回归：
 * <ul>
 *   <li>同步链路<b>无召回</b>必须走 {@code generateAnswerDirect}（旧实现传 null 消费者走流式 → NPE）；</li>
 *   <li>同步链路召回命中"无法回答"标记词 → 切换 {@code generateAnswerDirect} 兜底；</li>
 *   <li>流式链路无召回 → 走 4 参 {@code generateAnswerDirectStreaming}（含 CancellationToken），
 *       绝不可退化成同步直答（会吞掉逐 token 推送语义）。</li>
 * </ul>
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnswerPipeline 同步/流式分支路由（3.2 回归守护）")
class AnswerPipelineRoutingTest {

    @Mock private SearchService searchService;
    @Mock private RerankerService rerankerService;
    @Mock private LlmService llmService;
    @Mock private GraphAssistedRetriever graphRetriever;
    @Mock private ConversationRepository conversationRepository;
    @Mock private BusinessMetrics metrics;

    private AnswerPipeline pipeline;

    @BeforeEach
    void setUp() {
        pipeline = new AnswerPipeline(searchService, rerankerService, llmService,
                graphRetriever, conversationRepository, metrics, new KeywordIntentClassifier());
    }

    private ChatSession emptySession() {
        return ChatSession.create("s1", 1L);
    }

    @Test
    @DisplayName("同步 + 无召回：走 generateAnswerDirect，绝不调流式")
    void syncNoRecallGoesDirect() {
        when(llmService.generateAnswerDirect(eq("科普问题"), anyList())).thenReturn("直接兜底答案");

        String answer = pipeline.generate("科普问题", List.of(), List.of(),
                emptySession(), null, CancellationToken.none());

        assertEquals("直接兜底答案", answer);
        // 3.2 回归守护：同步无召回绝不能走流式（旧实现传 null 消费者 → NPE）
        verify(llmService).generateAnswerDirect("科普问题", List.of());
        verify(llmService, never()).generateAnswerStreaming(anyString(), anyList(), anyList(), any(), any());
    }

    @Test
    @DisplayName("同步 + 召回命中无法回答标记：自动切换 generateAnswerDirect 兜底")
    void syncRecallHitsNoAnswerMarkerFallsBackToDirect() {
        RetrievalResult doc = RetrievalResult.builder().chunkId("c1").content("知识库内容").build();
        when(llmService.generateAnswer("科普问题", List.of(doc), List.of()))
                .thenReturn("抱歉，我没有找到相关的本地资料。");
        when(llmService.generateAnswerDirect("科普问题", List.of())).thenReturn("直答兜底");

        String answer = pipeline.generate("科普问题", List.of(doc), List.of(),
                emptySession(), null, CancellationToken.none());

        assertEquals("直答兜底", answer);
        verify(llmService).generateAnswerDirect("科普问题", List.of());
    }

    @Test
    @DisplayName("同步 + 正常召回：只走 generateAnswer，不触发兜底")
    void syncRecallNormalUsesRagOnly() {
        RetrievalResult doc = RetrievalResult.builder().chunkId("c1").content("知识库内容").build();
        when(llmService.generateAnswer("科普问题", List.of(doc), List.of())).thenReturn("RAG 回答");

        String answer = pipeline.generate("科普问题", List.of(doc), List.of(),
                emptySession(), null, CancellationToken.none());

        assertEquals("RAG 回答", answer);
        verify(llmService, never()).generateAnswerDirect(anyString(), anyList());
    }

    @Test
    @DisplayName("同步 + 预约相关：走 generateAnswerWithTools")
    void syncAppointmentUsesTools() {
        when(llmService.generateAnswerWithTools(anyString(), anyList(), anyList(), any(), any()))
                .thenReturn("工具回答");

        ChatSession session = emptySession();
        session.setSlots(BookingSlots.builder().campus("cq").build());
        String answer = pipeline.generate("还有哪些服务可预约", List.of(), List.of(),
                session, null, CancellationToken.none());

        assertEquals("工具回答", answer);
        verify(llmService, never()).generateAnswerDirect(anyString(), anyList());
    }

    @Test
    @DisplayName("流式 + 无召回：走 4 参 generateAnswerDirectStreaming，token 回调贯通")
    void streamingNoRecallGoesDirectStreaming() {
        when(llmService.generateAnswerDirectStreaming(eq("科普问题"), eq(List.of()), any(), any()))
                .thenReturn("流式直答");

        AtomicInteger tokens = new AtomicInteger();
        String answer = pipeline.generate("科普问题", List.of(), List.of(),
                emptySession(), t -> tokens.incrementAndGet(), CancellationToken.none());

        assertEquals("流式直答", answer);
        // 5.3：必须是 4 参可取消版本（带 CancellationToken），3 参打桩在此不再匹配
        verify(llmService).generateAnswerDirectStreaming(eq("科普问题"), eq(List.of()), any(), any());
        // 同步直答绝不能在流式链路中被调用（会丢逐 token 推送）
        verify(llmService, never()).generateAnswerDirect(anyString(), anyList());
    }
}