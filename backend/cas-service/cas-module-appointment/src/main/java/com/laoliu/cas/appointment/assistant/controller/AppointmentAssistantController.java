package com.laoliu.cas.appointment.assistant.controller;

import com.laoliu.cas.appointment.assistant.dto.request.AssistantBookingDraftRequest;
import com.laoliu.cas.appointment.assistant.dto.response.AssistantBookingDraft;
import com.laoliu.cas.appointment.assistant.dto.response.AssistantBookingResult;
import com.laoliu.cas.appointment.assistant.dto.response.AssistantConsultantVO;
import com.laoliu.cas.appointment.assistant.dto.response.AssistantEquipmentVO;
import com.laoliu.cas.appointment.assistant.dto.response.AssistantRoomVO;
import com.laoliu.cas.appointment.assistant.dto.response.AssistantServiceVO;
import com.laoliu.cas.appointment.assistant.service.AppointmentAssistantService;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceStatusResponse;
import com.laoliu.cas.appointment.interfaces.dto.response.TimeSlotRespVO;
import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.common.security.SecurityFrameworkUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 预约助手专用接口（内网签名调用，供 kb-service 的 Function Calling 使用）
 * <p>
 * 与普通用户端接口的区别：
 * <ol>
 *   <li>查询接口支持按「校区 / 分类 / 日期时段」组合过滤，并把校区、余量、是否空闲
 *       一并回填，减少 AI 二次推理；</li>
 *   <li>预约动作拆成「草稿 → 确认」两段，草稿只校验不落库，
 *       必须由用户明确确认后才真正下单。</li>
 * </ol>
 *
 * <p>身份来源：优先 JWT 登录用户；无 JWT 时（KB 内网签名场景，该头已经过
 * {@code InternalAuthFilter} 签名校验绑定）才信任 {@code X-User-Id}。
 *
 * @author forever-king
 */
@Tag(name = "AI 预约助手（内网）")
@RestController
@RequestMapping("/appointments/assistant")
@RequiredArgsConstructor
public class AppointmentAssistantController {

    private final AppointmentAssistantService assistantService;

    // ==================== 查询 ====================

    @Operation(summary = "查询可预约服务（带校区、分类与实时余量）")
    @GetMapping("/services")
    public CommonResult<List<AssistantServiceVO>> services(
            @Parameter(description = "校区 cq仓前 / xs下沙，不传=全部") @RequestParam(required = false) String campus,
            @Parameter(description = "分类编码 teacher/equipment/space/activity，不传=全部") @RequestParam(required = false) String category,
            @Parameter(description = "名称/描述关键词，不传=全部") @RequestParam(required = false) String keyword) {
        return CommonResult.success(assistantService.findServices(campus, category, keyword));
    }

