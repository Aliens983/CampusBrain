package com.kb.domain.conversation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Conversation message entity.
 * <p>
 * Each row is one message (user question or assistant answer) in a
 * multi-turn conversation, identified by session_id.
 * </p>
 * @author forever-king
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    /** 消息唯一标识 */
    private Long id;

    /** 会话ID，标识一次多轮对话 */
    private String sessionId;

    /** 消息角色：user（用户）、assistant（助手）或 system（系统） */
    private String role;

    /** 消息文本内容 */
    private String content;

    /** 从 references_json 解析出的引用来源列表 */
    private List<CitationRef> references;

    /** 用户反馈：like（点赞）、dislike（踩）或 null（无反馈） */
    private String feedback;

    /** 所属租户 ID（多租户隔离） */
    private Long tenantId;

    /** 消息创建时间 */
    private LocalDateTime createdAt;

    /**
     * Citation reference within an answer.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CitationRef {
        /** 引用的文档ID */
        private String documentId;

        /** 引用的文档标题 */
        private String documentTitle;

        /** 引用的分块ID */
        private String chunkId;

        /** 分块在文档中的序号 */
        private Integer chunkIndex;

        /** 引用片段的文本摘要 */
        private String snippet;

        /** 相关性得分 */
        private Double score;
    }

    public boolean isUser() {
        return "user".equalsIgnoreCase(role);
    }

    public boolean isAssistant() {
        return "assistant".equalsIgnoreCase(role);
    }

    public void recordFeedback(String feedback) {
        if (!"like".equals(feedback) && !"dislike".equals(feedback)) {
            throw new IllegalArgumentException("Feedback must be 'like' or 'dislike'");
        }
        this.feedback = feedback;
    }
}
