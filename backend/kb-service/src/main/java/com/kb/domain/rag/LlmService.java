package com.kb.domain.rag;

import java.util.List;
import java.util.function.Consumer;

/**
 * Domain interface for LLM-powered answer generation.
 * <p>
 * Supports both synchronous and streaming (SSE) response modes.
 * </p>
 * @author forever-king
 */
public interface LlmService {

    /**
     * Generate an answer synchronously.
     *
     * @param query               the user's question
     * @param retrievedDocs       relevant document chunks from retrieval
     * @param conversationHistory previous messages in this session
     * @return the generated answer text
     */
    String generateAnswer(String query, List<RetrievalResult> retrievedDocs,
                          List<ChatMessage> conversationHistory);

    /**
     * Generate an answer with streaming (token-by-token via callback).
     *
     * @param query               the user's question
     * @param retrievedDocs       relevant document chunks from retrieval
     * @param conversationHistory previous messages in this session
     * @param tokenConsumer       callback invoked for each generated token
     * @return the complete generated answer text
     */
    String generateAnswerStreaming(String query, List<RetrievalResult> retrievedDocs,
                                   List<ChatMessage> conversationHistory,
                                   Consumer<String> tokenConsumer);

    /**
     * Generate an answer with real-time tool (Function Calling) enhancement.
     * LLM 可在回答时自主调用预约查询工具获取实时数据
     *
     * @param query               the user's question
     * @param retrievedDocs       relevant document chunks from retrieval
     * @param conversationHistory previous messages in this session
     * @param tokenConsumer       callback invoked with the generated answer
     * @return the generated answer text
     */
    String generateAnswerWithTools(String query, List<RetrievalResult> retrievedDocs,
                                   List<ChatMessage> conversationHistory,
                                   Consumer<String> tokenConsumer);

    /**
     * 本地资料库未检索到相关内容时的兜底回答：直接用大模型（DeepSeek）对话，
     * 不携带 RAG 上下文
     *
     * @param query               the user's question
     * @param conversationHistory previous messages in this session
     * @return the generated answer text
     */
    String generateAnswerDirect(String query, List<ChatMessage> conversationHistory);

    /**
     * 本地资料库未检索到相关内容时的兜底回答（流式版，SSE 逐 token 推送）
     */
    String generateAnswerDirectStreaming(String query, List<ChatMessage> conversationHistory,
                                         Consumer<String> tokenConsumer);

    /**
     * A simplified chat message for domain use (no LangChain4j dependency).
     */
    record ChatMessage(String role, String content) {
        public static ChatMessage user(String content) {
            return new ChatMessage("user", content);
        }
        public static ChatMessage assistant(String content) {
            return new ChatMessage("assistant", content);
        }
        public static ChatMessage system(String content) {
            return new ChatMessage("system", content);
        }
    }
}
