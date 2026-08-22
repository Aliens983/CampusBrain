package com.kb.infrastructure.common;

import com.kb.domain.document.Document;

import java.util.Map;

/**
 * 文档领域对象 → DTO 转换器。
 * @author forever-king
 */
public final class DocumentAssembler {

    private DocumentAssembler() {}

    public static Map<String, Object> toDTO(Document doc) {
        return Map.of(
                "id", doc.getId(),
                "title", doc.getTitle(),
                "fileType", doc.getFileType(),
                "fileSize", doc.getFileSize(),
                "status", doc.getStatus().name(),
                "chunkCount", doc.getChunkCount(),
                "createdAt", doc.getCreatedAt()
        );
    }
}
