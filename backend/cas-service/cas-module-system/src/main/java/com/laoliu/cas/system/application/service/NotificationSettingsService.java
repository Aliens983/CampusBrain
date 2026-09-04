package com.laoliu.cas.system.application.service;

import com.laoliu.cas.system.infrastructure.persistence.mapper.NotificationPolicyMapper;
import com.laoliu.cas.system.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 通知设置服务（管理端全局策略 + 用户个人偏好，MySQL 存储）。
 *
 * <p>里程碑 A：让「管理端系统设置→通知策略」与「用户端个人中心→通知偏好」
 * 共用同一套开关/存储，并让审核结果邮件的发送真正受这两个开关控制。</p>
 *
 * <p>存储约定（对应 DDL：cas-service/sql/10_notification_settings.sql）：
 * <ul>
 *   <li>管理端全局策略：notification_policy 单行表（id=1）的 email_enabled / site_enabled / sms_enabled</li>
 *   <li>用户个人偏好：user 表的 email_notify / site_notify 列</li>
 * </ul>
 * 实际发送 = 管理端策略启用 且 用户偏好开启。</p>
 *
 * @author forever-king
 */
@Service
@RequiredArgsConstructor
public class NotificationSettingsService {

    private final UserMapper userMapper;
    private final NotificationPolicyMapper policyMapper;

    // ==================== 管理端全局策略 ====================

    public boolean isEmailEnabled() {
        return enabled(policyMapper.selectEmailEnabled());
    }

    public boolean isSiteEnabled() {
        return enabled(policyMapper.selectSiteEnabled());
    }

    public boolean isSmsEnabled() {
        return enabled(policyMapper.selectSmsEnabled());
    }

    public void setEmailEnabled(boolean on) {
        policyMapper.updateEmailEnabled(on);
    }

    public void setSiteEnabled(boolean on) {
        policyMapper.updateSiteEnabled(on);
    }

    public void setSmsEnabled(boolean on) {
        policyMapper.updateSmsEnabled(on);
    }

    // ==================== 用户个人偏好 ====================

    public boolean isEmailOn(Long userId) {
        return userId == null || enabled(userMapper.selectEmailNotifyByUserId(userId));
    }

    public boolean isSiteOn(Long userId) {
        return userId == null || enabled(userMapper.selectSiteNotifyByUserId(userId));
    }

    public void setEmailOn(Long userId, boolean on) {
        if (userId != null) {
            userMapper.updateEmailNotify(userId, on);
        }
    }

    public void setSiteOn(Long userId, boolean on) {
        if (userId != null) {
            userMapper.updateSiteNotify(userId, on);
        }
    }

    // ==================== 合成判断 ====================

    /**
     * 是否允许给该用户发送邮件通知：
     * 管理端全局策略「邮件」启用 且 该用户个人偏好「邮件」开启。
     */
    public boolean isEmailAllowed(Long userId) {
        return isEmailEnabled() && (userId == null || isEmailOn(userId));
    }

    /**
     * 是否允许给该用户发送站内通知（预留：站内消息中心尚未实现）。
     */
    public boolean isSiteAllowed(Long userId) {
        return isSiteEnabled() && (userId == null || isSiteOn(userId));
    }

    // ==================== 工具 ====================

    /** 数据库 0/1 → boolean；字段缺失时默认按开启处理 */
    private boolean enabled(Integer v) {
        return v == null || v == 1;
    }
}
