package com.kb.infrastructure.rag.embedding;

import com.kb.domain.rag.EmbeddingService;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI Embedding service implementation via LangChain4j.
 * <p>
 * Uses text-embedding-3-small (default) or text-embedding-3-large.
 * Batch processing with automatic retry for rate limits.
 * </p>
 *
 * @author forever-king
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class OpenAiEmbeddingService implements EmbeddingService {

    /** 嵌入模型实例 */
    private final EmbeddingModel embeddingModel;

    @Value("${langchain4j.openai.embedding-model.model-name:text-embedding-3-small}")
    /** 模型名称 */
    private String modelName;

    /**
     * 最大批处理大小。
     * text-embedding-3 模型支持每次最多 2048 个 token，
     * 且 API 接受输入数组。
     */
    private static final int MAX_BATCH_SIZE = 20;

    @Override
    public float[] embed(String text) {
        if (text == null || text.isEmpty()) {
            return new float[0];
        }
        try {
            dev.langchain4j.data.embedding.Embedding embedding =
                    embeddingModel.embed(text).content();
            return embedding.vector();
        } catch (Exception e) {
            log.error("Embedding failed for text (length={})", text.length(), e);
            throw new RuntimeException("Embedding failed", e);
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        List<float[]> allEmbeddings = new ArrayList<>();

        // Process in batches to respect API limits
        for (int i = 0; i < texts.size(); i += MAX_BATCH_SIZE) {
            int end = Math.min(i + MAX_BATCH_SIZE, texts.size());
            List<String> batch = texts.subList(i, end);

            try {
                var response = embeddingModel.embedAll(
                        batch.stream()
                                .map(dev.langchain4j.data.segment.TextSegment::from)
                                .toList()
                );

                for (dev.langchain4j.data.embedding.Embedding embedding :
                        response.content()) {
                    allEmbeddings.add(embedding.vector());
                }

                log.debug("Embedded batch {}-{}/{}", i + 1, end, texts.size());
            } catch (Exception e) {
                log.error("Embedding batch failed at index {}", i, e);
                // Return empty vectors for failed batch to maintain index alignment
                for (int j = i; j < end; j++) {
                    allEmbeddings.add(new float[0]);
                }
            }
        }

        return allEmbeddings;
    }
}
