package com.laoliu.cas.system.application.service;

/**
 * 角色管理服务。
 *
 * @author forever-king
 */
public interface RoleService {

    /**
     * 根据用户ID获取角色描述。
     *
     * @param userId 用户ID
     * @return "管理员"、"普通用户" 或 null
     */
    String getRoleByUserId(Long userId);

    /**
     * 设置用户角色。
     *
     * @param userId  目标用户ID
     * @param newRole 新角色值（0=普通用户, 1=管理员）
     * @throws com.laoliu.cas.common.exception.ResourceNotFoundException 用户不存在
     * @throws com.laoliu.cas.common.exception.ForbiddenException        试图修改超级管理员角色
     */
    void setRoleById(Long userId, Integer newRole);
}
