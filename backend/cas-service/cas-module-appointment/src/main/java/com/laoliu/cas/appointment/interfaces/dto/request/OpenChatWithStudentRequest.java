package com.laoliu.cas.appointment.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 打开咨询会话请求（教师对名下咨询档期的学生发起，教师侧使用）
 *
 * @author forever-king
 */
@Data
@Schema(description = "教师对咨询过的学生打开咨询会话请求")
public class OpenChatWithStudentRequest {

    @NotNull(message = "请选择学生")
    @Schema(description = "学生用户ID")
    private Long studentId;
}
