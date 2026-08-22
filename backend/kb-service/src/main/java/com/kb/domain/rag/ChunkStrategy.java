package com.kb.domain.rag;

import com.kb.domain.document.DocumentChunk;

import java.util.List;
import java.util.Map;

/**
 * Domain interface for document chunking strategies.
 * <p>
 * Different strategies can be plugged in:
 * <ul>
 *   <li>Fixed-size chunking: split by character count</li>
 *   <li>Sliding window: overlap between adjacent chunks</li>
 *   <li>Semantic chunking: split by paragraph/section boundaries</li>
 * </ul>
 * </p>
 * @author forever-king
 */
public interface ChunkStrategy {

    /**
     * Split raw text into chunks.
     *
     * @param text     the full document text
     * @param metadata parent document metadata to attach to each chunk
     * @return ordered list of chunks
     */
    List<DocumentChunk> chunk(String text, Map<String, Object> metadata);
}
