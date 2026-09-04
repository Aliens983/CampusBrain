package com.laoliu.cas.system.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户个人通知偏好（用于个人中心「通知偏好」，也作为请求/响应载体）。
 *
 * @author forever-king
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotifyPrefDTO {

    /** 是否接收邮件通知 */
    private Boolean emailOn = true;
}
