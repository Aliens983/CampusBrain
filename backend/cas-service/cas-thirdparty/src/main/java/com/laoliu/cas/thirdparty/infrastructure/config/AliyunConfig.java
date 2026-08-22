package com.laoliu.cas.thirdparty.infrastructure.config;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author forever-king
 */
@Configuration
public class AliyunConfig {

    @Value("${aliyun.access-key-id:}")
    private String accessKeyId;

    @Value("${aliyun.access-key-secret:}")
    private String accessKeySecret;

    @Value("${aliyun.region-id:cn-hangzhou}")
    private String regionId;

    @Bean
    public Client dysmsClient() throws Exception {
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret)
                .setRegionId(regionId);
        return new Client(config);
    }
}
