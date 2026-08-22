package com.laoliu.cas.system.api.impl;

import com.laoliu.cas.system.api.UserInfoApi;
import com.laoliu.cas.system.api.dto.UserInfoDTO;
import com.laoliu.cas.system.infrastructure.persistence.dataobject.UserDO;
import com.laoliu.cas.system.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 用户信息跨模块 API 实现
 *
 * @author forever-king
 */
@Component
@RequiredArgsConstructor
public class UserInfoApiImpl implements UserInfoApi {

    private final UserMapper userMapper;

    @Override
    public UserInfoDTO getUserById(Long userId) {
        UserDO userDO = userMapper.selectById(userId);
        return UserInfoDTO.fromEntity(userDO != null ? userDO.toEntity() : null);
    }
}