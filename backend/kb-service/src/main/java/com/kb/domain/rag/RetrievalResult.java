package com.kb.domain.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Value object representing a single retrieval result from the
 * hybrid search pipeline (keyword + vector → RRF fusion).
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalResult {

    /** 分块唯一标识 */
    private String chunkId;

    /** 所属文档ID */
    private String documentId;

    /** 文档标题，用于引用展示 */
    private String documentTitle;

    /** 分块的实际文本内容 */
    private String content;

    /** 分块在文档中的序号 */
    private int chunkIndex;

    /** RRF 融合后的综合相关性得分 */
    private double score;

    /** 结果来源：keyword（关键词检索）、vector（向量检索）或 hybrid（混合检索） */
    private String source;

    /** 页码（如果可从 PDF 元数据中获取） */
    private Integer pageNumber;

    /** 章节标题（如果可获取） */
    private String sectionTitle;

    /**
     * Return a short snippet for citation display.
     */
    public String getSnippet(int maxLength) {
        if (content == null) return "";
        return content.length() <= maxLength
                ? content
                : content.substring(0, maxLength) + "...";
    }
}
