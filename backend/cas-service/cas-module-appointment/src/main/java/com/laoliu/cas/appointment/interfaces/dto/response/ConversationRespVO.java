package com.laoliu.cas.appointment.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 咨询沟通会话响应 VO（会话列表 / 打开会话）
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "咨询沟通会话")
public class ConversationRespVO implements Serializable {

    @Schema(description = "会话ID")
    private Long id;

    @Schema(description = "对端用户ID（学生视角=教师，教师视角=学生）")
    private Long peerUserId;

    @Schema(description = "对端身份 student/teacher")
    private String peerRole;

    @Schema(description = "对端显示名")
    private String peerName;

    @Schema(description = "最后一条消息内容（可能为空）")
    private String lastMessage;

    @Schema(description = "最后一条消息时间（可能为空）")
    private LocalDateTime lastTime;

    @Schema(description = "发给我的未读数")
    private Long unreadCount;
}
