package com.kb.infrastructure.persistence.elasticsearch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Elasticsearch document entity for full-text + vector search.
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EsDocumentEntity {

    /**
     * 分块唯一ID（与Qdrant中的point ID保持一致）。
     * Chunk unique ID (same as Qdrant point ID)
     */
    private String chunkId;

    /**
     * 所属文档ID。
     * Parent document ID
     */
    private String documentId;

    /**
     * 文档显示标题。
     * Document display title
     */
    private String documentTitle;

    /**
     * 分块文本内容，用于关键词检索。
     * Chunk text content (for keyword search)
     */
    private String content;

    /**
     * 稠密向量嵌入（1024维，用于ES 8.x的kNN搜索）。
     * Dense vector embedding (1024-dim, for kNN search in ES 8.x)
     */
    private List<Float> contentVector;

    /**
     * 分块在文档中的顺序索引。
     * Chunk order index within document
     */
    private Integer chunkIndex;

    /**
     * 源文件类型。
     * Source file type
     */
    private String fileType;

    /**
     * PDF文件中的页码（如适用）。
     * Page number if from PDF
     */
    private Integer pageNumber;

    /**
     * 章节标题。
     * Section/chapter title
     */
    private String sectionTitle;

    /**
     * 创建时间，用于排序。
     * Creation timestamp for sorting
     */
    /** 所属租户 ID */
    private Long tenantId;

    private String createdAt;
}
