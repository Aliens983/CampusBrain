package com.laoliu.cas.system.api;

import com.laoliu.cas.system.api.dto.UserInfoDTO;

/**
 * 用户信息跨模块 API 接口
 *
 * @author forever-king
 */
public interface UserInfoApi {

    /**
     * 根据 ID 获取用户信息
     * @param userId 用户 ID
     * @return 用户信息 DTO
     */
    UserInfoDTO getUserById(Long userId);
}
