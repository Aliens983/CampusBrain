package com.laoliu.cas.system.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author forever-king
 */
@Data
@Schema(description = "用户信息及预约服务响应")
public class UserInfoAndServicesViaMPResponse implements Serializable {

    // 安全：对外响应只承载 UserResponse 投影（无密码哈希），
    // 不得直接持有领域实体 User——其 password 字段会被 Jackson 原样序列化外泄。
    @Schema(description = "用户基本信息")
    private UserResponse user;

    @Schema(description = "用户预约的服务列表")
    private List<BookingRecordResponse> bookings;
}
