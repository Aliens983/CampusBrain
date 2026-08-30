package com.laoliu.cas.server.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nacos 配置中心热更新演示端点
 * <p>
 * 配置项 {@code cas.greeting} 托管在 Nacos（dataId = cas-service.yaml），
 * 修改后无需重启即可生效（@RefreshScope）
 *
 * @author forever-king
 */
@RestController
@RequestMapping("/config-demo")
@RefreshScope
public class ConfigDemoController {

    @Value("${cas.greeting:默认问候}")
    private String greeting;

    @GetMapping("/greeting")
    public String greeting() {
        return greeting;
    }
}
