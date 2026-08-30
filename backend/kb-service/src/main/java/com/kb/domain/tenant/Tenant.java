package com.kb.domain.tenant;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户/工作空间实体 — 实现多租户数据隔离
 * <p>
 * 每个租户拥有独立的知识库、文档、对话记录
 * 用户通过 tenant_id 关联到所属组织
 * </p>
 *
 * @author forever-king
 */
@Data
@Builder
public class Tenant {

    /** 租户唯一标识 */
    private Long id;

    /** 租户名称（组织/公司名） */
    private String name;

    /** 租户编码（URL-safe 标识符） */
    private String code;

    /** 管理员用户 ID */
    private Long ownerId;

    /** 租户状态：ACTIVE / SUSPENDED / DELETED */
    private String status;

    /** 成员数量上限 */
    private Integer maxMembers;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