    @Operation(summary = "查询咨询师（校区由所属服务继承）")
    @GetMapping("/consultants")
    public CommonResult<List<AssistantConsultantVO>> consultants(
            @Parameter(description = "校区 cq/xs") @RequestParam(required = false) String campus,
            @Parameter(description = "姓名/部门/职称/简介关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "日期 yyyy-MM-dd，传了则附带该日可用时段数") @RequestParam(required = false) String date) {
        return CommonResult.success(assistantService.findConsultants(campus, keyword, date));
    }

    @Operation(summary = "查询某咨询师某日的可预约时段")
    @GetMapping("/consultants/{consultantId}/slots")
    public CommonResult<List<TimeSlotRespVO>> consultantSlots(
            @Parameter(description = "咨询师ID", required = true) @PathVariable Long consultantId,
            @Parameter(description = "日期 yyyy-MM-dd", required = true) @RequestParam String date) {
        return CommonResult.success(assistantService.findConsultantSlots(consultantId, date));
    }

    @Operation(summary = "查询教室（传日期时段则附带是否空闲）")
    @GetMapping("/rooms")
    public CommonResult<List<AssistantRoomVO>> rooms(
            @Parameter(description = "校区 cq/xs") @RequestParam(required = false) String campus,
            @Parameter(description = "日期 yyyy-MM-dd") @RequestParam(required = false) String date,
            @Parameter(description = "开始时间 HH:mm") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间 HH:mm") @RequestParam(required = false) String endTime) {
        return CommonResult.success(assistantService.findRooms(campus, date, startTime, endTime));
    }

    @Operation(summary = "查询设备（传借用窗口则附带该窗口剩余可借数量）")
    @GetMapping("/equipment")
    public CommonResult<List<AssistantEquipmentVO>> equipment(
            @Parameter(description = "校区 cq/xs") @RequestParam(required = false) String campus,
            @Parameter(description = "名称/分类/描述关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "日期 yyyy-MM-dd") @RequestParam(required = false) String date,
            @Parameter(description = "开始时间 HH:mm") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间 HH:mm") @RequestParam(required = false) String endTime) {
        return CommonResult.success(assistantService.findEquipment(campus, keyword, date, startTime, endTime));
    }

    @Operation(summary = "查询我的预约")
    @GetMapping("/my-bookings")
    public CommonResult<List<ServiceStatusResponse>> myBookings(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @Parameter(description = "状态 0待审/1通过/2拒绝/3取消/4完成，不传=全部") @RequestParam(required = false) Integer manageStatus) {
        Long userId = resolveUserId(headerUserId);
        if (userId == null) {
            return CommonResult.badRequest("缺少用户身份");
        }
        return CommonResult.success(assistantService.findMyBookings(userId, manageStatus));
    }

    // ==================== 两段式预约 ====================

    @Operation(summary = "生成预约草稿（只校验与预览，不落库）")
    @PostMapping("/bookings/draft")
    public CommonResult<AssistantBookingDraft> createDraft(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestBody AssistantBookingDraftRequest request) {
        Long userId = resolveUserId(headerUserId);
        if (userId == null) {
            return CommonResult.badRequest("缺少用户身份");
        }
        return CommonResult.success(assistantService.createDraft(userId, request));
    }

    @Operation(summary = "读取预约草稿（用于渲染确认卡片）")
    @GetMapping("/bookings/draft/{draftId}")
    public CommonResult<AssistantBookingDraft> getDraft(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @Parameter(description = "草稿ID", required = true) @PathVariable String draftId) {
        Long userId = resolveUserId(headerUserId);
        if (userId == null) {
            return CommonResult.badRequest("缺少用户身份");
        }
        return CommonResult.success(assistantService.getDraft(userId, draftId));
    }

    @Operation(summary = "放弃预约草稿")
    @DeleteMapping("/bookings/draft/{draftId}")
    public CommonResult<Void> discardDraft(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @Parameter(description = "草稿ID", required = true) @PathVariable String draftId) {
        Long userId = resolveUserId(headerUserId);
        if (userId == null) {
            return CommonResult.badRequest("缺少用户身份");
        }
        assistantService.discardDraft(userId, draftId);
        return CommonResult.success("已取消该预约草稿", null);
    }

    @Operation(summary = "确认预约草稿并真正下单", description = "二次校验容量/时段/库存后复用既有下单逻辑")
    @PostMapping("/bookings/{draftId}/confirm")
    public CommonResult<AssistantBookingResult> confirmDraft(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @Parameter(description = "草稿ID", required = true) @PathVariable String draftId) {
        Long userId = resolveUserId(headerUserId);
        if (userId == null) {
            return CommonResult.badRequest("缺少用户身份");
        }
        return CommonResult.success(assistantService.confirmDraft(userId, draftId));
    }

    @Operation(summary = "取消我的预约", description = "限本人待审核单，以及已通过的活动报名单")
    @PostMapping("/bookings/{orderId}/cancel")
    public CommonResult<AssistantBookingResult> cancelBooking(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @Parameter(description = "预约单号", required = true) @PathVariable Long orderId) {
        Long userId = resolveUserId(headerUserId);
        if (userId == null) {
            return CommonResult.badRequest("缺少用户身份");
        }
        return CommonResult.success(assistantService.cancelBooking(userId, orderId));
    }

    /**
     * 身份解析：优先 JWT 登录用户，忽略伪造的 X-User-Id；
     * 无 JWT（KB 内网签名场景）时才信任经签名校验绑定的 X-User-Id
     */
    private Long resolveUserId(Long headerUserId) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        return loginUserId != null ? loginUserId : headerUserId;
    }
}
