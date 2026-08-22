package com.laoliu.cas.thirdparty.infrastructure.persistence.repository;

import com.laoliu.cas.thirdparty.domain.entity.AiChatHistory;
import com.laoliu.cas.thirdparty.domain.repository.AiChatHistoryRepository;
import com.laoliu.cas.thirdparty.infrastructure.persistence.dataobject.AiChatHistoryDO;
import com.laoliu.cas.thirdparty.infrastructure.persistence.mapper.AiChatHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * AI 对话历史仓储实现
 *
 * @author forever-king
 */
@Repository
@RequiredArgsConstructor
public class AiChatHistoryRepositoryImpl implements AiChatHistoryRepository {

    private final AiChatHistoryMapper aiChatHistoryMapper;

    @Override
    public void save(AiChatHistory chatHistory) {
        AiChatHistoryDO chatHistoryDO = AiChatHistoryDO.fromEntity(chatHistory);
        aiChatHistoryMapper.insert(chatHistoryDO);
    }
}
