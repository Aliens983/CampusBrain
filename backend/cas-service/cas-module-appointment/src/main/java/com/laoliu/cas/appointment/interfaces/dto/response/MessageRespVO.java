package com.laoliu.cas.appointment.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 咨询沟通消息响应 VO
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "咨询沟通消息")
public class MessageRespVO implements Serializable {

    @Schema(description = "消息ID（轮询增量用 afterId）")
    private Long id;

    @Schema(description = "发送者用户ID")
    private Long senderId;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "发送时间")
    private LocalDateTime createdAt;

    @Schema(description = "是否我发的（前端据此左右对齐）")
    private Boolean isMine;
}
