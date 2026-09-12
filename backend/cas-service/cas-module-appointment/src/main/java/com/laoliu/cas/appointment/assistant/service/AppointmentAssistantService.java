package com.laoliu.cas.appointment.assistant.service;

import com.laoliu.cas.appointment.assistant.dto.request.AssistantBookingDraftRequest;
import com.laoliu.cas.appointment.assistant.dto.response.AssistantBookingDraft;
import com.laoliu.cas.appointment.assistant.dto.response.AssistantBookingResult;
import com.laoliu.cas.appointment.assistant.dto.response.AssistantConsultantVO;
import com.laoliu.cas.appointment.assistant.dto.response.AssistantEquipmentVO;
import com.laoliu.cas.appointment.assistant.dto.response.AssistantRoomVO;
import com.laoliu.cas.appointment.assistant.dto.response.AssistantServiceVO;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceStatusResponse;
import com.laoliu.cas.appointment.interfaces.dto.response.TimeSlotRespVO;

import java.util.List;

/**
 * 预约助手应用服务 —— 为 KB 智能助手提供「校园预约」实时查询与代办能力
 * <p>
 * 设计要点：
 * <ol>
 *   <li>查询类方法全部只读，支持按校区 / 分类 / 关键词 / 日期时段过滤，供 AI 做信息检索；</li>
 *   <li>预约动作采用<b>两段式</b>：先 {@link #createDraft} 生成待确认草稿（只校验不落库），
 *       用户明确确认后再 {@link #confirmDraft} 真正下单，避免 AI 擅自替用户预约；</li>
 *   <li>草稿存 Redis 并设 TTL，确认时二次校验（容量/时段/库存可能已变），防越权与过期重放。</li>
 * </ol>
 *
 * @author forever-king
 */
public interface AppointmentAssistantService {

    /** 查询可预约服务（校区 / 分类 / 关键词过滤，带实时余量） */
    List<AssistantServiceVO> findServices(String campus, String category, String keyword);

    /** 查询咨询师（校区由所属服务继承；传 date 时附带该日可用时段数） */
    List<AssistantConsultantVO> findConsultants(String campus, String keyword, String date);

    /** 查询某咨询师某日的可预约时段 */
    List<TimeSlotRespVO> findConsultantSlots(Long consultantId, String date);

    /** 查询教室（传 date+起止时间时附带是否空闲） */
    List<AssistantRoomVO> findRooms(String campus, String date, String startTime, String endTime);

    /** 查询设备（传借用窗口时附带该窗口剩余可借数量） */
    List<AssistantEquipmentVO> findEquipment(String campus, String keyword, String date,
                                             String startTime, String endTime);

    /** 查询我的预约（manageStatus 为空表示全部） */
    List<ServiceStatusResponse> findMyBookings(Long userId, Integer manageStatus);

    /** 生成预约草稿（只校验 + 预览，不落库） */
    AssistantBookingDraft createDraft(Long userId, AssistantBookingDraftRequest request);

    /** 读取草稿（用于前端渲染确认卡片） */
    AssistantBookingDraft getDraft(Long userId, String draftId);

    /** 放弃草稿 */
    void discardDraft(Long userId, String draftId);

    /** 确认草稿并真正下单 */
    AssistantBookingResult confirmDraft(Long userId, String draftId);

    /** 取消我的预约（仅限本人待审核单与已通过的活动单，语义同用户端自助取消） */
    AssistantBookingResult cancelBooking(Long userId, Long orderId);
}
