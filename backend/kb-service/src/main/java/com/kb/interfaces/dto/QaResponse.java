package com.kb.interfaces.dto;

import com.kb.domain.conversation.Conversation.CitationRef;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Q&A response DTO (used for non-streaming endpoint).
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QaResponse {

    /** 会话ID */
    private String sessionId;

    /** 用户原始问题 */
    private String query;

    /** 模型生成的回答 */
    private String answer;

    /** 回答中引用的文档来源列表 */
    private List<CitationRef> citations;

    /** 引用来源数量 */
    private Integer sourcesCount;

    /** 处理耗时（毫秒） */
    private Long processingTimeMs;
}
