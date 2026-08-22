package com.laoliu.cas.thirdparty.infrastructure.persistence.mapper;

import com.laoliu.cas.thirdparty.infrastructure.persistence.dataobject.AiChatHistoryDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author forever-king
 */
@Mapper
public interface AiChatHistoryMapper {
    void insert(AiChatHistoryDO aiChatHistoryDO);
}