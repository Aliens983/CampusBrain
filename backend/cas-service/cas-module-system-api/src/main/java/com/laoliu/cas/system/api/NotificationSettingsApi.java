package com.laoliu.cas.system.api;

/**
 * 通知设置跨模块 API（2.3）。
 * <p>只暴露其他业务模块真正需要的合成判断；管理端策略与用户偏好的读写
 * 仍由 system 模块内部的 NotificationSettingsService 承担，不进入本契约。
 *
 * @author forever-king
 */
public interface NotificationSettingsApi {

    /**
     * 是否允许给该用户发送邮件通知：
     * 管理端全局策略「邮件」启用 且 该用户个人偏好「邮件」开启。
     *
     * @param userId 目标用户 ID（null 视为允许）
     */
    boolean isEmailAllowed(Long userId);
}
