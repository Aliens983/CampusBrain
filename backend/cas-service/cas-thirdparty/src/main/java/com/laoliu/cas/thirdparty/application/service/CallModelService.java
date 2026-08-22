package com.laoliu.cas.thirdparty.application.service;

import com.laoliu.cas.thirdparty.interfaces.dto.request.ChatReqVO;
import com.laoliu.cas.thirdparty.interfaces.dto.response.ChatRespVO;

/**
 * 大模型调用应用层服务接口。
 *
 * @author forever-king
 */
public interface CallModelService {
    /** 调用Qwen大模型进行对话 */
    ChatRespVO callQwenModel(Long userId, ChatReqVO request);
}
