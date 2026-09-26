package com.kb.application.service;

import com.kb.domain.chat.ChatSessionRepository;
import com.kb.domain.conversation.ConversationRepository;
import com.kb.domain.conversation.ConversationSummary;
import com.kb.domain.rag.LlmService;
import com.kb.domain.rag.RerankerService;
import com.kb.domain.rag.SearchService;
import com.kb.infrastructure.cache.QaCacheService;
import com.kb.infrastructure.cache.SemanticCacheService;
import com.kb.infrastructure.client.CasClient;
import com.kb.infrastructure.metrics.BusinessMetrics;
import com.kb.infrastructure.rag.graph.GraphAssistedRetriever;
import com.kb.infrastructure.rag.intent.KeywordIntentClassifier;
import com.kb.infrastructure.rag.rewrite.ContextualQueryRewriter;
import com.kb.infrastructure.security.SecurityFrameworkUtils;
import com.laoliu.auth.dto.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * 历史会话列表查询测试：验证以当前登录用户身份从仓储取会话摘要，
 * 顺序由 SQL 保证（最近活跃倒序），服务层只做身份透传。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("历史会话列表查询测试")
class QaConversationListTest {

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

    private static final Long USER_ID = 7L;

    @BeforeEach
    void setUp() {
        AnswerPipeline pipeline = new AnswerPipeline(searchService, rerankerService, llmService,
                graphRetriever, conversationRepository, metrics, new KeywordIntentClassifier());
        CacheGuard cacheGuard = new CacheGuard(new KeywordIntentClassifier(), qaCacheService,
                semanticCacheService, conversationRepository, metrics);
        PendingBookingExecutor pendingExecutor = new PendingBookingExecutor(casClient,
                chatSessionRepository, conversationRepository, metrics);
        service = new QaApplicationService(contextualRewriter, conversationRepository,
                chatSessionRepository, metrics, redisTemplate, pipeline, cacheGuard, pendingExecutor);

        LoginUser loginUser = new LoginUser();
        loginUser.setId(USER_ID);
        SecurityFrameworkUtils.setLoginUser(loginUser);
    }

    @AfterEach
    void clearUser() {
        SecurityFrameworkUtils.clearContext();
    }

    @Test
    @DisplayName("返回当前登录用户的会话摘要并保持仓储顺序")
    void listConversationsReturnsCurrentUserSessions() {
        LocalDateTime now = LocalDateTime.now();
        when(conversationRepository.findSessionsByUser(USER_ID, 50)).thenReturn(List.of(
                new ConversationSummary("s-recent", "最近的问题", now),
                new ConversationSummary("s-old", "早些时候的问题", now.minusDays(1))
        ));

        List<ConversationSummary> result = service.listConversations();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).sessionId()).isEqualTo("s-recent");
        assertThat(result.get(0).title()).isEqualTo("最近的问题");
        assertThat(result.get(1).sessionId()).isEqualTo("s-old");
    }

    @Test
    @DisplayName("无历史会话时返回空列表（清库/新用户场景）")
    void listConversationsEmptyWhenNoHistory() {
        when(conversationRepository.findSessionsByUser(USER_ID, 50)).thenReturn(List.of());

        assertThat(service.listConversations()).isEmpty();
    }
}
