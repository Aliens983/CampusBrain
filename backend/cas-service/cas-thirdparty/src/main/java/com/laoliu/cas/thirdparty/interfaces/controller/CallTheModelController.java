package com.laoliu.cas.thirdparty.interfaces.controller;

import com.laoliu.cas.common.api.GetUserIdViaTokenApi;
import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.thirdparty.application.service.CallModelService;
import com.laoliu.cas.thirdparty.interfaces.dto.request.ChatReqVO;
import com.laoliu.cas.thirdparty.interfaces.dto.response.ChatRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author forever-king
 */
@Slf4j
@Tag(name = "大模型调用")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class CallTheModelController {

    private final CallModelService qwenService;
    private final GetUserIdViaTokenApi getUserIdViaTokenApi;

    @Operation(summary = "调用Qwen大模型", description = "与阿里云Qwen大模型进行对话交互，支持自定义模型名称，默认使用qwen-plus模型")
    @PostMapping("/chat")
    public CommonResult<ChatRespVO> chatWithQwen(@Valid @RequestBody ChatReqVO request) {
        Long userId = getUserIdViaTokenApi.getUserId();

        log.info("收到聊天请求，消息: {},用户ID:{}", request.getMessage(), userId);

        if (request.getModel() == null || request.getModel().trim().isEmpty()) {
            request.setModel("qwen-plus");
        }

        ChatRespVO response = qwenService.callQwenModel(userId, request);
        log.info("返回聊天响应，成功: {}, 模型: {}", response.isSuccess(), response.getModel());
        return CommonResult.success(response);
    }
}
