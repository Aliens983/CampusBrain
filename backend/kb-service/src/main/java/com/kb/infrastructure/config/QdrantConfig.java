package com.kb.infrastructure.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Qdrant vector database client configuration.
 *
 * @author forever-king
 */
@Configuration
public class QdrantConfig {

    /** Qdrant向量数据库主机地址 */
    @Value("${qdrant.host}")
    private String host;

    /** Qdrant向量数据库端口 */
    @Value("${qdrant.port}")
    private int port;

    /** Qdrant 服务 API Key；本地开发未启用鉴权时为空 */
    @Value("${qdrant.api-key:}")
    private String apiKey;

    @Bean
    public QdrantClient qdrantClient() {
        String resolvedHost = "localhost".equals(host) ? "127.0.0.1" : host;
        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(resolvedHost, port, false)
                .withTimeout(Duration.ofSeconds(30));
        if (apiKey != null && !apiKey.isBlank()) {
            builder.withApiKey(apiKey);
        }
        return new QdrantClient(builder.build());
    }
}
