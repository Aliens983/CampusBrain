package com.kb.application.service;

import com.kb.domain.chat.ChatSession;
import com.kb.domain.conversation.Conversation;
import com.kb.domain.conversation.ConversationRepository;
import com.kb.domain.rag.CancellationToken;
import com.kb.domain.rag.LlmService;
import com.kb.domain.rag.RerankerService;
import com.kb.domain.rag.RetrievalResult;
import com.kb.domain.rag.SearchService;
import com.kb.infrastructure.metrics.BusinessMetrics;
import com.kb.infrastructure.rag.graph.GraphAssistedRetriever;
import com.kb.infrastructure.rag.llm.NoAnswerMarkers;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * 回答管线协作类（Q-01 从 QaApplicationService 拆出）。
 * <p>
 * 职责：历史消息加载、检索 + 重排、意图路由 → LLM 生成（流式/同步共用）、
 * 引用快照构建。拆分为独立组件后 QaApplicationService 仅负责编排；
 * 所有行为的对外表现（SSE 协议、断连取消、兜底与工具路由）与原实现完全一致。
 * </p>
 *
 * @author forever-king
 */
@Component
@RequiredArgsConstructor
public class AnswerPipeline {

    private final SearchService searchService;
    private final RerankerService rerankerService;
    private final LlmService llmService;
    private final GraphAssistedRetriever graphRetriever;
    private final ConversationRepository conversationRepository;
    private final BusinessMetrics metrics;

    /** 注入 LLM 的历史消息条数（对话表读取上限） */
    @Value("${kb.qa.history-limit:8}")
    private int historyLimit;

    /** 引用快照 snippet 截断长度 */
    @Value("${kb.qa.citation-snippet-length:120}")
    private int citationSnippetLength;

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

    public boolean isAppointmentQuery(String q) {
        if (q == null || q.isEmpty()) {
            return false;
        }
        return APPOINTMENT_KEYWORDS.stream().anyMatch(q::contains);
    }

    /**
     * 加载会话历史，转为 LLM 消息（过滤空白内容）。
     */
    public List<LlmService.ChatMessage> loadHistory(String sessionId, Long userId) {
        List<Conversation> messages =
                conversationRepository.getRecentMessages(sessionId, historyLimit, userId);
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

    /**
     * 检索 + 重排：
     * <ul>
     *   <li>SSE 链路（graphAssisted=true）走图谱增强检索 {@link GraphAssistedRetriever}；</li>
     *   <li>同步链路（graphAssisted=false）走关键词检索 {@link SearchService}。</li>
     * </ul>
     * 行为与原 QaApplicationService 的 Step 2（含检索时延埋点）完全一致。
     */
    public List<RetrievalResult> retrieveAndRerank(String rewritten, boolean graphAssisted) {
        long retrievalStart = System.currentTimeMillis();
        List<RetrievalResult> retrieved = graphAssisted
                ? graphRetriever.retrieve(rewritten)
                : searchService.search(rewritten);
        metrics.recordRetrievalLatency(System.currentTimeMillis() - retrievalStart);
        return rerankerService.rerank(rewritten, retrieved);
    }

    /**
     * 意图路由后的生成入口（流式/同步共用）：
     * <ul>
     *   <li>预约相关（含会话中已累积槽位）→ Function Calling 链路；</li>
     *   <li>本地资料有结果 → RAG；本地无法回答 → DeepSeek 兜底；</li>
     *   <li>本地无召回 → 直接流式兜底。</li>
     * </ul>
     */
    public String generate(String query, List<RetrievalResult> reranked,
                           List<LlmService.ChatMessage> history,
                           ChatSession session, Consumer<String> onToken,
                           CancellationToken cancellationToken) {
        boolean appointmentRelated = isAppointmentQuery(query) || !session.slotsOrEmpty().isEmpty();
        String contextHint = session.slotsOrEmpty().describe();

        // SSE 链路（onToken != null）：三条路径全部真流式，支持断连取消
        if (onToken != null) {
            if (appointmentRelated) {
                return llmService.generateAnswerWithToolsStreaming(
                        query, reranked, history, onToken, contextHint, cancellationToken);
            }
            if (reranked != null && !reranked.isEmpty()) {
                // RAG 流内部带"无法回答"前缀门控，命中标记时自动切换直接流式兜底，
                // 不会把"无法回答"与兜底答案拼在一起
                return llmService.generateAnswerStreaming(
                        query, reranked, history, onToken, cancellationToken);
            }
            return llmService.generateAnswerDirectStreaming(
                    query, history, onToken, cancellationToken);
        }

        // 同步链路：一次性返回
        if (appointmentRelated) {
            return llmService.generateAnswerWithTools(query, reranked, history, null, contextHint);
        }
        if (reranked != null && !reranked.isEmpty()) {
            String ragAnswer = llmService.generateAnswer(query, reranked, history);
            // Q-05：同步链路与流式前缀门控共用 NoAnswerMarkers 唯一词表，不做私有复制
            if (NoAnswerMarkers.looksLikeNoAnswer(ragAnswer)) {
                ragAnswer = llmService.generateAnswerDirect(query, history);
            }
            return ragAnswer;
        }
        return llmService.generateAnswerDirectStreaming(query, history, null, cancellationToken);
    }

    /**
     * 检索结果 → 引用快照（snippet 截断长度可配置，默认 120）。
     */
    public List<Conversation.CitationRef> buildCitations(List<RetrievalResult> results) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        return results.stream()
                .map(r -> Conversation.CitationRef.builder()
                        .documentId(r.getDocumentId())
                        .documentTitle(r.getDocumentTitle())
                        .chunkId(r.getChunkId())
                        .chunkIndex(r.getChunkIndex())
                        .snippet(r.getSnippet(citationSnippetLength))
                        .score(r.getScore())
                        .build())
                .toList();
    }
}