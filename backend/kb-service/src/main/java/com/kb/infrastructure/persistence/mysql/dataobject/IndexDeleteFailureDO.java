package com.kb.infrastructure.persistence.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 外部索引删除失败对账 DO，映射 {@code index_delete_failure} 表（A-04）。
 *
 * @author forever-king
 */
@Data
@TableName("index_delete_failure")
public class IndexDeleteFailureDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    /** QDRANT / ELASTICSEARCH */
    private String target;

    private String failReason;

    private Integer retryCount;

    /** PENDING / RESOLVED / GIVE_UP */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
