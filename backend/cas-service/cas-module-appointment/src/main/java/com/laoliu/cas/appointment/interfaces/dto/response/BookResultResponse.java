package com.laoliu.cas.appointment.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 预约结果响应 DTO
 *
 * @author forever-king
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "预约结果响应")
public class BookResultResponse {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户邮箱")
    private String email;

    @Schema(description = "用户年级")
    private String grade;

    @Schema(description = "用户预约的服务列表")
    private List<BookingDTO> allBookedServices;
}
