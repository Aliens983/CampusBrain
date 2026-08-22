package com.laoliu.cas.server.controller;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sentinel 限流演示端点。
 * <p>
 * 资源 {@code sentinel-demo-limited} 配置 QPS=1 的流控规则，
 * 超过阈值的请求抛出 {@link BlockException}，返回「被限流」。
 *
 * @author forever-king
 */
@RestController
@RequestMapping("/sentinel-demo")
public class SentinelDemoController {

    @GetMapping("/limited")
    public String limited() {
        Entry entry = null;
        try {
            entry = SphU.entry("sentinel-demo-limited");
            return "请求通过";
        } catch (BlockException e) {
            return "请求被限流了";
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }
}
