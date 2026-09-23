package com.kb.infrastructure.persistence.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.OperationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * ES bulk 部分分片失败必须上抛（消费端重试/DLQ），不得仅 warn 后把文档标 READY。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
class EsDocumentRepositoryBulkTest {

    @Mock
    private ElasticsearchClient esClient;

    private EsDocumentRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        repository = new EsDocumentRepository(esClient);
        Field indexName = EsDocumentRepository.class.getDeclaredField("indexName");
        indexName.setAccessible(true);
        indexName.set(repository, "chunks");
    }

    @Test
    @DisplayName("bulk 部分分片失败：抛异常并携带失败分片 id 与原因，不能静默成功")
    void shouldThrowWhenAnyBulkItemHasError() throws Exception {
        BulkResponseItem failed = BulkResponseItem.of(b -> b
                .operationType(OperationType.Index)
                .index("chunks")
                .id("chunk-2")
                .status(400)
                .error(e -> e.type("mapper_parsing_exception").reason("字段映射失败")));
        BulkResponseItem succeeded = BulkResponseItem.of(b -> b
                .operationType(OperationType.Index).index("chunks").id("chunk-1").status(201));
        BulkResponse response = BulkResponse.of(b -> b
                .took(3L)
                .errors(true)
                .items(List.of(succeeded, failed)));
        when(esClient.bulk(any(BulkRequest.class))).thenReturn(response);

        EsDocumentEntity doc = EsDocumentEntity.builder().chunkId("chunk-2").build();
        assertThatThrownBy(() -> repository.bulkIndex(List.of(doc)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("部分分片失败")
                .hasMessageContaining("chunk-2")
                .hasMessageContaining("字段映射失败");
    }

    @Test
    @DisplayName("bulk 全部分片成功：正常返回不抛异常")
    void shouldSucceedWhenNoBulkItemErrors() throws Exception {
        BulkResponse response = BulkResponse.of(b -> b
                .took(2L)
                .errors(false)
                .items(List.of(BulkResponseItem.of(i -> i
                        .operationType(OperationType.Index).index("chunks").id("chunk-1").status(201)))));
        when(esClient.bulk(any(BulkRequest.class))).thenReturn(response);

        repository.bulkIndex(List.of(EsDocumentEntity.builder().chunkId("chunk-1").build()));
    }
}
