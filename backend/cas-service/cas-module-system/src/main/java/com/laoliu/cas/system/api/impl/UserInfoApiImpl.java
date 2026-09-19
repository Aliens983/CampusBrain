package com.laoliu.cas.system.api.impl;

import com.laoliu.cas.system.api.UserInfoApi;
import com.laoliu.cas.system.api.dto.UserInfoDTO;
import com.laoliu.cas.system.domain.entity.User;
import com.laoliu.cas.system.infrastructure.persistence.dataobject.UserDO;
import com.laoliu.cas.system.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 用户信息跨模块 API 实现（2.3：契约在 cas-module-system-api，实体映射留在本模块内部）
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
        if (userDO == null) {
            return null;
        }
        User user = userDO.toEntity();
        UserInfoDTO dto = new UserInfoDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setAge(user.getAge());
        dto.setSex(user.getSex());
        dto.setGrade(user.getGrade());
        return dto;
    }
}