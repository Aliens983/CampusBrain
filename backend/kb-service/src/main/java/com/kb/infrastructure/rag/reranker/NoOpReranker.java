package com.kb.infrastructure.rag.reranker;

import com.kb.domain.rag.RerankerService;
import com.kb.domain.rag.RetrievalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * No-operation reranker for MVP.
 * <p>
 * Simply returns the candidate list as-is, keeping the RRF scores.
 * Replace with CrossEncoderReranker when integrating a rerank model
 * (e.g., Jina Reranker API, bge-reranker-large, Cohere Rerank).
 * </p>
 *
 * @author forever-king
 */
@Slf4j
@Component
public class NoOpReranker implements RerankerService {

    @Override
    public List<RetrievalResult> rerank(String query, List<RetrievalResult> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        // Sort by existing score (from RRF fusion)
        candidates.sort(Comparator.comparingDouble(RetrievalResult::getScore).reversed());

        log.debug("Reranker (no-op): returning {} candidates", candidates.size());
        return candidates;
    }
}
