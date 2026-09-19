package com.kb.infrastructure.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch client configuration.
 *
 * @author forever-king
 */
@Configuration
public class ElasticsearchConfig {

    /** Elasticsearch服务器主机地址 */
    @Value("${elasticsearch.host}")
    private String host;

    /** Elasticsearch服务器端口 */
    @Value("${elasticsearch.port}")
    private int port;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // 4.18（深度审查 P2）：补齐 connect/socket 超时与连接池上限，
        // 防止 ES 挂起时问答检索线程被拖死（检索侧还有 30s 总预算兜底）。
        RestClient restClient = RestClient.builder(
                new HttpHost(host, port, "http")
        ).setRequestConfigCallback(cb -> cb
                .setConnectTimeout(5_000)
                .setSocketTimeout(30_000)
                .setConnectionRequestTimeout(5_000)
        ).setHttpClientConfigCallback(b -> b
                .setMaxConnTotal(50)
                .setMaxConnPerRoute(20)
        ).build();

        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper()
        );

        return new ElasticsearchClient(transport);
    }
}
