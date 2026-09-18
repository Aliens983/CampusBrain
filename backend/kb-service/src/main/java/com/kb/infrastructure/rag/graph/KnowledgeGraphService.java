package com.kb.infrastructure.rag.graph;

import com.kb.domain.knowledgegraph.EntityExtractor;
import com.kb.domain.knowledgegraph.EntityRelation;
import com.kb.domain.knowledgegraph.KnowledgeEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 知识图谱服务 — 管理实体索引和关系构建（12-10 整改版）
 * <p>
 * 当前仍为<b>进程内内存态</b>（ConcurrentHashMap），不跨实例共享、重启丢失；
 * 中期方案是落 MySQL 边表/Neo4j，本轮先解决四个具体缺陷：
 * <ol>
 *   <li><b>无界增长</b>：实体/边加可配置容量上限（{@code kb.graph.max-entities /
 *       max-relations}），触顶后只保留既有节点/边，新结构丢弃并限频告警，
 *       避免文档无限灌入撑爆堆内存；</li>
 *   <li><b>不随文档清理</b>：维护实体/边对 documentId 的归属集合，
 *       {@link #deleteByDocument(Long)} 在文档删除/重处理时精确摘除；</li>
 *   <li><b>去重 O(n²)</b>：边以"有序实体名对"为键存 Map，判重/加权 O(1)，
 *       替代此前每对实体 stream 全表扫描（且旧实现按抽取器随机 UUID 配对，
 *       跨分块同名实体永远连不上边，去重实际失效）；</li>
 *   <li>抽取失败不拖垮文档入库：best-effort 由调用方（文档消费者）逐块 try/catch 兜底。</li>
 * </ol>
 * 邻接表 {@code name → 邻居名集合} 随边的增删同步维护，供 {@link #expandQuery}
 * 做一跳邻居扩展。所有集合均为并发结构，消费端多线程（prefetch&gt;1）可并发写入。
 *
 * @author forever-king
 */
@Slf4j
@Service
public class KnowledgeGraphService {

    private final EntityExtractor entityExtractor;

    /** 实体索引：规范名 → 实体（同名保留置信度更高者） */
    private final Map<String, KnowledgeEntity> entityIndex = new ConcurrentHashMap<>();

    /** 实体归属：实体名 → 出现过的文档 ID 集合（删文档时据此决定节点是否可摘除） */
    private final Map<String, Set<Long>> entityDocs = new ConcurrentHashMap<>();

    /** 关系表：有序实体名对 → 关系（O(1) 判重；已存在则累加共现权重） */
    private final Map<String, EntityRelation> relationByPair = new ConcurrentHashMap<>();

    /** 边归属：有序实体名对 → 共现过的文档 ID 集合 */
    private final Map<String, Set<Long>> edgeDocs = new ConcurrentHashMap<>();

    /** 邻接表：实体名 → 一跳邻居实体名集合，随边增删同步 */
    private final Map<String, Set<String>> adjacency = new ConcurrentHashMap<>();

    private final int maxEntities;
    private final int maxRelations;

    /** 触顶丢弃计数，用于限频告警（每 100 次打一条 warn，避免刷爆日志） */
    private final AtomicLong entityCapSkips = new AtomicLong();
    private final AtomicLong relationCapSkips = new AtomicLong();

    public KnowledgeGraphService(EntityExtractor entityExtractor,
                                 @Value("${kb.graph.max-entities:50000}") int maxEntities,
                                 @Value("${kb.graph.max-relations:200000}") int maxRelations) {
        this.entityExtractor = entityExtractor;
        this.maxEntities = maxEntities;
        this.maxRelations = maxRelations;
    }

    /**
     * 从文档分块中抽取实体并构建关系图。
     * <p>
     * 本方法只做内存计算、不抛受检异常；抽取/构建异常由调用方逐块吞掉（best-effort）。
     */
    public void ingestChunk(String text, Long documentId, String chunkId) {
        List<KnowledgeEntity> entities = entityExtractor.extract(text, documentId, chunkId);
        if (entities == null || entities.isEmpty()) {
            return;
        }

        // 1) 实体按规范名索引（同名保留高置信度），并登记文档归属
        for (KnowledgeEntity entity : entities) {
            String name = entity.getName();
            if (name == null || name.isBlank()) {
                continue;
            }
            boolean isNew = !entityIndex.containsKey(name);
            if (isNew && entityIndex.size() >= maxEntities) {
                long skipped = entityCapSkips.incrementAndGet();
                if (skipped % 100 == 1) {
                    log.warn("知识图谱实体数已达上限 {}，新实体不再索引（已累计丢弃 {}）；"
                            + "请尽快落地持久化图存储", maxEntities, skipped);
                }
                continue;
            }
            entityIndex.merge(name, entity, (existing, incoming) ->
                    incoming.getConfidence() != null && existing.getConfidence() != null
                            && incoming.getConfidence() > existing.getConfidence()
                            ? incoming : existing);
            entityDocs.computeIfAbsent(name, k -> ConcurrentHashMap.newKeySet())
                    .add(documentId);
        }

        // 2) 同一分块内实体两两共现建边，键为有序实体名对（O(1) 判重/加权）
        for (int i = 0; i < entities.size(); i++) {
            for (int j = i + 1; j < entities.size(); j++) {
                String a = entities.get(i).getName();
                String b = entities.get(j).getName();
                if (a == null || b == null || a.isBlank() || b.isBlank() || a.equals(b)) {
                    continue;
                }
                String pairKey = pairKey(a, b);

                // 容量只限制"新边"；已存在边允许继续累加权重与归属
                if (!relationByPair.containsKey(pairKey) && relationByPair.size() >= maxRelations) {
                    long skipped = relationCapSkips.incrementAndGet();
                    if (skipped % 100 == 1) {
                        log.warn("知识图谱边数已达上限 {}，新边不再构建（已累计丢弃 {}）",
                                maxRelations, skipped);
                    }
                    continue;
                }

                KnowledgeEntity aCanonical = entityIndex.get(a);
                KnowledgeEntity bCanonical = entityIndex.get(b);
                if (aCanonical == null || bCanonical == null) {
                    // 端点实体因触顶未索引：边无可达意义，跳过
                    continue;
                }

                relationByPair.compute(pairKey, (k, existing) -> {
                    if (existing == null) {
                        return EntityRelation.builder()
                                .id(UUID.randomUUID().toString())
                                .sourceEntityId(aCanonical.getId())
                                .targetEntityId(bCanonical.getId())
                                .relationType("RELATED_TO")
                                .cooccurrenceCount(1)
                                .documentId(documentId)
                                .confidence(minConfidence(aCanonical, bCanonical))
                                .build();
                    }
                    // 同一实体对再次共现：权重 +1（修正旧代码"有则加权"注释与实现不符）
                    Integer prev = existing.getCooccurrenceCount();
                    return EntityRelation.builder()
                            .id(existing.getId())
                            .sourceEntityId(existing.getSourceEntityId())
                            .targetEntityId(existing.getTargetEntityId())
                            .relationType(existing.getRelationType())
                            .cooccurrenceCount((prev == null ? 0 : prev) + 1)
                            .documentId(existing.getDocumentId())
                            .confidence(existing.getConfidence())
                            .build();
                });
                edgeDocs.computeIfAbsent(pairKey, k -> ConcurrentHashMap.newKeySet())
                        .add(documentId);
                adjacency.computeIfAbsent(a, k -> ConcurrentHashMap.newKeySet()).add(b);
                adjacency.computeIfAbsent(b, k -> ConcurrentHashMap.newKeySet()).add(a);
            }
        }

        log.debug("KG ingested: {} entities, {} relations from chunk {}",
                entities.size(), relationByPair.size(), chunkId);
    }

    /**
     * 删除某文档在图谱中的全部归属（12-10）。
     * <p>
     * 文档删除或强制重处理（消费者 Step0）时调用：实体/边的文档归属集合移除该文档，
     * 归属变空才真正摘除节点与边，并同步清理邻接表。
     * 不变量：边的文档集合是其两个端点实体文档集合的子集（同一次 ingest 原子登记），
     * 因此端点实体被摘除时，其关联边也必然已无归属，不会留下悬挂边。
     *
     * @return 被摘除的（实体数, 边数），便于调用方记日志
     */
    public synchronized int[] deleteByDocument(Long documentId) {
        if (documentId == null) {
            return new int[]{0, 0};
        }

        int edgesRemoved = 0;
        Iterator<Map.Entry<String, Set<Long>>> edgeIt = edgeDocs.entrySet().iterator();
        while (edgeIt.hasNext()) {
            Map.Entry<String, Set<Long>> e = edgeIt.next();
            if (e.getValue().remove(documentId) && e.getValue().isEmpty()) {
                String pairKey = e.getKey();
                edgeIt.remove();
                relationByPair.remove(pairKey);
                String[] names = splitPairKey(pairKey);
                removeAdjacency(names[0], names[1]);
                removeAdjacency(names[1], names[0]);
                edgesRemoved++;
            }
        }

        int entitiesRemoved = 0;
        Iterator<Map.Entry<String, Set<Long>>> entIt = entityDocs.entrySet().iterator();
        while (entIt.hasNext()) {
            Map.Entry<String, Set<Long>> e = entIt.next();
            if (e.getValue().remove(documentId) && e.getValue().isEmpty()) {
                String name = e.getKey();
                entIt.remove();
                entityIndex.remove(name);
                // 理论上邻居边已在上一步清空，这里兜底摘掉残余邻接引用
                adjacency.remove(name);
                for (Set<String> neighbors : adjacency.values()) {
                    neighbors.remove(name);
                }
                entitiesRemoved++;
            }
        }

        if (entitiesRemoved > 0 || edgesRemoved > 0) {
            log.info("KG 清理文档归属完成: documentId={}, 摘除实体={}, 摘除边={}",
                    documentId, entitiesRemoved, edgesRemoved);
        }
        return new int[]{entitiesRemoved, edgesRemoved};
    }

    /**
     * 根据查询关键词扩展实体 → 返回相关实体名称列表
     * 用于检索时扩展 query 的语义覆盖范围
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

        // 一跳邻居（直接查邻接表，不再按随机实体 ID 全表扫边）
        Set<String> relatedNames = new HashSet<>();
        for (String name : new ArrayList<>(expansion)) {
            Set<String> neighbors = adjacency.get(name);
            if (neighbors != null) {
                relatedNames.addAll(neighbors);
            }
        }
        expansion.addAll(relatedNames);

        log.debug("Query expansion: [{}] → {}", query, expansion);
        return expansion;
    }

    /**
     * 统计信息
     */
    public Map<String, Object> stats() {
        return Map.of(
                "entityCount", entityIndex.size(),
                "relationCount", relationByPair.size(),
                "maxEntities", maxEntities,
                "maxRelations", maxRelations
        );
    }

    // ==================== 内部工具 ====================

    /** 有序实体名对作为边的规范键，与方向无关（A-B 与 B-A 视为同一条边） */
    private static String pairKey(String a, String b) {
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }

    private static String[] splitPairKey(String key) {
        int idx = key.indexOf('|');
        return new String[]{key.substring(0, idx), key.substring(idx + 1)};
    }

    private void removeAdjacency(String from, String to) {
        Set<String> neighbors = adjacency.get(from);
        if (neighbors != null) {
            neighbors.remove(to);
        }
    }

    private static Double minConfidence(KnowledgeEntity a, KnowledgeEntity b) {
        if (a.getConfidence() == null) {
            return b.getConfidence();
        }
        if (b.getConfidence() == null) {
            return a.getConfidence();
        }
        return Math.min(a.getConfidence(), b.getConfidence());
    }
}
