package com.laoliu.cas.system.application.service.impl;

import com.laoliu.cas.common.exception.ForbiddenException;
import com.laoliu.cas.common.exception.ResourceNotFoundException;
import com.laoliu.cas.system.application.service.RoleService;
import com.laoliu.cas.system.domain.entity.User;
import com.laoliu.cas.system.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 角色管理服务实现。
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

        if (newRole == 1) {
            userRepository.updateRoleToAdmin(userId);
        } else {
            userRepository.updateRoleToCommonUser(userId);
        }
    }

    @Override
    public String getRoleByUserId(Long userId) {
        String role = userRepository.getRoleByUserId(userId);
        if (role != null) {
            if ("1".equals(role)) {
                return "管理员";
            } else if ("0".equals(role)) {
                return "普通用户";
            }
        }
        return role;
    }
}
