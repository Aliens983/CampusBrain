package com.laoliu.cas.system.api.impl;

import com.laoliu.cas.common.api.GetUserIdViaTokenApi;
import com.laoliu.cas.common.security.SecurityFrameworkUtils;
import org.springframework.stereotype.Component;

/**
 * 获取当前登录用户ID（系统模块提供，跨模块复用）。
 *
 * @author forever-king
 */
@Component
public class GetUserIdViaTokenApiImpl implements GetUserIdViaTokenApi {

    @Override
    public Long getUserId() {
        return SecurityFrameworkUtils.getLoginUserId();
    }
}
