package com.kb.infrastructure.persistence.qdrant;

import com.kb.domain.rag.VectorStoreService;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.Distance;
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

    /** Qdrant gRPC 公共样板（collection 创建/payload 转换，Q-05） */
    private final QdrantSupport qdrantSupport;

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
        // Q-05：collection 幂等创建收敛到 QdrantSupport；保持历史容错语义（failFast=false）
        qdrantSupport.ensureCollection(collectionName, vectorSize, distanceType,
                Distance.Euclid, false);
    }

    @Override
    public void upsert(List<VectorPoint> points) {
        if (points == null || points.isEmpty()) return;

        List<PointStruct> qdrantPoints = new ArrayList<>();
        for (VectorPoint p : points) {
            // Convert Map<String, Object> to Map<String, Value>
            Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload = new HashMap<>();
            for (Map.Entry<String, Object> entry : p.payload().entrySet()) {
                payload.put(entry.getKey(), qdrantSupport.toValue(entry.getValue()));
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
                            .addAllVector(qdrantSupport.toFloatList(queryVector))
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
                    plainPayload.put(entry.getKey(), qdrantSupport.fromValue(entry.getValue()));
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

}

