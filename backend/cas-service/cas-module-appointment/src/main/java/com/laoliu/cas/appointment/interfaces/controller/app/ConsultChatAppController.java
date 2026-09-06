package com.laoliu.cas.appointment.interfaces.controller.app;

import com.laoliu.cas.appointment.application.service.ConsultChatService;
import com.laoliu.cas.appointment.interfaces.dto.request.OpenChatByBookingRequest;
import com.laoliu.cas.appointment.interfaces.dto.request.OpenChatRequest;
import com.laoliu.cas.appointment.interfaces.dto.request.OpenChatWithStudentRequest;
import com.laoliu.cas.appointment.interfaces.dto.request.SendMessageRequest;
import com.laoliu.cas.appointment.interfaces.dto.response.ConversationRespVO;
import com.laoliu.cas.appointment.interfaces.dto.response.MessageRespVO;
import com.laoliu.cas.common.annotation.RequireRole;
import com.laoliu.cas.common.enums.UserRoleEnum;
import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.common.security.SecurityFrameworkUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 咨询沟通接口（学生 ⇄ 教师 1:1 在线留言）
 * <p>
 * 学生与教师共用同一套会话，调用者身份（student_id / teacher_id）由登录者角色判定，
 * 仅「教师咨询」场景开放；设备/教室/活动不提供聊天。消息接收采用轮询增量拉取。
 *
 * @author forever-king
 */
@Tag(name = "咨询沟通（学生⇄教师）")
@RestController
@RequestMapping("/app/chat/consult")
@RequiredArgsConstructor
public class ConsultChatAppController {

    private final ConsultChatService consultChatService;

    @Operation(summary = "我的会话列表")
    @GetMapping("/conversations")
    @RequireRole({UserRoleEnum.USER, UserRoleEnum.TEACHER})
    public CommonResult<List<ConversationRespVO>> listConversations() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        Integer role = SecurityFrameworkUtils.getLoginUserRole();
        return CommonResult.success(consultChatService.listConversations(userId, role));
    }

    @Operation(summary = "总未读数（导航红点）")
    @GetMapping("/conversations/unread-count")
    @RequireRole({UserRoleEnum.USER, UserRoleEnum.TEACHER})
    public CommonResult<Long> unreadCount() {
        return CommonResult.success(consultChatService.unreadTotal(SecurityFrameworkUtils.getLoginUserId()));
    }

    @Operation(summary = "学生从选咨询师卡片打开/创建会话（仅教师咨询类咨询师）")
    @PostMapping("/conversations/open-with-consultant")
    @RequireRole({UserRoleEnum.USER, UserRoleEnum.TEACHER})
    public CommonResult<ConversationRespVO> openWithConsultant(@Valid @RequestBody OpenChatRequest request) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        Integer role = SecurityFrameworkUtils.getLoginUserRole();
        return CommonResult.success(consultChatService.openWithConsultant(userId, role, request.getConsultantId()));
    }

    @Operation(summary = "教师对其名下咨询档期的学生打开/创建会话")
    @PostMapping("/conversations/open-with-student")
    @RequireRole({UserRoleEnum.USER, UserRoleEnum.TEACHER})
    public CommonResult<ConversationRespVO> openWithStudent(@Valid @RequestBody OpenChatWithStudentRequest request) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        Integer role = SecurityFrameworkUtils.getLoginUserRole();
        return CommonResult.success(consultChatService.openWithStudent(userId, role, request.getStudentId()));
    }

    @Operation(summary = "学生凭自己的咨询预约单打开/创建会话（我的预约进入）")
    @PostMapping("/conversations/open-by-booking")
    @RequireRole({UserRoleEnum.USER, UserRoleEnum.TEACHER})
    public CommonResult<ConversationRespVO> openByBooking(@Valid @RequestBody OpenChatByBookingRequest request) {
        return CommonResult.success(
                consultChatService.openByBooking(SecurityFrameworkUtils.getLoginUserId(), request.getOrderId()));
    }

    @Operation(summary = "拉取会话消息", description = "afterId 为空=全量历史（升序）；非空=拉取 id 大于该值的增量（轮询）")
    @GetMapping("/conversations/{id}/messages")
    @RequireRole({UserRoleEnum.USER, UserRoleEnum.TEACHER})
    public CommonResult<List<MessageRespVO>> listMessages(@PathVariable Long id,
                                                          @RequestParam(required = false) Long afterId) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return CommonResult.success(consultChatService.listMessages(userId, id, afterId));
    }

    @Operation(summary = "发送消息")
    @PostMapping("/conversations/{id}/messages")
    @RequireRole({UserRoleEnum.USER, UserRoleEnum.TEACHER})
    public CommonResult<MessageRespVO> sendMessage(@PathVariable Long id,
                                                   @Valid @RequestBody SendMessageRequest request) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return CommonResult.success(consultChatService.sendMessage(userId, id, request.getContent()));
    }

    @Operation(summary = "把会话中发给我的消息置为已读")
    @PutMapping("/conversations/{id}/read")
    @RequireRole({UserRoleEnum.USER, UserRoleEnum.TEACHER})
    public CommonResult<Integer> markRead(@PathVariable Long id) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return CommonResult.success(consultChatService.markRead(userId, id));
    }
}
