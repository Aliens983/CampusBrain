package com.kb.infrastructure.persistence.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Document Chunk Data Object — maps to `document_chunk` MySQL table.
 *
 * @author forever-king
 */
@Data
@TableName("document_chunk")
public class DocumentChunkDO {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的文档ID */
    private Long documentId;

    /** 分块序号 */
    private Integer chunkIndex;

    /** 分块文本内容 */
    private String content;

    /** 分块内容的哈希值，用于去重 */
    private String chunkHash;

    /** Token数量 */
    private Integer tokenCount;

    /** JSON字符串，存储元数据信息 */
    @TableField("metadata")
    private String metadataJson;

    /** Qdrant向量库中的点ID */
    private String qdrantId;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
