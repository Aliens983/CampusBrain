package com.laoliu.cas.common.exception.code;

import com.laoliu.cas.common.exception.ErrorCode;

/**
 * 角色相关错误码 —— 已废弃，请使用 {@link UserErrorCode}。
 *
 * @author forever-king
 * @deprecated 使用 {@link UserErrorCode} 替代
 */
@Deprecated
public interface RoleErrorCode {

    ErrorCode ROLE_NOT_FOUND = new ErrorCode(20017, "角色不存在");

    ErrorCode ROLE_UPDATE_FAILED = new ErrorCode(20018, "角色更新失败");

    ErrorCode PERMISSION_DENIED = new ErrorCode(20019, "权限不足");

    ErrorCode ADMIN_ROLE_REQUIRED = new ErrorCode(20020, "需要管理员权限");

}
