package com.kb.infrastructure.persistence.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.kb.domain.rag.RetrievalResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Repository for Elasticsearch operations on document chunks.
 * <p>
 * Used for keyword-based full-text search (BM25) in the hybrid retrieval pipeline.
 * </p>
 *
 * @author forever-king
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class EsDocumentRepository {

    /** Elasticsearch客户端，用于执行索引、搜索、删除等操作。 */
    private final ElasticsearchClient esClient;

    /** Elasticsearch索引名称，从配置文件注入。 */
    @Value("${elasticsearch.index-name}")
    private String indexName;

    /**
     * Bulk index a list of document chunks.
     */
    public void bulkIndex(List<EsDocumentEntity> documents) {
        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();

        for (EsDocumentEntity doc : documents) {
            bulkBuilder.operations(op -> op
                    .index(idx -> idx
                            .index(indexName)
                            .id(doc.getChunkId())
                            .document(doc)
                    )
            );
        }

        try {
            BulkResponse response = esClient.bulk(bulkBuilder.build());
            if (response.errors()) {
                log.warn("ES bulk index had errors: {}",
                        response.items().stream()
                                .filter(item -> item.error() != null)
                                .map(item -> item.error().reason())
                                .collect(Collectors.joining(", ")));
            }
        } catch (IOException e) {
            throw new RuntimeException("ES bulk index failed", e);
        }
    }

    /**
     * Keyword (BM25) search on document chunk content.
     */
    public List<RetrievalResult> keywordSearch(String query, int topK) {
        try {
            SearchResponse<EsDocumentEntity> response = esClient.search(s -> s
                            .index(indexName)
                            .query(q -> q
                                    .match(m -> m
                                            .field("content")
                                            .query(query)
                                    )
                            )
                            .size(topK)
                            .sort(sort -> sort
                                    .score(sc -> sc.order(SortOrder.Desc))
                            ),
                    EsDocumentEntity.class
            );

            return response.hits().hits().stream()
                    .map(this::toRetrievalResult)
                    .toList();

        } catch (IOException e) {
            throw new RuntimeException("ES keyword search failed", e);
        }
    }

    /**
     * Delete a document chunk from ES by ID.
     */
    public void deleteByChunkId(String chunkId) {
        try {
            esClient.delete(d -> d.index(indexName).id(chunkId));
        } catch (IOException e) {
            log.warn("Failed to delete ES document: {}", chunkId, e);
        }
    }

    /**
     * Delete all chunks belonging to a document.
     */
    public void deleteByDocumentId(String documentId) {
        try {
            esClient.deleteByQuery(d -> d
                    .index(indexName)
                    .query(q -> q
                            .term(t -> t
                                    .field("documentId")
                                    .value(documentId)
                            )
                    )
            );
        } catch (IOException e) {
            log.warn("Failed to delete ES docs for document: {}", documentId, e);
        }
    }

    private RetrievalResult toRetrievalResult(Hit<EsDocumentEntity> hit) {
        EsDocumentEntity doc = hit.source();
        return RetrievalResult.builder()
                .chunkId(doc.getChunkId())
                .documentId(doc.getDocumentId())
                .documentTitle(doc.getDocumentTitle())
                .content(doc.getContent())
                .chunkIndex(doc.getChunkIndex() != null ? doc.getChunkIndex() : 0)
                .score(hit.score() != null ? hit.score().doubleValue() : 0.0)
                .source("keyword")
                .pageNumber(doc.getPageNumber())
                .sectionTitle(doc.getSectionTitle())
                .build();
    }
}
