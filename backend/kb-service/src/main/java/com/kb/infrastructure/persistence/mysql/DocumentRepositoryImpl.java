package com.kb.infrastructure.persistence.mysql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.domain.document.Document;
import com.kb.domain.document.DocumentChunk;
import com.kb.domain.document.DocumentRepository;
import com.kb.domain.document.DocumentStatus;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kb.infrastructure.persistence.mysql.dataobject.DocumentChunkDO;
import com.kb.infrastructure.persistence.mysql.dataobject.DocumentDO;
import com.kb.infrastructure.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DocumentRepository implementation backed by MySQL (MyBatis-Plus).
 *
 * @author forever-king
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DocumentRepositoryImpl implements DocumentRepository {

    /** 文档Mapper */
    private final DocumentMapper documentMapper;

    /** 文档分块Mapper */
    private final DocumentChunkMapper chunkMapper;

    /** JSON序列化/反序列化工具 */
    private final ObjectMapper objectMapper;

    // ========== Document ==========

    @Override
    public Document save(Document document) {
        DocumentDO docDO = toDocumentDO(document);
        if (docDO.getId() == null) {
            documentMapper.insert(docDO);
        } else {
            documentMapper.updateById(docDO);
        }
        return toDocument(docDO);
    }

    @Override
    public Optional<Document> findById(Long id) {
        LambdaQueryWrapper<DocumentDO> wrapper = new LambdaQueryWrapper<DocumentDO>()
                .eq(DocumentDO::getId, id);
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            wrapper.eq(DocumentDO::getTenantId, tenantId);
        }
        DocumentDO docDO = documentMapper.selectOne(wrapper);
        return Optional.ofNullable(docDO).map(this::toDocument);
    }

    @Override
    public List<Document> findAll() {
        return documentMapper.selectList(tenantFilter()).stream()
                .map(this::toDocument)
                .toList();
    }

    /** 构建租户过滤条件（如果当前设置了租户上下文） */
    private LambdaQueryWrapper<DocumentDO> tenantFilter() {
        LambdaQueryWrapper<DocumentDO> wrapper = new LambdaQueryWrapper<>();
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            wrapper.eq(DocumentDO::getTenantId, tenantId);
        }
        return wrapper;
    }

    @Override
    public List<Document> findAll(int page, int size) {
        var pageQuery = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<DocumentDO>(page + 1, size);
        var result = documentMapper.selectPage(pageQuery, tenantFilter());
        return result.getRecords().stream().map(this::toDocument).toList();
    }

    @Override
    public List<Document> findByStatus(DocumentStatus status) {
        return documentMapper.selectByStatus(status.name()).stream()
                .map(this::toDocument)
                .toList();
    }

    @Override
    public void updateStatus(Long id, DocumentStatus status, String errorMsg) {
        documentMapper.updateStatus(id, status.name(), errorMsg);
    }

    @Override
    public void markReady(Long id, int chunkCount) {
        documentMapper.markReady(id, chunkCount);
    }

    @Override
    public void delete(Long id) {
        LambdaQueryWrapper<DocumentDO> wrapper = new LambdaQueryWrapper<DocumentDO>()
                .eq(DocumentDO::getId, id);
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            wrapper.eq(DocumentDO::getTenantId, tenantId);
        }
        int deleted = documentMapper.delete(wrapper);
        if (deleted == 0) {
            return;
        }
        chunkMapper.deleteByDocumentId(id);
    }

    @Override
    public long count() {
        return documentMapper.selectCount(tenantFilter());
    }

    @Override
    public long countByStatus(DocumentStatus status) {
        LambdaQueryWrapper<DocumentDO> wrapper = tenantFilter()
                .eq(DocumentDO::getStatus, status.name());
        return documentMapper.selectCount(wrapper);
    }

    // ========== Chunks ==========

    @Override
    public void saveChunks(List<DocumentChunk> chunks, Long documentId) {
        for (DocumentChunk chunk : chunks) {
            DocumentChunkDO chunkDO = new DocumentChunkDO();
            chunkDO.setDocumentId(documentId);
            chunkDO.setChunkIndex(chunk.getChunkIndex());
            chunkDO.setContent(chunk.getContent());
            chunkDO.setChunkHash(computeHash(chunk.getContent()));
            chunkDO.setTokenCount(chunk.getTokenCount());
            chunkDO.setMetadataJson(toJson(chunk.getMetadata()));
            chunkDO.setQdrantId(chunk.getQdrantId());
            chunkMapper.insert(chunkDO);
        }
    }

    @Override
    public List<DocumentChunk> findChunksByDocumentId(Long documentId) {
        return chunkMapper.selectByDocumentId(documentId).stream()
                .map(this::toChunk)
                .toList();
    }

    // ========== Conversion ==========

    private Document toDocument(DocumentDO docDO) {
        return Document.builder()
                .id(docDO.getId())
                .title(docDO.getTitle())
                .fileType(docDO.getFileType())
                .fileSize(docDO.getFileSize())
                .filePath(docDO.getFilePath())
                .status(DocumentStatus.valueOf(docDO.getStatus()))
                .chunkCount(docDO.getChunkCount())
                .ownerId(docDO.getOwnerId())
                .tenantId(docDO.getTenantId())
                .metadata(parseJsonMap(docDO.getMetadataJson()))
                .errorMsg(docDO.getErrorMsg())
                .createdAt(docDO.getCreatedAt())
                .updatedAt(docDO.getUpdatedAt())
                .build();
    }

    private DocumentDO toDocumentDO(Document doc) {
        DocumentDO docDO = new DocumentDO();
        docDO.setId(doc.getId());
        docDO.setTitle(doc.getTitle());
        docDO.setFileType(doc.getFileType());
        docDO.setFileSize(doc.getFileSize());
        docDO.setFilePath(doc.getFilePath());
        docDO.setStatus(doc.getStatus().name());
        docDO.setChunkCount(doc.getChunkCount());
        docDO.setOwnerId(doc.getOwnerId());
        docDO.setTenantId(doc.getTenantId());
        docDO.setMetadataJson(toJson(doc.getMetadata()));
        docDO.setErrorMsg(doc.getErrorMsg());
        return docDO;
    }

    private DocumentChunk toChunk(DocumentChunkDO chunkDO) {
        return DocumentChunk.builder()
                .id(chunkDO.getId())
                .documentId(chunkDO.getDocumentId())
                .chunkIndex(chunkDO.getChunkIndex())
                .content(chunkDO.getContent())
                .chunkHash(chunkDO.getChunkHash())
                .tokenCount(chunkDO.getTokenCount())
                .metadata(parseJsonMap(chunkDO.getMetadataJson()))
                .qdrantId(chunkDO.getQdrantId())
                .createdAt(chunkDO.getCreatedAt())
                .build();
    }

    // ========== Utilities ==========

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize to JSON", e);
            return null;
        }
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse JSON: {}", json, e);
            return null;
        }
    }

    static String computeHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
