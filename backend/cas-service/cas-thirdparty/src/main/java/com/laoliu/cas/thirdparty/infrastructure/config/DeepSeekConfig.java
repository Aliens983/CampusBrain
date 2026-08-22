package com.laoliu.cas.thirdparty.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author forever-king
 */
@Data
@Component
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekConfig {
    private String apiKey;
    private String apiUrl;
    private int timeout = 30000;
}
