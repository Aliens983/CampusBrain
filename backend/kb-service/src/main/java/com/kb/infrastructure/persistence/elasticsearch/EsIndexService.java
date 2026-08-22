package com.kb.infrastructure.persistence.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.JsonData;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Manages the Elasticsearch index lifecycle.
 * <p>
 * Creates the kb_documents index with appropriate mappings
 * for both keyword (BM25) and vector (kNN) search capabilities.
 * </p>
 *
 * @author forever-king
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsIndexService {

    /** Elasticsearch客户端，用于索引管理操作。 */
    private final ElasticsearchClient esClient;

    /** Elasticsearch索引名称，从配置文件注入。 */
    @Value("${elasticsearch.index-name}")
    private String indexName;

    /** 向量维度大小，与Qdrant配置保持一致，从配置文件注入。 */
    @Value("${qdrant.vector-size}")
    private int vectorSize;

    /**
     * Ensure the index exists on startup (idempotent).
     */
    @PostConstruct
    public void ensureIndex() {
        try {
            ExistsRequest existsRequest = ExistsRequest.of(e -> e.index(indexName));
            boolean exists = esClient.indices().exists(existsRequest).value();

            if (!exists) {
                createIndex();
                log.info("Elasticsearch index '{}' created successfully", indexName);
            } else {
                log.info("Elasticsearch index '{}' already exists", indexName);
            }
        } catch (IOException e) {
            log.error("Failed to ensure Elasticsearch index '{}'", indexName, e);
        }
    }

    private void createIndex() throws IOException {
        esClient.indices().create(CreateIndexRequest.of(c -> c
                .index(indexName)
                .settings(s -> s
                        .numberOfShards("1")
                        .numberOfReplicas("0")
                )
                .mappings(m -> m
                        .properties("chunkId", p -> p.keyword(k -> k))
                        .properties("documentId", p -> p.keyword(k -> k))
                        .properties("documentTitle", p -> p
                                .text(t -> t.analyzer("standard"))
                        )
                        .properties("content", p -> p
                                .text(t -> t.analyzer("standard"))
                        )
                        .properties("contentVector", p -> p
                                .denseVector(d -> d
                                        .dims(vectorSize)
                                        .index(true)
                                        .similarity("cosine")
                                )
                        )
                        .properties("chunkIndex", p -> p.integer(i -> i))
                        .properties("fileType", p -> p.keyword(k -> k))
                        .properties("pageNumber", p -> p.integer(i -> i))
                        .properties("sectionTitle", p -> p
                                .text(t -> t.analyzer("standard"))
                        )
                        .properties("createdAt", p -> p.date(d -> d))
                )
        ));
    }
}
