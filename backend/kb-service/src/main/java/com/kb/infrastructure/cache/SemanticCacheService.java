package com.kb.infrastructure.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.domain.conversation.Conversation;
import com.kb.domain.rag.EmbeddingService;
import com.kb.infrastructure.persistence.qdrant.QdrantSupport;
import com.kb.infrastructure.rag.llm.NoAnswerMarkers;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
 *   <li>点位 ID 由问题经 SHA-256 预哈希后派生（1.7：底层的 {@code UUID.nameUUIDFromBytes}
 *       实为 <b>MD5（128 位）</b>），同问题幂等覆盖、碰撞概率可忽略；</li>
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

    /**
     * 3.8（深度审查 P1）归属维度：记录生成该条缓存的用户。
     * <ul>
     *   <li>有归属（owner_id 存在）→ 仅该用户可见（其答案可能引用了私有文档）；</li>
     *   <li>无归属（字段缺失，未登录/服务内生成）→ 全量共享，与 4.1"无 owner=全局共享"口径一致。</li>
     * </ul>
     */
    private static final String FIELD_OWNER_ID = "owner_id";
    /** 过期清理时的时间余量（毫秒），避免边界点的缓存刚写入即被清扫 */
    private static final long CLEANUP_TTL_MARGIN_MS = 3600_000L;

    private final QdrantClient qdrantClient;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;
    /** Qdrant gRPC 公共样板（collection 创建/向量转换，Q-05） */
    private final QdrantSupport qdrantSupport;

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
        // Q-05：collection 幂等创建收敛到 QdrantSupport。
        // 启动期 Qdrant 不可用不能阻止应用启动：缓存是可降级旁路（failFast=false），运行期各方法自带异常保护。
        boolean ready = qdrantSupport.ensureCollection(collectionName, vectorSize, "Cosine",
                Distance.Dot, false);
        if (ready) {
            log.info("Qdrant 语义缓存 collection '{}' 就绪 (dim={}, threshold={}, ttl={}h)",
                    collectionName, vectorSize, similarityThreshold, ttlHours);
        }
    }

    /**
     * 查找语义缓存。
     *
     * @param question 改写后的用户问题
     * @param userId   当前登录用户（3.8 归属过滤；null=未登录，仅可见全局共享条目）
     * @return top1 相似度 ≥ 阈值、未过期且对当前用户可见的缓存；未命中/任何异常均返回 null（降级走正常 RAG）
     */
    public SemanticCacheHit lookup(String question, Long userId) {
        if (!cacheEnabled || question == null || question.isBlank()) {
            return null;
        }
        try {
            float[] queryVec = embeddingService.embed(question);

            // 只在未过期且对当前用户可见的点中检索：cached_at >= 截止时间，
            // 归属 = 本人（owner_id == userId）或全局共享（owner_id 缺失）
            long cutoff = System.currentTimeMillis() - ttlHours * 3600_000L;
            Filter.Builder visibleFilter = Filter.newBuilder()
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
                            .build());
            visibleFilter.addMust(ownerVisibilityCondition(userId));

            List<ScoredPoint> results = qdrantClient.searchAsync(
                    SearchPoints.newBuilder()
                            .setCollectionName(collectionName)
                            .addAllVector(qdrantSupport.toFloatList(queryVec))
                            .setLimit(1)
                            .setScoreThreshold((float) similarityThreshold)
                            .setFilter(visibleFilter.build())
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
     *
     * @param ownerId 生成者用户 ID（3.8）；null=未登录/服务内生成 → 不落 owner_id，视为全局共享
     */
    public void store(String question, String answer, List<Conversation.CitationRef> citations, Long ownerId) {
        if (!cacheEnabled || question == null || question.isBlank()
                || answer == null || answer.isBlank()) {
            return;
        }
        // 4.19（深度审查 P2）："无法回答/拒绝回答/注入套话"类内容不写入全局语义缓存，
        // 避免一次 prompt 注入的响应被固化放大为 TTL 内全体用户共享的答案。
        if (NoAnswerMarkers.looksLikeNoAnswer(answer)) {
            log.debug("跳过语义缓存写入（无法回答/拒绝回答类内容）: [{}]", question);
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
            if (ownerId != null) {
                // 仅登录用户写入时记归属；全局共享条目不写该字段，lookup 按 isNull 放行
                point.putPayload(FIELD_OWNER_ID, value(ownerId));
            }
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

    /**
     * 3.8（深度审查 P1）归属可见性条件：owner_id == 当前用户 或 owner_id 缺失（全局共享）。
     * 未登录（userId==null）仅放行全局共享条目。
     */
    private Condition ownerVisibilityCondition(Long userId) {
        if (userId == null) {
            return Condition.newBuilder()
                    .setIsNull(Points.IsNullCondition.newBuilder().setKey(FIELD_OWNER_ID).build())
                    .build();
        }
        Filter.Builder visible = Filter.newBuilder()
                .addShould(Condition.newBuilder()
                        .setField(FieldCondition.newBuilder()
                                .setKey(FIELD_OWNER_ID)
                                .setMatch(Match.newBuilder().setInteger(userId))
                                .build())
                        .build())
                .addShould(Condition.newBuilder()
                        .setIsNull(Points.IsNullCondition.newBuilder().setKey(FIELD_OWNER_ID).build())
                        .build());
        return Condition.newBuilder().setFilter(visible.build()).build();
    }

    /**
     * 3.8（深度审查 P1）过期点清理任务：查找阶段只做"懒过滤"（cached_at >= cutoff），
     * 过期点永不被删除，collection 只增不减。本任务按固定延迟删除已过 TTL（外加 1h 余量）
     * 的缓存点，与懒过滤互补。
     */
    @Scheduled(fixedDelayString = "${kb.cache.semantic-cleanup-interval-ms:21600000}",
            initialDelayString = "${kb.cache.semantic-cleanup-init-delay-ms:600000}")
    public void cleanupStaleCacheEntries() {
        if (!cacheEnabled) {
            return;
        }
        try {
            long cutoff = System.currentTimeMillis() - ttlHours * 3600_000L - CLEANUP_TTL_MARGIN_MS;
            Filter staleFilter = Filter.newBuilder()
                    .addMust(Condition.newBuilder()
                            .setField(FieldCondition.newBuilder()
                                    .setKey(FIELD_CACHE_TYPE)
                                    .setMatch(Match.newBuilder().setKeyword(CACHE_TYPE))
                                    .build())
                            .build())
                    .addMust(Condition.newBuilder()
                            .setField(FieldCondition.newBuilder()
                                    .setKey(FIELD_CACHED_AT)
                                    .setRange(Points.Range.newBuilder().setLt(cutoff).build())
                                    .build())
                            .build())
                    .build();
            qdrantClient.deleteAsync(
                    DeletePoints.newBuilder()
                            .setCollectionName(collectionName)
                            .setPoints(PointsSelector.newBuilder().setFilter(staleFilter).build())
                            .setWait(true)
                            .build()
            ).get();
            log.info("语义缓存过期清理完成：删除 cached_at < {} 的点", cutoff);
        } catch (Exception e) {
            // 清理失败不影响主链路，下个周期再试
            log.warn("语义缓存过期清理失败: {}", e.getMessage());
        }
    }

    // ==================== helpers ====================

    /**
     * 由问题内容确定性生成点位 ID。
     * <p>
     * 1.7（深度审查 P2）：修正注释——{@code UUID.nameUUIDFromBytes} 内部为 <b>MD5（128 位）</b>
     * 而非 SHA-256（SHA-256 仅用于把问题字节预哈希进 MD5 输入，最终空间仍是 128 位）。
     * 同一问题恒映射同一 ID（幂等覆盖），不同问题碰撞概率约 2⁻⁶⁴，可忽略。
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
