package com.kb.domain.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 会话中等待用户确认的预约草稿
 * <p>
 * 助手生成草稿后不会直接下单，而是把草稿挂到会话上；
 * 用户下一轮回复"确认"才真正提交，回复"取消"则丢弃。
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingBooking implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 动作类型：BOOK 预约 / CANCEL 取消 */
    public static final String ACTION_BOOK = "BOOK";
    public static final String ACTION_CANCEL = "CANCEL";

    /** CAS 侧草稿ID（BOOK 动作有效） */
    private String draftId;

    /** 待取消的预约单号（CANCEL 动作有效） */
    private Long orderId;

    /** 动作类型 BOOK / CANCEL */
    private String action;

    /** 资源类型 SERVICE/CONSULTATION/ROOM/EQUIPMENT */
    private String resourceType;

    /** 资源类型中文名 */
    private String resourceTypeName;

    private Long serviceId;
    private String serviceName;
    private Long resourceId;
    private String resourceName;
    private String campusName;
    private String date;
    private String startTime;
    private String endTime;
    private Integer quantity;
    private Boolean needAudit;

    /** 一行摘要 */
    private String summary;

    /** 确认提示语 */
    private String confirmPrompt;

    /** 草稿过期时间（毫秒） */
    private Long expiresAt;

    public boolean isExpired() {
        return expiresAt != null && System.currentTimeMillis() > expiresAt;
    }
}
