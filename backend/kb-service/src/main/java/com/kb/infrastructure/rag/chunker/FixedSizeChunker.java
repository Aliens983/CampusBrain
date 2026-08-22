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
 * Fixed-size chunking strategy with intelligent break-point detection.
 * <p>
 * Splits text into chunks of approximately {@code chunkSize} characters,
 * with {@code overlap} characters of context shared between adjacent chunks.
 * Tries to break at natural boundaries (sentence endings, line breaks)
 * rather than mid-word.
 * </p>
 *
 * @author forever-king
 */
@Component
public class FixedSizeChunker implements ChunkStrategy {

    /** 分块大小（字符数） */
    @Value("${chunking.default-size}")
    private int chunkSize;

    /** 相邻块之间的重叠字符数 */
    @Value("${chunking.default-overlap}")
    private int overlap;

    @Override
    public List<DocumentChunk> chunk(String text, Map<String, Object> metadata) {
        List<DocumentChunk> chunks = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return chunks;
        }

        int start = 0;
        int index = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            // If we're not at the end, try to find a natural break point
            if (end < text.length()) {
                end = findBestBreakPoint(text, end);
            }

            String chunkText = text.substring(start, end).trim();
            if (!chunkText.isEmpty()) {
                Map<String, Object> chunkMeta = new HashMap<>(metadata != null ? metadata : Map.of());
                chunkMeta.put("chunkStart", start);
                chunkMeta.put("chunkEnd", end);

                chunks.add(DocumentChunk.builder()
                        .content(chunkText)
                        .chunkIndex(index++)
                        .tokenCount(estimateTokens(chunkText))
                        .metadata(chunkMeta)
                        .build());
            }

            // 已消费到文本末尾：直接结束，避免重叠逻辑导致 start 原地踏步而无限循环
            if (end >= text.length()) {
                break;
            }

            // 下一块从 (end - overlap) 开始；若未前进则强制跳到 end，保证每次循环都前进
            int nextStart = end - overlap;
            if (nextStart <= start) {
                nextStart = end;
            }
            start = nextStart;
        }

        return chunks;
    }

    /**
     * Search near the ideal end position for the best natural break point.
     * Priority: paragraph break > line break > sentence end > comma > space.
     */
    private int findBestBreakPoint(String text, int idealEnd) {
        int searchStart = Math.max(0, idealEnd - 100);
        int searchEnd = Math.min(text.length(), idealEnd + 50);
        String window = text.substring(searchStart, searchEnd);

        // Priority 1: Double line break (paragraph)
        int best = window.lastIndexOf("\n\n");
        if (best >= 20) return searchStart + best + 2;

        // Priority 2: Single line break
        best = window.lastIndexOf("\n");
        if (best >= 20) return searchStart + best + 1;

        // Priority 3: Chinese period
        best = window.lastIndexOf("。");
        if (best >= 20) return searchStart + best + 1;

        // Priority 4: English period + space
        best = window.lastIndexOf(". ");
        if (best >= 20) return searchStart + best + 2;

        // Priority 5: Comma
        best = window.lastIndexOf("，");
        if (best >= 20) return searchStart + best + 1;

        // Fallback: use the ideal end
        return idealEnd;
    }

    /**
     * Rough token estimation.
     * Chinese characters ~0.5 tokens each, English words ~1.3 tokens each.
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        // Simplified: count Chinese chars + English word count
        int chineseChars = 0;
        int englishWords = 0;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                chineseChars++;
            }
        }
        englishWords = text.split("\\s+").length;
        return (int) (chineseChars * 0.5 + englishWords * 1.3);
    }
}
