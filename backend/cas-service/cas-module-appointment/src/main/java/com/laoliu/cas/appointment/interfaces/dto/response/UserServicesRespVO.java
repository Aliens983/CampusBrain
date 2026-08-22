package com.laoliu.cas.appointment.interfaces.dto.response;

import com.laoliu.cas.common.result.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户已预约服务响应 VO。
 *
 * @author forever-king
 */
@Data
@Schema(description = "用户已预约服务响应")
public class UserServicesRespVO {

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户角色")
    private Integer userRole;

    @Schema(description = "用户邮箱")
    private String userEmail;

    @Schema(description = "已预约服务（分页）")
    private PageResult<ServiceRespVO> services;

    public static UserServicesRespVO of(String userName, Long userId, Integer role, String email, PageResult<ServiceRespVO> services) {
        UserServicesRespVO vo = new UserServicesRespVO();
        vo.setUserName(userName);
        vo.setUserId(userId);
        vo.setUserRole(role);
        vo.setUserEmail(email);
        vo.setServices(services);
        return vo;
    }
}
