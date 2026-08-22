package com.kb.infrastructure.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO对象存储客户端配置
 *
 * @author forever-king
 */
@Slf4j
@Configuration
@Getter
public class MinioConfig {

    /** MinIO服务端点地址 */
    @Value("${minio.endpoint}")
    private String endpoint;

    /** MinIO访问密钥 */
    @Value("${minio.access-key}")
    private String accessKey;

    /** MinIO秘密密钥 */
    @Value("${minio.secret-key}")
    private String secretKey;

    /** 存储桶名称 */
    @Value("${minio.bucket}")
    private String bucket;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * 应用启动时自动创建存储桶（如果不存在）
     */
    @PostConstruct
    public void ensureBucket() {
        try {
            MinioClient client = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            boolean exists = client.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());

            if (!exists) {
                client.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket '{}' created successfully", bucket);
            } else {
                log.info("MinIO bucket '{}' already exists", bucket);
            }
        } catch (Exception e) {
            log.error("Failed to ensure MinIO bucket '{}'", bucket, e);
        }
    }
}
