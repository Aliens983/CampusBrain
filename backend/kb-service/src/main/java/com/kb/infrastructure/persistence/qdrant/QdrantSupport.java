package com.kb.infrastructure.persistence.qdrant;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.JsonWithInt.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static io.qdrant.client.ValueFactory.value;

/**
 * Qdrant gRPC 公共样板（Q-05 收敛）。
 * <p>
 * 此前 {@link QdrantVectorStore} 与 {@code SemanticCacheService} 各自维护
 * payload 值转换、{@code float[] → List<Float>}、collection 幂等创建的同构实现，
 * 口径漂移风险高。现统一在此。
 *
 * @author forever-king
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QdrantSupport {

    private final QdrantClient qdrantClient;

    /**
     * 幂等创建 collection：已存在则跳过。
     * <p>
     * 1.7（深度审查 P2）：修正注释与实现不符——当前两个调用方
     * （文档向量库 {@code QdrantVectorStore}、语义缓存 {@code SemanticCacheService}）
     * <b>均显式使用 Cosine</b>（语义相似度 0.95 阈值按余弦语义）；{@code nonCosineFallback}
     * 仅在调用方传入非 "Cosine" 度量名时生效，属预留扩展位而非既有行为。
     *
     * @param failFast true：创建失败抛 IllegalStateException（语义缓存启动即必须可用）；
     *                 false：仅记日志（向量库保持历史容错语义，由后续调用重试暴露）
     */
    public boolean ensureCollection(String collectionName, int vectorSize,
                                    String distanceType, Distance nonCosineFallback,
                                    boolean failFast) {
        try {
            boolean exists = qdrantClient.collectionExistsAsync(collectionName).get();
            if (exists) {
                log.info("Qdrant collection '{}' already exists", collectionName);
                return true;
            }
            Distance distance = "Cosine".equalsIgnoreCase(distanceType)
                    ? Distance.Cosine : nonCosineFallback;
            qdrantClient.createCollectionAsync(
                    collectionName,
                    VectorParams.newBuilder()
                            .setSize(vectorSize)
                            .setDistance(distance)
                            .build()
            ).get();
            log.info("Created Qdrant collection '{}' with {} dimensions (distance={})",
                    collectionName, vectorSize, distance);
            return true;
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            // 多实例并发启动可能重复创建；collection 已存在视为成功，其余按 failFast 决定
            if (alreadyExists(e)) {
                log.info("Qdrant collection '{}' created concurrently", collectionName);
                return true;
            }
            if (failFast) {
                throw new IllegalStateException(
                        "Failed to create Qdrant collection: " + collectionName, e);
            }
            log.error("Failed to create Qdrant collection '{}'", collectionName, e);
            return false;
        }
    }

    /**
     * Java 值 → Qdrant payload Value。
     * Qdrant 的 setStringValue(null) 会抛 NPE，null 统一存为空字符串；
     * 复杂对象退化为 toString。
     */
    public Value toValue(Object obj) {
        if (obj == null) {
            return value("");
        }
        if (obj instanceof String s) {
            return value(s);
        }
        if (obj instanceof Integer i) {
            return value(i.longValue());
        }
        if (obj instanceof Long l) {
            return value(l);
        }
        if (obj instanceof Double d) {
            return value(d);
        }
        if (obj instanceof Float f) {
            return value(f.doubleValue());
        }
        if (obj instanceof Boolean b) {
            return value(b);
        }
        return value(obj.toString());
    }

    /** Qdrant payload Value → Java 值 */
    public Object fromValue(Value v) {
        if (v == null) {
            return null;
        }
        if (v.hasStringValue()) {
            return v.getStringValue();
        }
        if (v.hasIntegerValue()) {
            return v.getIntegerValue();
        }
        if (v.hasDoubleValue()) {
            return v.getDoubleValue();
        }
        if (v.hasBoolValue()) {
            return v.getBoolValue();
        }
        return v.toString();
    }

    /** float[] → Qdrant 向量接口需要的装箱 List */
    public List<Float> toFloatList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float f : array) {
            list.add(f);
        }
        return list;
    }

    private boolean alreadyExists(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().toLowerCase().contains("already exists")) {
                return true;
            }
        }
        return false;
    }
}
