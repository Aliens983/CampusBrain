package com.laoliu.cas.system.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端全局通知策略（用于系统设置「通知策略」）。
 *
 * @author forever-king
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotifyPolicyDTO {

    /** 全局是否启用邮件通知 */
    private Boolean emailEnabled = true;

    /** 全局是否启用短信通知（需短信服务开通，当前默认关闭） */
    private Boolean smsEnabled = false;
}
