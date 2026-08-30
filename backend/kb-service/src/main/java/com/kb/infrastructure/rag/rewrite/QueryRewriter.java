package com.kb.infrastructure.rag.rewrite;

import com.kb.domain.rag.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 查询改写服务 — 解决多轮对话中的指代消解问题
 * <p>
 * 当用户在多轮对话中使用代词（如"它"、"这个"、"上面说的"）时，
 * 结合对话历史将模糊指代改写为明确的、独立的查询语句
 * </p>
 * <p>
 * 示例：
 * <pre>
 * 历史：[用户: 年假有多少天？, 助手: 根据员工手册，年假为10天。]
 * 用户输入: "那怎么申请呢？"
 * 改写后: "年假怎么申请？"
 * </pre>
 * </p>
 * @author forever-king
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueryRewriter {

    private final LlmService llmService;

    /** 最大历史消息数，用于构建改写上下文 */
    private static final int MAX_HISTORY = 4;

    /**
     * 对用户查询进行上下文改写
     * <p>
     * 如果 conversationHistory 为空或是首轮对话，直接返回原始查询
     * 否则调用 LLM 进行指代消解
     * </p>
     *
     * @param query               原始用户查询
     * @param conversationHistory 最近的对话历史
     * @return 改写后的独立查询语句
     */
    public String rewrite(String query, List<LlmService.ChatMessage> conversationHistory) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return query;
        }

        // 只取最近 N 轮对话
        List<LlmService.ChatMessage> recentHistory = conversationHistory.size() > MAX_HISTORY
                ? conversationHistory.subList(conversationHistory.size() - MAX_HISTORY, conversationHistory.size())
                : conversationHistory;

        try {
            String rewritten = llmService.generateAnswer(
                    buildRewritePrompt(query, recentHistory),
                    List.of(),
                    List.of()
            );
            if (rewritten != null && !rewritten.isBlank() && rewritten.length() < 500) {
                log.debug("Query rewritten: [{}] -> [{}]", query, rewritten);
                return rewritten.trim();
            }
        } catch (Exception e) {
            log.warn("Query rewrite failed, using original query: {}", e.getMessage());
        }

        return query;
    }

    /**
     * 构建指代消解的提示词
     */
    private String buildRewritePrompt(String query, List<LlmService.ChatMessage> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个查询改写助手。根据对话历史，将用户的模糊查询改写为独立、明确的查询语句。\n\n");
        sb.append("规则：\n");
        sb.append("1. 将代词（'它'、'这个'、'那个'、'上面说的'）替换为具体指代的内容。\n");
        sb.append("2. 补充必要的上下文信息使查询独立。\n");
        sb.append("3. 如果查询已经很明确，原样返回。\n");
        sb.append("4. 只输出改写后的查询，不要任何解释。\n\n");

        sb.append("对话历史：\n");
        for (LlmService.ChatMessage msg : history) {
            String roleLabel = "user".equals(msg.role()) ? "用户" : "助手";
            sb.append(roleLabel).append(": ").append(msg.content()).append("\n");
        }

        sb.append("\n用户当前输入: ").append(query).append("\n");
        sb.append("改写后的查询: ");

        return sb.toString();
    }
}
