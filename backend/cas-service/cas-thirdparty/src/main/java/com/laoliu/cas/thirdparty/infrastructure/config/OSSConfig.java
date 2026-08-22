package com.laoliu.cas.thirdparty.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author forever-king
 */
@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class OSSConfig {
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
}
