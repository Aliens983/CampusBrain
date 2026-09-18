package com.kb.infrastructure.rag.graph;

import com.kb.domain.knowledgegraph.EntityExtractor;
import com.kb.domain.knowledgegraph.KnowledgeEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * {@link KnowledgeGraphService} 12-10 整改测试：
 * 跨分块共现加权、文档归属精确摘除、容量上限、一跳邻居扩展。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("知识图谱内存态治理测试")
class KnowledgeGraphServiceTest {

    @Mock
    private EntityExtractor entityExtractor;

    private KnowledgeEntity entity(String id, String name, double confidence) {
        return KnowledgeEntity.builder()
                .id(id).name(name).type("TERM").confidence(confidence).build();
    }

    @Test
    @DisplayName("同一实体对跨分块重复共现：边去重且权重累加（O(1) Map 判重）")
    void coOccurrenceDedupAndIncrementWeight() {
        KnowledgeGraphService kg = new KnowledgeGraphService(entityExtractor, 50000, 200000);
        when(entityExtractor.extract(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(entity("u1", "向量检索", 0.9), entity("u2", "RAG", 0.8)));

        kg.ingestChunk("文本甲", 1L, "c1");
        kg.ingestChunk("文本乙", 1L, "c2");

        assertThat(kg.stats().get("entityCount")).isEqualTo(2);
        assertThat(kg.stats().get("relationCount")).isEqualTo(1);
        // 邻居关系双向可达
        assertThat(kg.expandQuery("向量检索")).contains("RAG");
    }

    @Test
    @DisplayName("边方向无关：A-B 与 B-A 视为同一条边")
    void edgeDirectionAgnostic() {
        KnowledgeGraphService kg = new KnowledgeGraphService(entityExtractor, 50000, 200000);
        when(entityExtractor.extract(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(entity("u1", "A", 0.9), entity("u2", "B", 0.8)))
                .thenReturn(List.of(entity("u3", "B", 0.9), entity("u4", "A", 0.8)));

        kg.ingestChunk("t1", 1L, "c1");
        kg.ingestChunk("t2", 1L, "c2");

        assertThat(kg.stats().get("relationCount")).isEqualTo(1);
        assertThat(kg.expandQuery("A")).contains("B");
    }

    @Test
    @DisplayName("删除文档归属：独占实体/边摘除；跨文档共享的保留")
    void deleteByDocumentRemovesOnlyExclusiveStructures() {
        KnowledgeGraphService kg = new KnowledgeGraphService(entityExtractor, 50000, 200000);
        // 文档1：A-B；文档2：B-C
        when(entityExtractor.extract(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(entity("a", "A", 0.9), entity("b1", "B", 0.9)));
        when(entityExtractor.extract(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.eq(2L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(entity("b2", "B", 0.9), entity("c", "C", 0.9)));

        kg.ingestChunk("t1", 1L, "c1");
        kg.ingestChunk("t2", 2L, "c2");

        int[] removedDoc1 = kg.deleteByDocument(1L);

        // A 独占被摘除（1 实体），B-C 边保留、A-B 边摘除（1 边）
        assertThat(removedDoc1).containsExactly(1, 1);
        assertThat(kg.stats().get("entityCount")).isEqualTo(2);
        assertThat(kg.stats().get("relationCount")).isEqualTo(1);
        assertThat(kg.expandQuery("B")).contains("C");
        assertThat(kg.expandQuery("B")).doesNotContain("A");

        int[] removedDoc2 = kg.deleteByDocument(2L);
        assertThat(removedDoc2).containsExactly(2, 1);
        assertThat(kg.stats().get("entityCount")).isEqualTo(0);
        assertThat(kg.stats().get("relationCount")).isEqualTo(0);
    }

    @Test
    @DisplayName("重处理同一文档：先删后灌不会让旧实体残留")
    void reprocessSameDocumentDoesNotAccumulate() {
        KnowledgeGraphService kg = new KnowledgeGraphService(entityExtractor, 50000, 200000);
        when(entityExtractor.extract(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(entity("old", "旧概念", 0.9)))
                .thenReturn(List.of(entity("new", "新概念", 0.9)));

        kg.ingestChunk("旧文本", 1L, "c1");
        kg.deleteByDocument(1L);
        kg.ingestChunk("新文本", 1L, "c2");

        assertThat(kg.stats().get("entityCount")).isEqualTo(1);
        assertThat(kg.expandQuery("概念")).containsExactly("新概念");
    }

    @Test
    @DisplayName("容量上限：触顶后新实体与新边不再写入，既有结构保留")
    void capacityCapStopsNewStructures() {
        KnowledgeGraphService kg = new KnowledgeGraphService(entityExtractor, 1, 1);
        when(entityExtractor.extract(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                // 第一次：X 被索引（cap=1），端点 Y 被跳过 → 无边
                .thenReturn(List.of(entity("x", "X", 0.9), entity("y", "Y", 0.9)));

        kg.ingestChunk("t1", 1L, "c1");

        assertThat(kg.stats().get("entityCount")).isEqualTo(1);
        assertThat(kg.stats().get("relationCount")).isEqualTo(0);
    }
}
