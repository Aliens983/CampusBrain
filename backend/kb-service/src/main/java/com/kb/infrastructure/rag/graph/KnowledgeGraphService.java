package com.kb.infrastructure.rag.graph;

import com.kb.domain.knowledgegraph.EntityExtractor;
import com.kb.domain.knowledgegraph.EntityRelation;
import com.kb.domain.knowledgegraph.KnowledgeEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 知识图谱服务 — 管理实体索引和关系构建。
 * <p>
 * 当前使用内存存储（ConcurrentHashMap），生产环境应迁移到 Neo4j 或
 * 图数据库。关系基于共现分析构建：同一分块中共同出现的实体之间建立边。
 * </p>
 *
 * @author forever-king
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeGraphService {

    private final EntityExtractor entityExtractor;

    /** 实体索引：name → Entity */
    private final Map<String, KnowledgeEntity> entityIndex = new ConcurrentHashMap<>();

    /** 实体关系列表 */
    private final List<EntityRelation> relations =
            Collections.synchronizedList(new ArrayList<>());

    /**
     * 从文档分块中抽取实体并构建关系图。
     */
    public void ingestChunk(String text, Long documentId, String chunkId) {
        List<KnowledgeEntity> entities = entityExtractor.extract(text, documentId, chunkId);
        if (entities.isEmpty()) return;

        // 索引实体（按名称去重，保留置信度更高的）
        for (KnowledgeEntity entity : entities) {
            entityIndex.merge(entity.getName(), entity, (existing, incoming) ->
                    incoming.getConfidence() > existing.getConfidence() ? incoming : existing);
        }

        // 构建共现关系：同一分块中的实体两两之间建立 RELATED_TO 边
        for (int i = 0; i < entities.size(); i++) {
            for (int j = i + 1; j < entities.size(); j++) {
                KnowledgeEntity a = entities.get(i);
                KnowledgeEntity b = entities.get(j);

                // 检查是否已有关系，有则增加权重
                boolean exists = relations.stream().anyMatch(r ->
                        (r.getSourceEntityId().equals(a.getId())
                                && r.getTargetEntityId().equals(b.getId()))
                                || (r.getSourceEntityId().equals(b.getId())
                                && r.getTargetEntityId().equals(a.getId())));

                if (!exists) {
                    relations.add(EntityRelation.builder()
                            .id(UUID.randomUUID().toString())
                            .sourceEntityId(a.getId())
                            .targetEntityId(b.getId())
                            .relationType("RELATED_TO")
                            .cooccurrenceCount(1)
                            .documentId(documentId)
                            .confidence(Math.min(a.getConfidence(), b.getConfidence()))
                            .build());
                }
            }
        }

        log.debug("KG ingested: {} entities, {} relations from chunk {}",
                entities.size(), relations.size(), chunkId);
    }

    /**
     * 根据查询关键词扩展实体 → 返回相关实体名称列表。
     * 用于检索时扩展 query 的语义覆盖范围。
     */
    public Set<String> expandQuery(String query) {
        Set<String> expansion = new HashSet<>();

        // 模糊匹配：查询中的词是否出现在实体名称中
        for (KnowledgeEntity entity : entityIndex.values()) {
            if (entity.getName().contains(query) || query.contains(entity.getName())) {
                expansion.add(entity.getName());
                // 进一步查找别名
                if (entity.getAliases() != null) {
                    expansion.addAll(Arrays.asList(entity.getAliases().split(",")));
                }
            }
        }

        // 查找相关联的实体（一跳邻居）
        Set<String> relatedNames = new HashSet<>();
        for (String name : expansion) {
            KnowledgeEntity matched = entityIndex.get(name);
            if (matched == null) continue;

            relations.stream()
                    .filter(r -> r.getSourceEntityId().equals(matched.getId())
                            || r.getTargetEntityId().equals(matched.getId()))
                    .forEach(r -> {
                        String otherId = r.getSourceEntityId().equals(matched.getId())
                                ? r.getTargetEntityId() : r.getSourceEntityId();
                        entityIndex.values().stream()
                                .filter(e -> e.getId().equals(otherId))
                                .findFirst()
                                .ifPresent(e -> relatedNames.add(e.getName()));
                    });
        }
        expansion.addAll(relatedNames);

        log.debug("Query expansion: [{}] → {}", query, expansion);
        return expansion;
    }

    /**
     * 统计信息。
     */
    public Map<String, Object> stats() {
        return Map.of(
                "entityCount", entityIndex.size(),
                "relationCount", relations.size()
        );
    }
}
