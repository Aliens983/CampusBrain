package com.laoliu.cas.system.application.service.impl;

import com.laoliu.auth.policy.RolePolicy;
import com.laoliu.cas.common.enums.UserRoleEnum;
import com.laoliu.cas.common.exception.ForbiddenException;
import com.laoliu.cas.common.exception.ResourceNotFoundException;
import com.laoliu.cas.system.application.service.RoleService;
import com.laoliu.cas.system.domain.entity.User;
import com.laoliu.cas.system.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 角色管理服务实现
 *
 * @author forever-king
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final UserRepository userRepository;

    @Override
    public void setRoleById(Long userId, Integer newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(404, "用户不存在"));

        // 超级管理员不能被降级（code 口径唯一来源 RolePolicy）
        if (user.getRole() != null && user.getRole() == RolePolicy.SUPER_ADMIN.getCode()) {
            throw new ForbiddenException(403, "不能修改超级管理员的角色");
        }

        // 可分配角色集合由 RolePolicy 统一定义：仅 普通用户/管理员/教师；超管不可经接口设置（防提权）
        if (!RolePolicy.isAssignableUserRole(newRole)) {
            throw new ForbiddenException(403, "角色仅支持 0 普通用户 / 1 管理员 / 3 教师");
        }
        userRepository.updateRole(userId, newRole);
    }

    @Override
    public String getRoleByUserId(Long userId) {
        String role = userRepository.getRoleByUserId(userId);
        if (role == null) {
            return null;
        }
        // 角色 code→中文名唯一来源 UserRoleEnum（其 code 委托 RolePolicy）；
        // 脏数据（非数字/未知 code）按最小权限原则展示为普通用户，与权限解析同口径
        try {
            return UserRoleEnum.getByCode(Integer.parseInt(role)).getDescription();
        } catch (NumberFormatException e) {
            return UserRoleEnum.USER.getDescription();
        }
    }
}
