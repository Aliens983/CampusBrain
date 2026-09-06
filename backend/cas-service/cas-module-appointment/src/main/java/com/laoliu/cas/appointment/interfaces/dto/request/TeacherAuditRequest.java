package com.laoliu.cas.appointment.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 教师审核请求（通过时 reason 可空；拒绝时 reason 必填，由审计逻辑校验）
 *
 * @author forever-king
 */
@Data
@Schema(description = "教师审核请求")
public class TeacherAuditRequest {

    @Schema(description = "审核备注 / 拒绝原因（拒绝时必填）")
    private String reason;
}
