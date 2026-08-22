package com.kb.application.service;

import com.kb.domain.conversation.Conversation;

import java.util.List;
import java.util.function.Consumer;

/**
 * Q&A 应用服务接口。
 * @author forever-king
 */
public interface IQaApplicationService {

    String askStreaming(String query, String sessionId,
                        Consumer<String> onToken,
                        Consumer<List<Conversation.CitationRef>> onCitations,
                        Consumer<Long> onMessageId);

    String ask(String query, String sessionId);

    List<Conversation> getConversationHistory(String sessionId);

    void recordFeedback(Long messageId, String feedback);
}
