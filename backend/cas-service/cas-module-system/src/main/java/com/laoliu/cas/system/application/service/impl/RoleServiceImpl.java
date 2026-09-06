package com.laoliu.cas.system.application.service.impl;

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

        // 超级管理员不能被降级
        if (user.getRole() != null && user.getRole() == 2) {
            throw new ForbiddenException(403, "不能修改超级管理员的角色");
        }

        // 允许的目标角色：0 普通用户 / 1 管理员 / 3 教师；不可设 2（超管需库内特殊处理，防提权）
        if (newRole == null || newRole < 0 || newRole > 3 || newRole == 2) {
            throw new ForbiddenException(403, "角色仅支持 0 普通用户 / 1 管理员 / 3 教师");
        }
        userRepository.updateRole(userId, newRole);
    }

    @Override
    public String getRoleByUserId(Long userId) {
        String role = userRepository.getRoleByUserId(userId);
        if (role != null) {
            return switch (role) {
                case "0" -> "普通用户";
                case "1" -> "管理员";
                case "2" -> "超级管理员";
                case "3" -> "教师";
                default -> role;
            };
        }
        return role;
    }
}
