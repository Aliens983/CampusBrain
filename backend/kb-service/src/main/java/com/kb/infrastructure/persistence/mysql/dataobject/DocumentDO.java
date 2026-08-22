package com.kb.infrastructure.persistence.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Document Data Object — maps to the `document` MySQL table.
 *
 * @author forever-king
 */
@Data
@TableName("document")
public class DocumentDO {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文档标题 */
    private String title;

    /** 文件类型 */
    private String fileType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件存储路径 */
    private String filePath;

    /** 文档处理状态 */
    private String status;

    /** 分块数量 */
    private Integer chunkCount;

    /** 文档所属用户ID */
    private Long ownerId;

    /** 所属租户 ID（多租户隔离，null = 个人模式） */
    private Long tenantId;

    /** JSON字符串，存储元数据信息，在Repository实现层进行转换 */
    @TableField("metadata")
    private String metadataJson;

    /** 错误信息 */
    private String errorMsg;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
