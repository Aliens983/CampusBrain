package com.laoliu.cas.thirdparty.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author forever-king
 */
@Data
@Component
@ConfigurationProperties(prefix = "qwen")
public class QwenConfig {
    private String apiKey;
    private String apiUrl = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
    private int timeout = 30000;
}
