package com.kb.infrastructure.rag.chunker;

import com.kb.domain.document.DocumentChunk;
import com.kb.domain.rag.ChunkStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sliding window chunking strategy.
 * <p>
 * Splits text by paragraphs first, then groups paragraphs into chunks
 * up to {@code windowSize} characters. Each chunk overlaps with the
 * previous one by retaining {@code overlap} characters at the end.
 * This produces more semantically coherent chunks than pure fixed-size.
 * </p>
 *
 * @author forever-king
 */
@Component
public class SlidingWindowChunker implements ChunkStrategy {

    /** 滑动窗口大小（字符数） */
    @Value("${chunking.default-size}")
    private int windowSize;

    /** 相邻窗口之间的重叠字符数 */
    @Value("${chunking.default-overlap}")
    private int overlap;

    @Override
    public List<DocumentChunk> chunk(String text, Map<String, Object> metadata) {
        List<DocumentChunk> chunks = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return chunks;
        }

        // Split by paragraphs
        String[] paragraphs = text.split("\n\n+");
        StringBuilder buffer = new StringBuilder();
        int index = 0;

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) continue;

            // If adding this paragraph exceeds the window, emit the current buffer
            if (buffer.length() > 0 && buffer.length() + trimmed.length() > windowSize) {
                chunks.add(buildChunk(buffer.toString(), index++, metadata));

                // Carry over the tail of the previous chunk for context continuity
                String prevContent = buffer.toString();
                if (prevContent.length() > overlap) {
                    String tail = prevContent.substring(prevContent.length() - overlap);
                    // Start from a clean boundary
                    int cleanStart = findSentenceStart(tail);
                    buffer = new StringBuilder(tail.substring(cleanStart) + "\n\n" + trimmed);
                } else {
                    buffer = new StringBuilder(trimmed);
                }
            } else {
                if (buffer.length() > 0) buffer.append("\n\n");
                buffer.append(trimmed);
            }
        }

        // Don't forget the last chunk
        if (buffer.length() > 0) {
            chunks.add(buildChunk(buffer.toString(), index, metadata));
        }

        return chunks;
    }

    private int findSentenceStart(String text) {
        // Try to start from a sentence boundary for cleaner context
        int period = text.indexOf("。");
        int newline = text.indexOf("\n");
        int start = 0;
        if (period > 0) start = period + 1;
        if (newline > 0 && newline < (period > 0 ? period : Integer.MAX_VALUE)) {
            start = newline + 1;
        }
        return Math.min(start, text.length());
    }

    private DocumentChunk buildChunk(String text, int index, Map<String, Object> metadata) {
        Map<String, Object> chunkMeta = new HashMap<>(metadata != null ? metadata : Map.of());
        return DocumentChunk.builder()
                .content(text.trim())
                .chunkIndex(index)
                .tokenCount(estimateTokens(text))
                .metadata(chunkMeta)
                .build();
    }

    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return (int) (text.length() * 0.5);
    }
}
