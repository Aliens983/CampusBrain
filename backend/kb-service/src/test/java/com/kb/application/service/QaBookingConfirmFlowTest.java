package com.kb.application.service;

import com.kb.domain.chat.AssistantEvent;
import com.kb.domain.chat.BookingSlots;
import com.kb.domain.chat.ChatSession;
import com.kb.domain.chat.ChatSessionRepository;
import com.kb.domain.chat.PendingBooking;
import com.kb.domain.conversation.Conversation;
import com.kb.domain.conversation.ConversationRepository;
import com.kb.domain.rag.LlmService;
import com.kb.domain.rag.RerankerService;
import com.kb.domain.rag.RetrievalResult;
import com.kb.domain.rag.SearchService;
import com.kb.infrastructure.client.CasClient;
import com.kb.infrastructure.client.CasResult;
import com.kb.infrastructure.client.dto.CasBookingResult;
import com.kb.infrastructure.metrics.BusinessMetrics;
import com.kb.infrastructure.rag.graph.GraphAssistedRetriever;
import com.kb.infrastructure.rag.rewrite.ContextualQueryRewriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 预约确认闭环测试
 * <p>
 * 验证"先确认、后下单"：
 * <ol>
 *   <li>用户答复"确认" → 真正调用 CAS 下单；</li>
 *   <li>用户答复"取消" → 丢弃草稿，绝不调下单接口；</li>
 *   <li>用户转移话题 → 丢弃旧草稿，避免后续误确认。</li>
 * </ol>
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("预约确认闭环")
class QaBookingConfirmFlowTest {

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

    @InjectMocks private QaApplicationService service;

    private ChatSession session;

    @BeforeEach
    void setUp() {
        session = ChatSession.create("s1", 1L);
        session.setPendingBooking(PendingBooking.builder()
                .action(PendingBooking.ACTION_BOOK)
                .draftId("draft-1")
                .resourceType(CasBookingDraftTypeStub.SERVICE)
                .summary("仓前校区 · 心理咨询 · 2026-09-12 09:00-10:00")
                .confirmPrompt("请确认是否提交以下预约")
                .needAudit(true)
                .build());
        when(chatSessionRepository.loadOrCreate(anyString(), any())).thenReturn(session);
    }

    @Test
    @DisplayName("用户确认 → 调用 CAS 下单并清空待确认状态")
    void shouldConfirmDraftWhenUserAgrees() {
        CasResult<CasBookingResult> ok = new CasResult<>();
        ok.setCode(200);
        ok.setData(CasBookingResult.builder().message("预约已提交，等待管理员审核").status("PENDING").build());
        when(casClient.confirmBookingDraft("draft-1")).thenReturn(ok);
        when(conversationRepository.saveWithReferences(anyString(), anyString(), anyString(), any()))
                .thenReturn(9L);

        List<AssistantEvent> events = new ArrayList<>();
        String answer = service.askStreaming("确认", "s1", t -> {}, c -> {}, id -> {}, events::add);

        verify(casClient).confirmBookingDraft("draft-1");
        verify(casClient, never()).discardBookingDraft("draft-1");
        assertTrue(answer.contains("预约已提交"));
        assertNull(session.getPendingBooking(), "确认后应清空待确认草稿");

        ArgumentCaptor<ChatSession> captor = ArgumentCaptor.forClass(ChatSession.class);
        verify(chatSessionRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        assertTrue(captor.getAllValues().stream().allMatch(s -> s.getPendingBooking() == null));
        assertTrue(events.stream().anyMatch(e -> AssistantEvent.TYPE_ACTION.equals(e.getType())));
    }

    @Test
    @DisplayName("用户取消 → 丢弃草稿，绝不调下单接口")
    void shouldDiscardDraftWhenUserRejects() {
        when(conversationRepository.saveWithReferences(anyString(), anyString(), anyString(), any()))
                .thenReturn(10L);

        String answer = service.askStreaming("算了，不要了", "s1", t -> {}, c -> {}, id -> {}, e -> {});

        verify(casClient).discardBookingDraft("draft-1");
        verify(casClient, never()).confirmBookingDraft(anyString());
        assertTrue(answer.contains("已取消"));
        assertNull(session.getPendingBooking());
    }

    @Test
    @DisplayName("用户转移话题 → 丢弃旧草稿并正常走问答链路")
    void shouldDiscardStaleDraftWhenTopicChanges() {
        when(contextualRewriter.rewrite(eq("知识库怎么用"), anyList(), any(BookingSlots.class)))
                .thenReturn(new ContextualQueryRewriter.RewriteResult("知识库怎么用", new BookingSlots(), false));
        when(graphRetriever.retrieve("知识库怎么用")).thenReturn(List.<RetrievalResult>of());
        when(rerankerService.rerank("知识库怎么用", List.of())).thenReturn(List.<RetrievalResult>of());
        when(llmService.generateAnswerDirectStreaming(anyString(), anyList(), any())).thenReturn("这是知识库用法");
        when(conversationRepository.saveWithReferences(anyString(), anyString(), anyString(), any()))
                .thenReturn(11L);

        service.askStreaming("知识库怎么用", "s1", t -> {}, c -> {}, id -> {}, e -> {});

        verify(casClient).discardBookingDraft("draft-1");
        verify(casClient, never()).confirmBookingDraft(anyString());
        assertNull(session.getPendingBooking(), "转移话题后不应残留待确认草稿");
    }

    @Test
    @DisplayName("没有待确认草稿时，'确认' 只是普通提问，不会误下单")
    void shouldNotBookWhenNoPendingDraft() {
        session.setPendingBooking(null);
        when(contextualRewriter.rewrite(eq("确认"), anyList(), any(BookingSlots.class)))
                .thenReturn(new ContextualQueryRewriter.RewriteResult("确认", new BookingSlots(), false));
        when(graphRetriever.retrieve("确认")).thenReturn(List.<RetrievalResult>of());
        when(rerankerService.rerank("确认", List.of())).thenReturn(List.<RetrievalResult>of());
        when(llmService.generateAnswerDirectStreaming(anyString(), anyList(), any())).thenReturn("请问要确认什么？");
        when(conversationRepository.saveWithReferences(anyString(), anyString(), anyString(), any()))
                .thenReturn(12L);

        service.askStreaming("确认", "s1", t -> {}, c -> {}, id -> {}, e -> {});

        verify(casClient, never()).confirmBookingDraft(anyString());
        verify(casClient, never()).discardBookingDraft(anyString());
    }

    /** 复用 CAS 侧草稿类型常量，避免测试里硬编码字符串 */
    private static final class CasBookingDraftTypeStub {
        private static final String SERVICE = "SERVICE";
    }
}
