package com.kb.infrastructure.persistence.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.kb.domain.document.DocumentIndexCleaner;
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
public class EsDocumentRepository implements DocumentIndexCleaner {

    /** Elasticsearch客户端，用于执行索引、搜索、删除等操作 */
    private final ElasticsearchClient esClient;

    /** Elasticsearch索引名称，从配置文件注入 */
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
                // 分片失败不得只 warn：部分 chunk 未落 ES 时文档却会被标记 READY，
                // 关键词检索永久丢片，且对账链路感知不到。收集失败明细后上抛，
                // 由 MQ 消费端按可重试异常退避重试（按 chunkId 幂等覆盖写），
                // 超限进 DLQ 人工补偿，不留「READY 但索引残缺」的静默不一致。
                String details = response.items().stream()
                        .filter(item -> item.error() != null)
                        .map(item -> "{id=" + item.id()
                                + ", status=" + item.status()
                                + ", reason=" + item.error().reason() + "}")
                        .collect(Collectors.joining(", "));
                long failedCount = response.items().stream()
                        .filter(item -> item.error() != null)
                        .count();
                log.error("ES bulk index 部分分片失败: total={}, failed={}, errors={}",
                        documents.size(), failedCount, details);
                throw new RuntimeException("ES bulk index 部分分片失败: failed="
                        + failedCount + "/" + documents.size() + " — " + details);
            }
        } catch (IOException e) {
            throw new RuntimeException("ES bulk index failed", e);
        }
    }

    /**
     * Keyword (BM25) search on document chunk content.
     * 4.1（深度审查 P0）：按归属过滤——可见 = 全局共享（ownerId 缺失/未知）OR 当前用户私有。
     *
     * @param ownerId 当前登录用户 ID；null 表示匿名/系统检索，仅返回共享文档
     */
    public List<RetrievalResult> keywordSearch(String query, int topK, Long ownerId) {
        try {
            SearchResponse<EsDocumentEntity> response = esClient.search(s -> s
                            .index(indexName)
                            .query(q -> q
                                    .bool(b -> {
                                        b.must(m -> m
                                                .match(mm -> mm
                                                        .field("content")
                                                        .query(query)
                                                )
                                        );
                                        if (ownerId != null) {
                                            // (ownerId == 当前用户) OR (ownerId 缺失 -> 共享文档)
                                            b.should(sh -> sh.term(t -> t
                                                    .field("ownerId")
                                                    .value(ownerId.toString())));
                                            // exists:false 用 "must_not exists" 表达共享文档
                                            b.should(sh -> sh.bool(sb -> sb
                                                    .mustNot(mn -> mn.exists(e -> e.field("ownerId")))));
                                            b.minimumShouldMatch("1");
                                        } else {
                                            b.mustNot(mn -> mn.exists(e -> e.field("ownerId")));
                                        }
                                        return b;
                                    })
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
    @Override
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
            // A-04：不得吞掉失败——否则上层的有限重试与"失败落库 + 补偿对账"全部失效，
            // 已删除文档的 ES 孤儿记录会长期残留。转为非受检异常向上传播，
            // 由 DocumentApplicationService 的重试/对账链路与 MQ 消费端的清理 catch 处理。
            log.error("Failed to delete ES docs for document: {}", documentId, e);
            throw new java.io.UncheckedIOException(
                    "ES 删除文档索引失败: documentId=" + documentId, e);
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
