package com.kb.infrastructure.rag.chunker;

import com.kb.domain.rag.ChunkStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Factory for selecting chunking strategies by name.
 *
 * @author forever-king
 */
@Component
@RequiredArgsConstructor
public class ChunkerFactory {

    /** 固定大小分块策略 */
    private final FixedSizeChunker fixedSizeChunker;
    /** 滑动窗口分块策略 */
    private final SlidingWindowChunker slidingWindowChunker;

    /**
     * Get a chunking strategy by name.
     *
     * @param name "fixed_size" | "sliding_window"
     * @return the corresponding ChunkStrategy
     */
    public ChunkStrategy getStrategy(String name) {
        return switch (name != null ? name.toLowerCase() : "") {
            case "fixed_size" -> fixedSizeChunker;
            case "sliding_window" -> slidingWindowChunker;
            default -> slidingWindowChunker;
        };
    }
}
