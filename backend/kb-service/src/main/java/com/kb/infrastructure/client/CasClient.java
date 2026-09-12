package com.kb.infrastructure.client;

import com.kb.infrastructure.client.dto.CasBookingDraft;
import com.kb.infrastructure.client.dto.CasBookingDraftRequest;
import com.kb.infrastructure.client.dto.CasBookingResult;
import com.kb.infrastructure.client.dto.CasConsultantOption;
import com.kb.infrastructure.client.dto.CasEquipmentOption;
import com.kb.infrastructure.client.dto.CasRoomOption;
import com.kb.infrastructure.client.dto.CasServiceOption;
import com.kb.infrastructure.client.dto.CasTimeSlot;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * CAS 服务接口客户端（Feign + Nacos 服务发现）
 * <p>
 * 通过内网签名头调用 CAS，用于 AI 助手实时查询预约数据并代办预约。
 * 直连 CAS（不走网关），依赖 {@link CasFeignConfig} 注入 X-Internal-Sign 签名。
 * </p>
 * <p>
 * 两类接口：
 * <ol>
 *   <li>{@code /appointments/availability}、{@code /appointments/mine} —— 原有精简只读接口；</li>
 *   <li>{@code /appointments/assistant/**} —— 预约助手专用：按校区/分类/时段过滤的查询，
 *       以及「草稿 → 确认」两段式预约。</li>
 * </ol>
 *
 * @author forever-king
 */
@FeignClient(name = "cas-service", configuration = CasFeignConfig.class)
public interface CasClient {

    // ==================== 原有精简接口 ====================

    /**
     * 获取可预约服务及其实时预约数
     */
    @GetMapping("/api/v1/appointments/availability")
    CasResult<List<CasAvailability>> getAvailability();

    /**
     * 获取当前登录用户的预约记录（X-User-Id 由 Feign 拦截器动态注入）
     */
    @GetMapping("/api/v1/appointments/mine")
    CasResult<List<CasBooking>> getMyBookings();

    // ==================== 预约助手：查询 ====================

    @GetMapping("/api/v1/appointments/assistant/services")
    CasResult<List<CasServiceOption>> getAssistantServices(
            @RequestParam("campus") String campus,
            @RequestParam("category") String category,
            @RequestParam("keyword") String keyword);

    @GetMapping("/api/v1/appointments/assistant/consultants")
    CasResult<List<CasConsultantOption>> getAssistantConsultants(
            @RequestParam("campus") String campus,
            @RequestParam("keyword") String keyword,
            @RequestParam("date") String date);

    @GetMapping("/api/v1/appointments/assistant/consultants/{consultantId}/slots")
    CasResult<List<CasTimeSlot>> getConsultantSlots(
            @PathVariable("consultantId") Long consultantId,
            @RequestParam("date") String date);

    @GetMapping("/api/v1/appointments/assistant/rooms")
    CasResult<List<CasRoomOption>> getAssistantRooms(
            @RequestParam("campus") String campus,
            @RequestParam("date") String date,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime);

    @GetMapping("/api/v1/appointments/assistant/equipment")
    CasResult<List<CasEquipmentOption>> getAssistantEquipment(
            @RequestParam("campus") String campus,
            @RequestParam("keyword") String keyword,
            @RequestParam("date") String date,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime);

    @GetMapping("/api/v1/appointments/assistant/my-bookings")
    CasResult<List<CasBooking>> getMyBookingsByStatus(
            @RequestParam("manageStatus") Integer manageStatus);

    // ==================== 预约助手：两段式预约 ====================

    @PostMapping("/api/v1/appointments/assistant/bookings/draft")
    CasResult<CasBookingDraft> createBookingDraft(@RequestBody CasBookingDraftRequest request);

    @GetMapping("/api/v1/appointments/assistant/bookings/draft/{draftId}")
    CasResult<CasBookingDraft> getBookingDraft(@PathVariable("draftId") String draftId);

    @DeleteMapping("/api/v1/appointments/assistant/bookings/draft/{draftId}")
    CasResult<Void> discardBookingDraft(@PathVariable("draftId") String draftId);

    @PostMapping("/api/v1/appointments/assistant/bookings/{draftId}/confirm")
    CasResult<CasBookingResult> confirmBookingDraft(@PathVariable("draftId") String draftId);

    @PostMapping("/api/v1/appointments/assistant/bookings/{orderId}/cancel")
    CasResult<CasBookingResult> cancelBooking(@PathVariable("orderId") Long orderId);
}
