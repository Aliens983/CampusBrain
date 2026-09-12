package com.kb.application.service;

import com.kb.domain.chat.AssistantEvent;
import com.kb.domain.conversation.Conversation;

import java.util.List;
import java.util.function.Consumer;

/**
 * Q&A 应用服务接口
 * @author forever-king
 */
public interface IQaApplicationService {

    /**
     * 流式问答（带结构化事件回调）
     *
     * @param userId  当前登录用户；传 null 时由服务内部从 SecurityContext 解析
     * @param onEvent 用于向前端推送槽位更新、待确认预约草稿、预约动作结果
     */
    String askStreaming(String query, String sessionId, Long userId,
                        Consumer<String> onToken,
                        Consumer<List<Conversation.CitationRef>> onCitations,
                        Consumer<Long> onMessageId,
                        Consumer<AssistantEvent> onEvent);

    /**
     * 流式问答（带结构化事件回调，用户身份由服务内部解析）
     *
     * @param onEvent 用于向前端推送槽位更新、待确认预约草稿、预约动作结果
     */
    String askStreaming(String query, String sessionId,
                        Consumer<String> onToken,
                        Consumer<List<Conversation.CitationRef>> onCitations,
                        Consumer<Long> onMessageId,
                        Consumer<AssistantEvent> onEvent);

    /**
     * 流式问答（无事件回调的简化签名）
     */
    String askStreaming(String query, String sessionId,
                        Consumer<String> onToken,
                        Consumer<List<Conversation.CitationRef>> onCitations,
                        Consumer<Long> onMessageId);

    String ask(String query, String sessionId);

    List<Conversation> getConversationHistory(String sessionId);

    void recordFeedback(Long messageId, String feedback);

    /** 清空会话上下文（槽位与待确认草稿），不影响消息历史 */
    void resetSession(String sessionId);
}
