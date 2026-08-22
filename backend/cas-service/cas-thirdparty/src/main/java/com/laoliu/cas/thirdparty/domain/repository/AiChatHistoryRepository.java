package com.laoliu.cas.thirdparty.domain.repository;

import com.laoliu.cas.thirdparty.domain.entity.AiChatHistory;

/**
 * AI 对话历史仓储接口
 *
 * @author forever-king
 */
public interface AiChatHistoryRepository {
    void save(AiChatHistory chatHistory);
}
