package com.kb.domain.document;

import java.util.List;

/**
 * Domain service for document-related business logic
 * that doesn't naturally belong to the Document aggregate.
 * @author forever-king
 */
public class DocumentDomainService {

    /**
     * Validate that the file type is supported.
     */
    public boolean isSupportedFileType(String fileType) {
        if (fileType == null) {
            return false;
        }
        String lower = fileType.toLowerCase();
        return "pdf".equals(lower) || "md".equals(lower)
                || "markdown".equals(lower) || "txt".equals(lower)
                || "docx".equals(lower) || "html".equals(lower)
                || "htm".equals(lower);
    }

    /**
     * Get the file type extension from a filename.
     */
    public String extractFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "unknown";
        }
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        // 尾部点号（如 "file."）没有实际扩展名，同样返回 unknown
        return ext.isEmpty() ? "unknown" : ext;
    }

    /**
     * Validate that a chunk list is non-empty and has correct indices.
     */
    public void validateChunks(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalArgumentException("Chunk list must not be empty");
        }
        for (int i = 0; i < chunks.size(); i++) {
            if (chunks.get(i).getChunkIndex() != i) {
                throw new IllegalArgumentException(
                        "Chunk index mismatch at position " + i);
            }
        }
    }
}
