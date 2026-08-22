package com.laoliu.cas.system.interfaces.dto.response;

import com.laoliu.cas.system.domain.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author forever-king
 */
@Data
@Schema(description = "用户信息及预约服务响应")
public class UserInfoAndServicesViaMPRespVO implements Serializable {

    @Schema(description = "用户基本信息")
    private User user;

    @Schema(description = "用户预约的服务列表")
    private List<BookingRecordRespVO> bookings;
}
