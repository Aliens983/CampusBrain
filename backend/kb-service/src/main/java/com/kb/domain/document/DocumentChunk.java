package com.kb.domain.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Document chunk entity.
 * <p>
 * A segment of document text after splitting, stored in both
 * MySQL (for reference) and Qdrant/ES (for vector/keyword search).
 * </p>
 * @author forever-king
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    /** 分块唯一标识 */
    private Long id;

    /** 所属文档ID */
    private Long documentId;

    /** 分块在文档中的序号（从0开始） */
    private Integer chunkIndex;

    /** 分块的文本内容 */
    private String content;

    /** 分块内容的哈希值，用于去重 */
    private String chunkHash;

    /** 分块的 Token 数量 */
    private Integer tokenCount;

    /** 分块元数据 */
    private Map<String, Object> metadata;

    /** Qdrant 中对应的向量点 ID */
    private String qdrantId;

    /** 分块创建时间 */
    private LocalDateTime createdAt;

    /**
     * Associate this chunk with a Qdrant point after vector storage.
     */
    public void assignQdrantId(String qdrantId) {
        this.qdrantId = qdrantId;
    }

    /**
     * Get a metadata value by key.
     */
    public Object getMetadataValue(String key) {
        return metadata != null ? metadata.get(key) : null;
    }
}
