package com.kb.domain.rag;

import java.util.List;

/**
 * Domain interface for text-to-vector embedding.
 * <p>
 * Implementations can use OpenAI Embedding API, local BGE models,
 * or any other embedding provider.
 * </p>
 * @author forever-king
 */
public interface EmbeddingService {

    /**
     * Convert a single text to a float vector.
     *
     * @param text the text to embed
     * @return float array (dimension depends on the model, e.g., 1024 for bge-large-zh)
     */
    float[] embed(String text);

    /**
     * Batch-convert multiple texts to vectors.
     * <p>Implementations should optimize for batch API calls where possible.</p>
     *
     * @param texts list of texts to embed
     * @return list of float arrays, same order as input
     */
    List<float[]> embedBatch(List<String> texts);
}
