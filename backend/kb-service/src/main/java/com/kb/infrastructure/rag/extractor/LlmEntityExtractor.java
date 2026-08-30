package com.kb.infrastructure.rag.extractor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.domain.knowledgegraph.EntityExtractor;
import com.kb.domain.knowledgegraph.KnowledgeEntity;
import com.kb.domain.rag.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 基于 LLM 的实体抽取实现
 * <p>
 * 通过 Few-Shot Prompt 引导 LLM 提取人名、地名、术语、产品名等实体，
 * 并返回 JSON 结构化结果。适合中文企业文档场景
 * </p>
 *
 * @author forever-king
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmEntityExtractor implements EntityExtractor {

    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    /** 单次抽取最大字符数 */
    private static final int MAX_CHARS = 2000;

    @Override
    public List<KnowledgeEntity> extract(String text, Long documentId, String chunkId) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        String prompt = buildExtractionPrompt(
                text.length() > MAX_CHARS ? text.substring(0, MAX_CHARS) : text);

        try {
            String response = llmService.generateAnswer(prompt, List.of(), List.of());
            return parseResponse(response, documentId, chunkId);
        } catch (Exception e) {
            log.warn("Entity extraction failed for docId={}, chunkId={}: {}",
                    documentId, chunkId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private String buildExtractionPrompt(String text) {
        return """
            你是一个命名实体识别（NER）专家。请从以下文本中提取所有实体，
            按 JSON 数组格式返回。每个实体包含：
            - name: 实体名称（规范形式）
            - type: PERSON | LOCATION | TERM | PRODUCT | DATE | METRIC
            - contextSnippet: 实体出现的原文片段（不超过 50 字）
            - confidence: 置信度 0-1

            只返回 JSON 数组，不要任何解释。若无实体，返回 []。

            文本：
            """ + text;
    }

    private List<KnowledgeEntity> parseResponse(String response, Long documentId,
                                                 String chunkId) {
        try {
            String json = extractJsonArray(response);
            List<Map<String, Object>> items = objectMapper.readValue(
                    json, new TypeReference<>() {});

            return items.stream().map(item -> KnowledgeEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .name((String) item.getOrDefault("name", ""))
                    .type((String) item.getOrDefault("type", "TERM"))
                    .documentId(documentId)
                    .chunkId(chunkId)
                    .contextSnippet((String) item.getOrDefault("contextSnippet", ""))
                    .confidence(toDouble(item.get("confidence")))
                    .build()).toList();
        } catch (Exception e) {
            log.warn("Failed to parse entity extraction response: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /** 从 LLM 响应中提取 JSON 数组 */
    private String extractJsonArray(String response) {
        int start = response.indexOf('[');
        int end = response.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return "[]";
    }

    private Double toDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0.5; }
        }
        return 0.5;
    }
}
