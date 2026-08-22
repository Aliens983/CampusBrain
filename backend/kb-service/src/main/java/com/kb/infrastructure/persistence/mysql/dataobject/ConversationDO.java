package com.kb.infrastructure.persistence.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Conversation Data Object — maps to `conversation` MySQL table.
 *
 * @author forever-king
 */
@Data
@TableName("conversation")
public class ConversationDO {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话ID */
    private String sessionId;

    /** 消息角色（user/assistant） */
    private String role;

    /** 消息内容 */
    private String content;

    /** JSON字符串，存储引用来源数组 */
    @TableField("references_json")
    private String referencesJson;

    /** 用户反馈 */
    private String feedback;

    /** 所属租户 ID（多租户隔离） */
    private Long tenantId;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
