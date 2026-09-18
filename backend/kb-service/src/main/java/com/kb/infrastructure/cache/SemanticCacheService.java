package com.kb.infrastructure.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.domain.conversation.Conversation;
import com.kb.domain.rag.EmbeddingService;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Points;
import io.qdrant.client.grpc.Points.Condition;
import io.qdrant.client.grpc.Points.DeletePoints;
import io.qdrant.client.grpc.Points.FieldCondition;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.Points.Match;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.PointsSelector;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;
import io.qdrant.client.grpc.Points.UpsertPoints;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;
import static io.qdrant.client.WithPayloadSelectorFactory.enable;

/**
 * 语义缓存服务 — 基于 Qdrant 向量检索的问答缓存。
 * <p>
 * 与精确匹配缓存（{@link QaCacheService}）互补：问题字面不同但语义高度相似时，
 * 通过向量余弦相似度（阈值可配，默认 0.95）命中缓存。
 * </p>
 * <h3>12-02/12-05 重构说明</h3>
 * 旧实现把向量以字符串存进 Redis，每次未命中都要 SCAN 全量键 + 逐条 2 次 GET
 * 在应用层算余弦（O(N) 次 Redis 往返），且键用 32 位 {@code hashCode()} 存在碰撞错答。
 * 现改为：
 * <ul>
 *   <li>独立 Qdrant collection，HNSW top1 检索，一次 gRPC 往返完成相似度匹配；</li>
 *   <li>点位 ID 由问题 SHA-256 确定性派生（UUIDv3），同问题幂等覆盖、无碰撞错答；</li>
 *   <li>答案与引用快照一起存 payload，语义命中也能带引用（与精确缓存行为一致）；</li>
 *   <li>payload 带 cached_at，检索过滤 + 应用层双重 TTL 判定，过期点懒删除。</li>
 * </ul>
 * 与精确缓存共用 {@code kb.cache.enabled} 开关；关闭时所有方法短路。
 *
 * @author forever-king
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticCacheService {

    /** payload 中标识"问答语义缓存"点的固定字段，供全量淘汰按 filter 删除 */
    private static final String CACHE_TYPE = "qa";
    private static final String FIELD_CACHE_TYPE = "cache_type";
    private static final String FIELD_CACHED_AT = "cached_at";
    private static final String FIELD_ANSWER = "answer";
    private static final String FIELD_CITATIONS = "citations";
    private static final String FIELD_QUESTION = "question";

    private final QdrantClient qdrantClient;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    @Value("${kb.cache.enabled:true}")
    private boolean cacheEnabled;

    /** 相似度阈值：仅 top1 分数 ≥ 阈值才命中 */
    @Value("${kb.cache.semantic-threshold:0.95}")
    private double similarityThreshold;

    /** 语义缓存有效期（小时） */
    @Value("${kb.cache.semantic-ttl-hours:24}")
    private long ttlHours;

    /** 语义缓存独立 collection（与文档块 collection 隔离，便于独立淘汰） */
    @Value("${kb.cache.semantic-collection:qa_semantic_cache}")
    private String collectionName;

    /** 与文档向量同一 embedding 模型，维度一致 */
    @Value("${qdrant.vector-size:1024}")
    private int vectorSize;

    /**
     * 语义缓存命中结果：答案 + 引用快照。
     */
    public record SemanticCacheHit(String answer, List<Conversation.CitationRef> citations) {
    }

    @PostConstruct
    void ensureCollection() {
        // 启动期 Qdrant 不可用不能阻止应用启动：缓存是可降级旁路，运行期各方法自带异常保护。
        try {
            if (Boolean.TRUE.equals(qdrantClient.collectionExistsAsync(collectionName).get())) {
                log.info("Qdrant 语义缓存 collection '{}' 已存在", collectionName);
                return;
            }
            qdrantClient.createCollectionAsync(collectionName,
                    VectorParams.newBuilder().setSize(vectorSize).setDistance(Distance.Dot).build()).get();
            log.info("Qdrant 语义缓存 collection '{}' 就绪 (dim={}, threshold={}, ttl={}h)",
                    collectionName, vectorSize, similarityThreshold, ttlHours);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("创建语义缓存 collection 被中断: {}", collectionName, e);
        } catch (Exception e) {
            // 多实例并发启动可能重复创建，已存在视为成功，其余异常降级关闭
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                log.info("Qdrant 语义缓存 collection '{}' 被并发创建", collectionName);
            } else {
                log.error("Qdrant 语义缓存 collection 初始化失败，语义缓存降级关闭", e);
            }
        }
    }

    private List<Float> toFloatList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float x : array) {
            list.add(x);
        }
        return list;
    }

    /**
     * 查找语义缓存。
     *
     * @param question 改写后的用户问题
     * @return top1 相似度 ≥ 阈值且未过期的缓存；未命中/任何异常均返回 null（降级走正常 RAG）
     */
    public SemanticCacheHit lookup(String question) {
        if (!cacheEnabled || question == null || question.isBlank()) {
            return null;
        }
        try {
            float[] queryVec = embeddingService.embed(question);

            // 只在未过期点中检索：cached_at >= 截止时间
            long cutoff = System.currentTimeMillis() - ttlHours * 3600_000L;
            Filter freshnessFilter = Filter.newBuilder()
                    .addMust(Condition.newBuilder()
                            .setField(FieldCondition.newBuilder()
                                    .setKey(FIELD_CACHE_TYPE)
                                    .setMatch(Match.newBuilder().setKeyword(CACHE_TYPE))
                                    .build())
                            .build())
                    .addMust(Condition.newBuilder()
                            .setField(FieldCondition.newBuilder()
                                    .setKey(FIELD_CACHED_AT)
                                    .setRange(Points.Range.newBuilder().setGte(cutoff).build())
                                    .build())
                            .build())
                    .build();

            List<ScoredPoint> results = qdrantClient.searchAsync(
                    SearchPoints.newBuilder()
                            .setCollectionName(collectionName)
                            .addAllVector(toFloatList(queryVec))
                            .setLimit(1)
                            .setScoreThreshold((float) similarityThreshold)
                            .setFilter(freshnessFilter)
                            .setWithPayload(enable(true))
                            .build()
            ).get();
            if (results.isEmpty()) {
                log.debug("语义缓存 MISS: [{}]", question);
                return null;
            }
            ScoredPoint hit = results.get(0);
            Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload = hit.getPayloadMap();
            String answer = payload.containsKey(FIELD_ANSWER)
                    ? payload.get(FIELD_ANSWER).getStringValue() : null;
            if (answer == null || answer.isBlank()) {
                return null;
            }
            List<Conversation.CitationRef> citations = deserializeCitations(
                    payload.get(FIELD_CITATIONS));
            log.info("语义缓存 HIT (score={}): [{}]", String.format("%.3f", hit.getScore()), question);
            return new SemanticCacheHit(answer, citations);
        } catch (Exception e) {
            // embedding 熔断/Qdrant 故障一律降级为未命中，绝不影响问答主链路
            log.warn("语义缓存查询失败，降级未命中: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 写入语义缓存（同问题幂等覆盖）。
     */
    public void store(String question, String answer, List<Conversation.CitationRef> citations) {
        if (!cacheEnabled || question == null || question.isBlank()
                || answer == null || answer.isBlank()) {
            return;
        }
        try {
            float[] vec = embeddingService.embed(question);
            UUID pointId = deterministicPointId(question);

            PointStruct.Builder point = PointStruct.newBuilder()
                    .setId(id(pointId))
                    .setVectors(vectors(vec))
                    .putPayload(FIELD_CACHE_TYPE, value(CACHE_TYPE))
                    .putPayload(FIELD_QUESTION, value(question))
                    .putPayload(FIELD_ANSWER, value(answer))
                    .putPayload(FIELD_CACHED_AT, value(System.currentTimeMillis()));
            String citationsJson = objectMapper.writeValueAsString(
                    citations == null ? List.of() : citations);
            point.putPayload(FIELD_CITATIONS, value(citationsJson));

            qdrantClient.upsertAsync(
                    UpsertPoints.newBuilder()
                            .setCollectionName(collectionName)
                            .setWait(true)
                            .addPoints(point.build())
                            .build()
            ).get();
        } catch (Exception e) {
            log.warn("语义缓存写入失败（不影响主链路）: {}", e.getMessage());
        }
    }

    /**
     * 清空全部语义缓存（按 cache_type 过滤删除，不影响同库其他 collection）。
     * <p>
     * 预约变更与文档增删改后联动调用。
     */
    public void evictAll() {
        if (!cacheEnabled) {
            return;
        }
        try {
            qdrantClient.deleteAsync(
                    DeletePoints.newBuilder()
                            .setCollectionName(collectionName)
                            .setPoints(PointsSelector.newBuilder()
                                    .setFilter(Filter.newBuilder()
                                            .addMust(Condition.newBuilder()
                                                    .setField(FieldCondition.newBuilder()
                                                            .setKey(FIELD_CACHE_TYPE)
                                                            .setMatch(Match.newBuilder()
                                                                    .setKeyword(CACHE_TYPE))
                                                            .build())
                                                    .build())
                                            .build())
                                    .build())
                            .setWait(true)
                            .build()
            ).get();
            log.info("联动失效：已清空 Qdrant 语义问答缓存 collection={}", collectionName);
        } catch (Exception e) {
            // 淘汰失败只依赖 TTL 兜底，不能让事件消费失败
            log.warn("清空语义缓存失败，将依赖 TTL {}h 自然过期: {}", ttlHours, e.getMessage());
        }
    }

    // ==================== helpers ====================

    /**
     * 由问题内容确定性生成点位 ID（SHA-256 → UUIDv3）：
     * 同一问题永远覆盖同一点位（幂等、无冗余），不同问题 256 位空间不碰撞。
     */
    private UUID deterministicPointId(String question) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return UUID.nameUUIDFromBytes(digest.digest(question.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            // SHA-256 是 JDK 必备算法，理论上不会缺失；兜底随机 ID（失去幂等覆盖但不阻断）
            return UUID.randomUUID();
        }
    }

    private List<Conversation.CitationRef> deserializeCitations(
            io.qdrant.client.grpc.JsonWithInt.Value value) {
        if (value == null || !value.hasStringValue()) {
            return List.of();
        }
        try {
            List<Conversation.CitationRef> refs = objectMapper.readValue(
                    value.getStringValue(), new TypeReference<List<Conversation.CitationRef>>() {
                    });
            return refs == null ? List.of() : refs;
        } catch (Exception e) {
            log.debug("语义缓存引用反序列化失败，按无引用处理: {}", e.getMessage());
            return List.of();
        }
    }
}
