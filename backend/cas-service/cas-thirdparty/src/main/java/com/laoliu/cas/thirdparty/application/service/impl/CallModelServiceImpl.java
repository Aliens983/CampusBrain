package com.laoliu.cas.thirdparty.application.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laoliu.cas.thirdparty.application.service.CallModelService;
import com.laoliu.cas.thirdparty.domain.entity.AiChatHistory;
import com.laoliu.cas.thirdparty.domain.repository.AiChatHistoryRepository;
import com.laoliu.cas.thirdparty.infrastructure.config.DeepSeekConfig;
import com.laoliu.cas.thirdparty.infrastructure.config.QwenConfig;
import com.laoliu.cas.thirdparty.interfaces.dto.request.ChatReqVO;
import com.laoliu.cas.thirdparty.interfaces.dto.response.ChatRespVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author forever-king
 */
@Slf4j
@Service
public class CallModelServiceImpl implements CallModelService {

    private final QwenConfig qwenConfig;
    private final DeepSeekConfig deepSeekConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiChatHistoryRepository aiChatHistoryRepository;

    public CallModelServiceImpl(QwenConfig qwenConfig, DeepSeekConfig deepSeekConfig, AiChatHistoryRepository aiChatHistoryRepository) {
        this.qwenConfig = qwenConfig;
        this.deepSeekConfig = deepSeekConfig;
        this.aiChatHistoryRepository = aiChatHistoryRepository;
    }

    @Override
    public ChatRespVO callQwenModel(Long userId, ChatReqVO request) {
        String model = request.getModel();
        if (model != null && model.startsWith("deepseek")) {
            return callDeepSeekModel(userId, request);
        }
        return callQwenApi(userId, request);
    }

    private ChatRespVO callQwenApi(Long userId, ChatReqVO request) {
        long startTime = System.currentTimeMillis();
        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl(qwenConfig.getApiUrl())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader("Authorization", "Bearer " + qwenConfig.getApiKey())
                    .build();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", request.getModel());

            Map<String, Object> input = new HashMap<>();
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个幽默的AI助手知识渊博,要友好解答问题.");
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", request.getMessage());
            input.put("messages", new Object[]{systemMessage, userMessage});
            requestBody.put("input", input);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("result_format", "message");
            requestBody.put("parameters", parameters);

            String jsonResponse = webClient.post().uri("").bodyValue(requestBody).retrieve().bodyToMono(String.class).block();
            JsonNode responseNode = objectMapper.readTree(jsonResponse);
            JsonNode choicesNode = responseNode.path("output").path("choices");

            if (choicesNode.isArray() && !choicesNode.isEmpty()) {
                String content = choicesNode.get(0).path("message").path("content").asText();
                long endTime = System.currentTimeMillis();
                saveHistory(userId, request.getModel(), request.getMessage(), content, (int)(endTime - startTime));
                return new ChatRespVO(content, request.getModel(), (int)(endTime - startTime));
            }
            return new ChatRespVO(false, "无法从API响应中提取内容");
        } catch (Exception e) {
            log.error("调用Qwen API失败: ", e);
            return new ChatRespVO(false, "AI服务暂时不可用: " + e.getMessage());
        }
    }

    private ChatRespVO callDeepSeekModel(Long userId, ChatReqVO request) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("调用 DeepSeek API, model: {}", request.getModel());

            WebClient webClient = WebClient.builder()
                    .baseUrl(deepSeekConfig.getApiUrl())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader("Authorization", "Bearer " + deepSeekConfig.getApiKey())
                    .build();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", request.getModel());

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> sysMsg = new HashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", "你是一个知识渊博的AI助手，请用友好、专业的方式回答问题。");
            messages.add(sysMsg);

            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", request.getMessage());
            messages.add(userMsg);

            requestBody.put("messages", messages);

            String jsonResponse = webClient.post().uri("/chat/completions").bodyValue(requestBody).retrieve().bodyToMono(String.class).block();
            log.info("DeepSeek 响应: {}", jsonResponse);

            JsonNode responseNode = objectMapper.readTree(jsonResponse);
            JsonNode choicesNode = responseNode.path("choices");

            if (choicesNode.isArray() && !choicesNode.isEmpty()) {
                String content = choicesNode.get(0).path("message").path("content").asText();
                long endTime = System.currentTimeMillis();
                saveHistory(userId, request.getModel(), request.getMessage(), content, (int)(endTime - startTime));
                return new ChatRespVO(content, request.getModel(), (int)(endTime - startTime));
            }
            return new ChatRespVO(false, "无法从 DeepSeek 响应中提取内容");
        } catch (Exception e) {
            log.error("调用 DeepSeek API 失败: ", e);
            return new ChatRespVO(false, "DeepSeek 服务暂时不可用: " + e.getMessage());
        }
    }

    private void saveHistory(Long userId, String model, String userMsg, String aiResp, int responseTimeMs) {
        try {
            AiChatHistory chatHistory = AiChatHistory.builder()
                    .userId(userId).model(model)
                    .userMessage(userMsg).aiResponse(aiResp)
                    .responseTimeMs(responseTimeMs)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            aiChatHistoryRepository.save(chatHistory);
        } catch (Exception e) {
            log.warn("保存对话历史失败: {}", e.getMessage());
        }
    }
}
