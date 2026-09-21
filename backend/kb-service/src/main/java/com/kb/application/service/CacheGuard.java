package com.kb.application.service;

import com.kb.domain.chat.ChatSession;
import com.kb.domain.conversation.Conversation;
import com.kb.domain.conversation.ConversationRepository;
import com.kb.domain.rag.IntentClassifier;
import com.kb.infrastructure.cache.QaCacheService;
import com.kb.infrastructure.cache.SemanticCacheService;
import com.kb.infrastructure.metrics.BusinessMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * 缓存守卫协作类（Q-01 从 QaApplicationService 拆出）。
 * <p>
 * 职责：判定"纯知识类"问题（可读写问答缓存）并完成命中返回 / 写回两条路径。
 * 预约实时类问题（余量、档期、我的预约等）一旦缓存就会向用户展示过期结果，
 * 因此必须完全绕开缓存——该规则与本组件一起被 QaApplicationService 复用。
 * </p>
 * <p>
 * 2.8（深度审查 P2）：预约意图判定收敛到 {@link IntentClassifier}，
 * 本组件不再依赖 {@link AnswerPipeline}，消除"缓存守卫→回答管线"越界与三角互知。
 * </p>
 *
 * @author forever-king
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheGuard {

    private final IntentClassifier intentClassifier;
    private final QaCacheService qaCacheService;
    private final SemanticCacheService semanticCacheService;
    private final ConversationRepository conversationRepository;
    private final BusinessMetrics metrics;

    /**
     * 是否为"纯知识类"问题：既不命中预约意图词，会话中也没有累积任何预约槽位。
     * 只有这类问题允许读写问答缓存——余量、档期、我的预约等实时数据一旦缓存
     * 就会向用户展示过期结果。
     */
    public boolean isKnowledgeOnlyQuery(String rewrittenQuery, ChatSession session) {
        return !intentClassifier.isAppointmentQuery(rewrittenQuery)
                && (session == null || session.slotsOrEmpty().isEmpty());
    }

    /**
     * 查缓存并按正常问答的收尾方式产出一轮回答。先精确（L1/L2），再语义（0.95）。
     *
     * @return 缓存命中时的完整答案；未命中返回 null（调用方继续走检索 + LLM）
     */
    public String answerFromCache(String originalQuery, String rewrittenQuery, String sid, Long userId,
                                  Consumer<String> onToken,
                                  Consumer<List<Conversation.CitationRef>> onCitations,
                                  Consumer<Long> onMessageId,
                                  long startTime) {
        List<Conversation.CitationRef> citations = List.of();
        String answer = null;

        // P1-02：精确缓存同样按归属过滤（此前只有语义缓存带 userId）
        var exact = qaCacheService.getCachedAnswer(rewrittenQuery, userId);
        if (exact.isPresent()) {
            answer = exact.get().answer();
            citations = exact.get().citations() == null ? List.of() : exact.get().citations();
        } else {
            // 3.8（深度审查 P1）：语义缓存按归属过滤——仅本人或全局共享条目可命中，
            // 避免 A 的私有文档答案被 B 语义命中（与 4.1 归属语义联动）
            SemanticCacheService.SemanticCacheHit semantic =
                    semanticCacheService.lookup(rewrittenQuery, userId);
            if (semantic != null) {
                answer = semantic.answer();
                citations = semantic.citations() == null ? List.of() : semantic.citations();
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
        return answer;
    }

    /**
     * 知识类回答写入两级缓存：精确缓存（Caffeine + Redis）与语义缓存（Qdrant 向量），
     * 两者都存引用快照，且都受 kb.cache.enabled 开关控制。
     *
     * @param ownerId 生成者（3.8：语义缓存落归属维度，null=未登录走全局共享）
     */
    public void populateKnowledgeCache(String rewrittenQuery, String answer,
                                       List<Conversation.CitationRef> citations, Long ownerId) {
        try {
            qaCacheService.cacheAnswer(rewrittenQuery, answer, citations, ownerId);
            semanticCacheService.store(rewrittenQuery, answer, citations, ownerId);
        } catch (Exception e) {
            // 缓存写入失败绝不影响主链路回答
            log.debug("问答缓存写入失败: {}", e.getMessage());
        }
    }
}