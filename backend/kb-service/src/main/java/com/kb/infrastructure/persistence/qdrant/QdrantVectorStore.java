package com.kb.infrastructure.persistence.qdrant;

import com.kb.domain.rag.VectorStoreService;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Points;
import io.qdrant.client.grpc.Points.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;
import static io.qdrant.client.WithPayloadSelectorFactory.enable;

/**
 * Qdrant vector store implementation.
 * Uses the Qdrant Java client gRPC API.
 *
 * @author forever-king
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QdrantVectorStore implements VectorStoreService {

    /** Qdrant客户端实例，用于与Qdrant向量数据库通信 */
    private final QdrantClient qdrantClient;

    /** Qdrant集合名称 */
    @Value("${qdrant.collection-name}")
    private String collectionName;

    /** 向量维度大小 */
    @Value("${qdrant.vector-size}")
    private int vectorSize;

    /** 距离计算类型（Cosine/Euclid） */
    @Value("${qdrant.distance}")
    private String distanceType;

    @Override
    @PostConstruct
    public void ensureCollection() {
        try {
            boolean exists = qdrantClient.collectionExistsAsync(collectionName).get();
            if (!exists) {
                Distance distance = "Cosine".equalsIgnoreCase(distanceType)
                        ? Distance.Cosine : Distance.Euclid;

                qdrantClient.createCollectionAsync(
                        collectionName,
                        VectorParams.newBuilder()
                                .setSize(vectorSize)
                                .setDistance(distance)
                                .build()
                ).get();
                log.info("Qdrant collection '{}' created (dim={}, dist={})",
                        collectionName, vectorSize, distanceType);
            } else {
                log.info("Qdrant collection '{}' already exists", collectionName);
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to ensure Qdrant collection '{}'", collectionName, e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void upsert(List<VectorPoint> points) {
        if (points == null || points.isEmpty()) return;

        List<PointStruct> qdrantPoints = new ArrayList<>();
        for (VectorPoint p : points) {
            // Convert Map<String, Object> to Map<String, Value>
            Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload = new HashMap<>();
            for (Map.Entry<String, Object> entry : p.payload().entrySet()) {
                payload.put(entry.getKey(), convertToValue(entry.getValue()));
            }

            PointStruct point = PointStruct.newBuilder()
                    .setId(id(UUID.fromString(p.id())))
                    .setVectors(vectors(p.vector()))
                    .putAllPayload(payload)
                    .build();
            qdrantPoints.add(point);
        }

        try {
            qdrantClient.upsertAsync(
                    UpsertPoints.newBuilder()
                            .setCollectionName(collectionName)
                            .setWait(true)
                            .addAllPoints(qdrantPoints)
                            .build()
            ).get();
            log.debug("Upserted {} vectors to Qdrant", points.size());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Qdrant upsert failed", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Qdrant upsert failed", e);
        }
    }

    @Override
    public List<ScoredVector> search(float[] queryVector, int limit, double scoreThreshold) {
        try {
            List<ScoredPoint> results = qdrantClient.searchAsync(
                    SearchPoints.newBuilder()
                            .setCollectionName(collectionName)
                            .addAllVector(floatToList(queryVector))
                            .setLimit(limit)
                            .setScoreThreshold((float) scoreThreshold)
                            .setWithPayload(enable(true))
                            .build()
            ).get();

            List<ScoredVector> svList = new ArrayList<>();
            for (ScoredPoint sp : results) {
                // Convert protobuf payload values back to plain Map<String, Object>
                Map<String, Object> plainPayload = new HashMap<>();
                for (Map.Entry<String, io.qdrant.client.grpc.JsonWithInt.Value> entry :
                        sp.getPayloadMap().entrySet()) {
                    plainPayload.put(entry.getKey(), convertFromValue(entry.getValue()));
                }

                svList.add(new ScoredVector(
                        sp.getId().getUuid(),
                        sp.getScore(),
                        plainPayload
                ));
            }
            return svList;

        } catch (InterruptedException | ExecutionException e) {
            log.error("Qdrant search failed", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Qdrant search failed", e);
        }
    }

    @Override
    public void delete(List<String> pointIds) {
        if (pointIds == null || pointIds.isEmpty()) return;
        try {
            List<Points.PointId> ids = pointIds.stream()
                    .map(uid -> id(UUID.fromString(uid)))
                    .toList();

            qdrantClient.deleteAsync(
                    DeletePoints.newBuilder()
                            .setCollectionName(collectionName)
                            .setPoints(PointsSelector.newBuilder()
                                    .setPoints(PointsIdsList.newBuilder()
                                            .addAllIds(ids)
                                            .build())
                                    .build())
                            .setWait(true)
                            .build()
            ).get();
            log.debug("Deleted {} vectors from Qdrant", pointIds.size());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Qdrant delete failed", e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void deleteByDocumentId(String documentId) {
        try {
            qdrantClient.deleteAsync(
                    DeletePoints.newBuilder()
                            .setCollectionName(collectionName)
                            .setPoints(PointsSelector.newBuilder()
                                    .setFilter(Filter.newBuilder()
                                            .addMust(Condition.newBuilder()
                                                    .setField(FieldCondition.newBuilder()
                                                            .setKey("document_id")
                                                            .setMatch(Match.newBuilder()
                                                                    .setKeyword(documentId)
                                                                    .build())
                                                            .build())
                                                    .build())
                                            .build())
                                    .build())
                            .setWait(true)
                            .build()
            ).get();
            log.debug("Deleted vectors for document {} from Qdrant", documentId);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Qdrant deleteByDocumentId failed", e);
            Thread.currentThread().interrupt();
        }
    }

    // ========== Value Converters ==========

    @SuppressWarnings("unchecked")
    private io.qdrant.client.grpc.JsonWithInt.Value convertToValue(Object obj) {
        if (obj == null) {
            return value((String) null);
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
        if (obj instanceof Map) {
            // For simplicity, serialize complex objects to string
            return value(obj.toString());
        }
        return value(obj.toString());
    }

    private Object convertFromValue(io.qdrant.client.grpc.JsonWithInt.Value value) {
        if (value == null) return null;
        if (value.hasStringValue()) return value.getStringValue();
        if (value.hasIntegerValue()) return value.getIntegerValue();
        if (value.hasDoubleValue()) return value.getDoubleValue();
        if (value.hasBoolValue()) return value.getBoolValue();
        return value.toString();
    }

    private List<Float> floatToList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float f : array) {
            list.add(f);
        }
        return list;
    }
}
