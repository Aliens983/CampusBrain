package com.kb.infrastructure.rag.tool;

import com.kb.domain.chat.BookingSlots;
import com.kb.domain.chat.ChatContextHolder;
import com.kb.domain.chat.ChatSession;
import com.kb.domain.chat.ChatSessionRepository;
import com.kb.domain.chat.PendingBooking;
import com.kb.infrastructure.client.CasClient;
import com.kb.infrastructure.client.CasResult;
import com.kb.infrastructure.client.dto.CasBookingDraft;
import com.kb.infrastructure.client.dto.CasBookingDraftRequest;
import com.kb.infrastructure.client.dto.CasConsultantOption;
import com.kb.infrastructure.client.dto.CasEquipmentOption;
import com.kb.infrastructure.client.dto.CasRoomOption;
import com.kb.infrastructure.client.dto.CasServiceOption;
import com.kb.infrastructure.client.dto.CasTimeSlot;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * 校园预约工具集（LangChain4j Function Calling）
 * <p>
 * 覆盖「查」与「办」两类能力：
 * <ul>
 *   <li>查询类：可预约服务、咨询师与时段、教室、设备、我的预约；</li>
 *   <li>动作类：{@link #prepareBooking} 只生成待确认草稿，
 *       {@link #requestCancelBooking} 只登记取消意向，
 *       <b>两者都不会直接改数据</b>，必须由用户明确确认后由应用层执行。</li>
 * </ul>
 *
 * <p>参数缺失时自动回退到会话上下文中已累积的槽位（{@link ChatContextHolder}），
 * 这样用户说"就按刚才那个约"也能正确执行。
 *
 * @author forever-king
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentTool {

    private final CasClient casClient;
    private final ChatSessionRepository chatSessionRepository;

    // ==================== 查询类工具 ====================

    @Tool("查询当前可预约的校园服务列表及其实时余量。可按校区、业务分类和关键词过滤。")
    public String searchAvailableServices(
            @P("校区：cq=仓前校区，xs=下沙校区。用户没说或不确定时留空字符串") String campus,
            @P("业务分类：teacher=教师咨询，equipment=设备借用，space=教室空间，activity=活动报名。不确定时留空字符串") String category,
            @P("服务名称关键词，如'自习室''心理咨询'。不确定时留空字符串") String keyword) {
        String resolvedCampus = normalizeCampus(firstNonBlank(campus, contextCampus()));
        String resolvedCategory = firstNonBlank(category, contextCategory());
        try {
            CasResult<List<CasServiceOption>> result = casClient.getAssistantServices(
                    resolvedCampus, resolvedCategory, blankToNull(keyword));
            if (!result.isSuccess() || result.getData() == null || result.getData().isEmpty()) {
                return "没有查到符合条件的可预约服务。可换个校区（cq 仓前 / xs 下沙）或换个关键词试试。";
            }
            StringBuilder sb = new StringBuilder("可预约服务如下（共 " + result.getData().size() + " 项）：\n");
            for (CasServiceOption s : result.getData()) {
                sb.append("- [服务ID ").append(s.getServiceId()).append("] ")
                        .append(s.getServiceName())
                        .append("（").append(nullToDash(s.getCampusName()))
                        .append(" / ").append(nullToDash(s.getCategoryName())).append("）");
                if (s.getRemaining() != null && s.getRemaining() >= 0) {
                    sb.append("，剩余 ").append(s.getRemaining()).append(" 个名额");
                } else {
                    sb.append("，名额不限");
                }
                if (Boolean.FALSE.equals(s.getBookable())) {
                    sb.append("，当前不可约：").append(nullToDash(s.getBookableReason()));
                }
                sb.append("\n");
            }
            remember(BookingSlots.builder()
                    .campus(resolvedCampus)
                    .category(resolvedCategory)
                    .build());
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("查询可预约服务失败", e);
            return "预约数据服务暂时不可用，请稍后再试。";
        }
    }

    @Tool("查询可预约的教师咨询师（心理咨询、学业辅导等），可按校区和关键词过滤；传日期可查看每人当天剩余可约时段数。")
    public String searchConsultants(
            @P("校区：cq=仓前校区，xs=下沙校区。用户没说或不确定时留空字符串") String campus,
            @P("教师姓名、部门或擅长领域关键词。不确定时留空字符串") String keyword,
            @P("日期 yyyy-MM-dd，用于统计当天可用时段数。不确定时留空字符串") String date) {
        String resolvedCampus = normalizeCampus(firstNonBlank(campus, contextCampus()));
        String resolvedDate = firstNonBlank(date, contextDate());
        try {
            CasResult<List<CasConsultantOption>> result = casClient.getAssistantConsultants(
                    resolvedCampus, blankToNull(keyword), blankToNull(resolvedDate));
            if (!result.isSuccess() || result.getData() == null || result.getData().isEmpty()) {
                return "没有查到符合条件的咨询师。可换个校区或放宽关键词。";
            }
            StringBuilder sb = new StringBuilder("可预约咨询师（共 " + result.getData().size() + " 位）：\n");
            for (CasConsultantOption c : result.getData()) {
                sb.append("- [咨询师ID ").append(c.getConsultantId()).append("] ")
                        .append(c.getName())
                        .append(c.getTitle() == null ? "" : "（" + c.getTitle() + "）")
                        .append(" · ").append(nullToDash(c.getDepartment()))
                        .append(" · 所属服务 ").append(nullToDash(c.getServiceName()));
                if (c.getAvailableSlotCount() != null) {
                    sb.append(" · ").append(resolvedDate).append(" 可约时段 ").append(c.getAvailableSlotCount()).append(" 个");
                }
                sb.append("\n");
            }
            remember(BookingSlots.builder().campus(resolvedCampus).category("teacher").date(resolvedDate).build());
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("查询咨询师失败", e);
            return "预约数据服务暂时不可用，请稍后再试。";
        }
    }

    @Tool("查询某位咨询师在指定日期的可预约时段，返回时段ID，预约咨询时需要使用该ID。")
    public String searchConsultantTimeSlots(
            @P("咨询师ID，来自咨询师列表") Long consultantId,
            @P("日期 yyyy-MM-dd。不确定时留空字符串") String date) {
        if (consultantId == null) {
            return "缺少咨询师ID，请先用咨询师列表接口确定咨询师。";
        }
        String resolvedDate = firstNonBlank(date, contextDate());
        if (resolvedDate == null || resolvedDate.isBlank()) {
            return "缺少日期，请告诉我要查哪一天（如'明天'、'2026-09-12'）。";
        }
        try {
            CasResult<List<CasTimeSlot>> result = casClient.getConsultantSlots(consultantId, resolvedDate);
            if (!result.isSuccess() || result.getData() == null || result.getData().isEmpty()) {
                return "该咨询师在 " + resolvedDate + " 没有可预约时段，换个日期或换一位咨询师试试。";
            }
            StringBuilder sb = new StringBuilder("咨询师 " + consultantId + " 在 " + resolvedDate + " 的可预约时段：\n");
            for (CasTimeSlot t : result.getData()) {
                sb.append("- [时段ID ").append(t.getSlotId()).append("] ")
                        .append(t.getStartTime()).append("-").append(t.getEndTime()).append("\n");
            }
            remember(BookingSlots.builder().date(resolvedDate).consultantId(consultantId).category("teacher").build());
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("查询咨询时段失败", e);
            return "预约数据服务暂时不可用，请稍后再试。";
        }
    }

    @Tool("查询可预约的教室/自习室；传日期与起止时间可判断哪些教室在该时段空闲。")
    public String searchRooms(
            @P("校区：cq=仓前校区，xs=下沙校区。用户没说或不确定时留空字符串") String campus,
            @P("日期 yyyy-MM-dd。不确定时留空字符串") String date,
            @P("开始时间 HH:mm，如 09:00。不确定时留空字符串") String startTime,
            @P("结束时间 HH:mm，如 10:00。不确定时留空字符串") String endTime) {
        String resolvedCampus = normalizeCampus(firstNonBlank(campus, contextCampus()));
        String resolvedDate = firstNonBlank(date, contextDate());
        String resolvedStart = firstNonBlank(startTime, contextStartTime());
        String resolvedEnd = firstNonBlank(endTime, contextEndTime());
        try {
            CasResult<List<CasRoomOption>> result = casClient.getAssistantRooms(
                    resolvedCampus, blankToNull(resolvedDate), blankToNull(resolvedStart), blankToNull(resolvedEnd));
            if (!result.isSuccess() || result.getData() == null || result.getData().isEmpty()) {
                return "没有查到符合条件的教室。可换个校区试试。";
            }
            StringBuilder sb = new StringBuilder("教室列表（共 " + result.getData().size() + " 间）：\n");
            for (CasRoomOption r : result.getData()) {
                sb.append("- [教室ID ").append(r.getRoomId()).append("] ").append(r.getName())
                        .append(" · ").append(nullToDash(r.getLocation()))
                        .append(" · 容纳 ").append(r.getSeats() == null ? "未知" : r.getSeats()).append(" 人")
                        .append(" · ").append(nullToDash(r.getCampusName()));
                if (r.getFree() != null) {
                    sb.append(" · ").append(resolvedDate).append(" ").append(resolvedStart).append("-")
                            .append(resolvedEnd).append(Boolean.TRUE.equals(r.getFree()) ? " 空闲" : " 已被占用");
                }
                sb.append("\n");
            }
            remember(BookingSlots.builder()
                    .campus(resolvedCampus).category("space").date(resolvedDate)
                    .startTime(resolvedStart).endTime(resolvedEnd).build());
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("查询教室失败", e);
            return "预约数据服务暂时不可用，请稍后再试。";
        }
    }

    @Tool("查询可借用的设备（投影仪、实验器材等）；传借用窗口可查看该时段还剩多少可借。")
    public String searchEquipment(
            @P("校区：cq=仓前校区，xs=下沙校区。用户没说或不确定时留空字符串") String campus,
            @P("设备名称或分类关键词。不确定时留空字符串") String keyword,
            @P("借用日期 yyyy-MM-dd。不确定时留空字符串") String date,
            @P("开始时间 HH:mm。不确定时留空字符串") String startTime,
            @P("结束时间 HH:mm。不确定时留空字符串") String endTime) {
        String resolvedCampus = normalizeCampus(firstNonBlank(campus, contextCampus()));
        String resolvedDate = firstNonBlank(date, contextDate());
        String resolvedStart = firstNonBlank(startTime, contextStartTime());
        String resolvedEnd = firstNonBlank(endTime, contextEndTime());
        try {
            CasResult<List<CasEquipmentOption>> result = casClient.getAssistantEquipment(
                    resolvedCampus, blankToNull(keyword),
                    blankToNull(resolvedDate), blankToNull(resolvedStart), blankToNull(resolvedEnd));
            if (!result.isSuccess() || result.getData() == null || result.getData().isEmpty()) {
                return "没有查到符合条件的设备。可换个校区或关键词试试。";
            }
            StringBuilder sb = new StringBuilder("设备列表（共 " + result.getData().size() + " 项）：\n");
            for (CasEquipmentOption e : result.getData()) {
                sb.append("- [设备ID ").append(e.getEquipmentId()).append("] ").append(e.getName())
                        .append(" · ").append(nullToDash(e.getCategory()))
                        .append(" · ").append(nullToDash(e.getCampusName()))
                        .append(" · 可借 ").append(e.getAvailableStock() == null ? 0 : e.getAvailableStock())
                        .append(nullToDash(e.getUnit()));
                if (e.getRemainingForWindow() != null) {
                    sb.append(" · 该时段剩余 ").append(e.getRemainingForWindow());
                }
                sb.append("\n");
            }
            remember(BookingSlots.builder()
                    .campus(resolvedCampus).category("equipment").date(resolvedDate)
                    .startTime(resolvedStart).endTime(resolvedEnd).build());
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("查询设备失败", e);
            return "预约数据服务暂时不可用，请稍后再试。";
        }
    }

    @Tool("查询当前登录用户自己的预约记录，可按状态过滤。")
    public String searchMyBookings(
            @P("状态：0=待审核，1=已通过，2=已拒绝，3=已取消，4=已完成。不传则查全部；不确定时传 null") Integer status) {
        try {
            // 不传状态时走精简接口：避免 Feign 把 null 序列化成 `manageStatus=` 造成参数绑定歧义
            CasResult<List<com.kb.infrastructure.client.CasBooking>> result = status == null
                    ? casClient.getMyBookings()
                    : casClient.getMyBookingsByStatus(status);
            if (!result.isSuccess() || result.getData() == null || result.getData().isEmpty()) {
                return "你当前没有符合条件的预约记录。";
            }
            StringBuilder sb = new StringBuilder("你的预约记录（共 " + result.getData().size() + " 条）：\n");
            for (com.kb.infrastructure.client.CasBooking b : result.getData()) {
                sb.append("- [预约号 ").append(b.getOrderId()).append("] ")
                        .append(b.getServiceName())
                        .append("（状态：").append(statusLabel(b.getManageStatus())).append("）");
                if (b.getConsultantName() != null) {
                    sb.append(" · 咨询师 ").append(b.getConsultantName());
                }
                if (b.getRoomName() != null) {
                    sb.append(" · 教室 ").append(b.getRoomName());
                }
                if (b.getEquipmentName() != null) {
                    sb.append(" · 设备 ").append(b.getEquipmentName())
                            .append(b.getQuantity() == null ? "" : " ×" + b.getQuantity());
                }
                if (b.getSlotDate() != null) {
                    sb.append(" · ").append(b.getSlotDate()).append(" ")
                            .append(nullToDash(b.getStartTime())).append("-").append(nullToDash(b.getEndTime()));
                }
                if (b.getReason() != null && !b.getReason().isBlank()) {
                    sb.append(" · 备注：").append(b.getReason());
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("查询我的预约失败", e);
            return "预约数据服务暂时不可用，请稍后再试。";
        }
    }

    // ==================== 动作类工具（均需用户二次确认） ====================

    /**
     * 生成预约草稿。
     * <p>
     * 只做校验与预览，<b>不会真正下单</b>；返回内容里包含"请确认"提示，
     * 并把草稿挂到当前会话，等待用户下一轮明确确认。
     */
    @Tool("当用户明确表示想预约某项校园服务时，生成一份待确认的预约草稿。此操作不会真的下单，必须等用户确认。")
    public String prepareBooking(
            @P("服务ID，来自服务列表") Long serviceId,
            @P("咨询师ID（预约教师咨询时必填，否则传 null）") Long consultantId,
            @P("咨询时段ID（预约教师咨询时必填，否则传 null）") Long slotId,
            @P("教室ID（预约教室时必填，否则传 null）") Long roomId,
            @P("设备ID（借用设备时必填，否则传 null）") Long equipmentId,
            @P("借用数量（设备借用时填，其他场景传 null）") Integer quantity,
            @P("日期 yyyy-MM-dd（教室/设备/咨询场景必填）") String date,
            @P("开始时间 HH:mm（教室/设备场景必填）") String startTime,
            @P("结束时间 HH:mm（教室/设备场景必填）") String endTime,
            @P("用途或主题（可选）") String purpose) {
        if (serviceId == null) {
            return "还不知道要预约哪个服务，请先从服务列表中确定服务ID。";
        }
        CasBookingDraftRequest request = CasBookingDraftRequest.builder()
                .serviceId(serviceId)
                .consultantId(consultantId)
                .slotId(slotId)
                .roomId(roomId)
                .equipmentId(equipmentId)
                .quantity(quantity)
                .date(firstNonBlank(date, contextDate()))
                .startTime(firstNonBlank(startTime, contextStartTime()))
                .endTime(firstNonBlank(endTime, contextEndTime()))
                .purpose(purpose)
                .build();
        try {
            CasResult<CasBookingDraft> result = casClient.createBookingDraft(request);
            if (!result.isSuccess() || result.getData() == null) {
                return "预约草稿生成失败：" + nullToDash(result.getMessage());
            }
            CasBookingDraft draft = result.getData();
            if (Boolean.FALSE.equals(draft.getValid())) {
                return "很抱歉，这个预约暂不可提交：" + nullToDash(draft.getInvalidReason())
                        + "。请换个时间、换个资源或调整数量后重试。";
            }
            PendingBooking pending = PendingBooking.builder()
                    .action(PendingBooking.ACTION_BOOK)
                    .draftId(draft.getDraftId())
                    .resourceType(draft.getResourceType())
                    .resourceTypeName(draft.getResourceTypeName())
                    .serviceId(draft.getServiceId())
                    .serviceName(draft.getServiceName())
                    .resourceId(draft.getResourceId())
                    .resourceName(draft.getResourceName())
                    .campusName(draft.getCampusName())
                    .date(draft.getDate())
                    .startTime(draft.getStartTime())
                    .endTime(draft.getEndTime())
                    .quantity(draft.getQuantity())
                    .needAudit(draft.getNeedAudit())
                    .summary(draft.getSummary())
                    .confirmPrompt(draft.getConfirmPrompt())
                    .expiresAt(draft.getExpiresAt())
                    .build();
            savePending(pending);
            remember(BookingSlots.builder()
                    .serviceId(draft.getServiceId())
                    .campus(draft.getCampus())
                    .category(draft.getCategoryCode())
                    .date(draft.getDate())
                    .startTime(draft.getStartTime())
                    .endTime(draft.getEndTime())
                    .quantity(draft.getQuantity())
                    .build());
            return "已为你生成预约草稿，等待确认：\n" + draft.getSummary() + "\n"
                    + "请向用户复述以上信息，并明确询问「是否确认预约？」。"
                    + "在用户明确答复之前，不要执行任何下单操作。";
        } catch (Exception e) {
            log.error("生成预约草稿失败", e);
            return "预约数据服务暂时不可用，请稍后再试。";
        }
    }

    /**
     * 登记取消意向（同样需要用户确认）
     */
    @Tool("当用户表示想取消某条预约时，登记取消意向。此操作不会真的取消，必须等用户确认。")
    public String requestCancelBooking(
            @P("要取消的预约单号，来自我的预约列表") Long orderId) {
        if (orderId == null) {
            return "还不知道要取消哪一条预约，请先查询我的预约记录确定预约号。";
        }
        PendingBooking pending = PendingBooking.builder()
                .action(PendingBooking.ACTION_CANCEL)
                .orderId(orderId)
                .summary("取消预约单 " + orderId)
                .confirmPrompt("请确认是否取消预约单 " + orderId + "？")
                .build();
        savePending(pending);
        return "已登记取消意向：预约单 " + orderId + "。"
                + "请向用户复述该预约单信息，并明确询问「是否确认取消？」。"
                + "在用户明确答复之前，不要执行任何取消操作。";
    }

    // ==================== 辅助 ====================

    /** 把本轮学到的条件回写到会话槽位，供后续追问继承 */
    private void remember(BookingSlots learned) {
        ChatContextHolder.ChatContext ctx = ChatContextHolder.get();
        if (ctx == null || ctx.sessionId() == null) {
            return;
        }
        ChatSession session = chatSessionRepository.loadOrCreate(ctx.sessionId(), ctx.userId());
        session.setSlots(session.slotsOrEmpty().merge(learned));
        chatSessionRepository.save(session);
        ChatContextHolder.set(new ChatContextHolder.ChatContext(
                ctx.sessionId(), ctx.userId(), session.getSlots()));
    }

    private void savePending(PendingBooking pending) {
        ChatContextHolder.ChatContext ctx = ChatContextHolder.get();
        if (ctx == null || ctx.sessionId() == null) {
            log.warn("缺少会话上下文，无法登记待确认动作");
            return;
        }
        ChatSession session = chatSessionRepository.loadOrCreate(ctx.sessionId(), ctx.userId());
        session.setPendingBooking(pending);
        chatSessionRepository.save(session);
    }

    private String contextCampus() {
        ChatContextHolder.ChatContext ctx = ChatContextHolder.get();
        return ctx == null ? null : ctx.campus();
    }

    private String contextCategory() {
        ChatContextHolder.ChatContext ctx = ChatContextHolder.get();
        return ctx == null ? null : ctx.category();
    }

    private String contextDate() {
        ChatContextHolder.ChatContext ctx = ChatContextHolder.get();
        return ctx == null ? null : ctx.date();
    }

    private String contextStartTime() {
        ChatContextHolder.ChatContext ctx = ChatContextHolder.get();
        return ctx == null ? null : ctx.startTime();
    }

    private String contextEndTime() {
        ChatContextHolder.ChatContext ctx = ChatContextHolder.get();
        return ctx == null ? null : ctx.endTime();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    /** 兼容"仓前校区""cq""CQ"等多种写法 */
    private static String normalizeCampus(String campus) {
        if (campus == null || campus.isBlank()) {
            return null;
        }
        String v = campus.toLowerCase(Locale.ROOT);
        if (v.contains("仓前") || v.startsWith("cq")) {
            return "cq";
        }
        if (v.contains("下沙") || v.startsWith("xs")) {
            return "xs";
        }
        return null;
    }

    private static String nullToDash(String v) {
        return (v == null || v.isBlank()) ? "—" : v;
    }

    private static String statusLabel(Integer status) {
        return switch (status == null ? -1 : status) {
            case 0 -> "待审核";
            case 1 -> "已通过";
            case 2 -> "已拒绝";
            case 3 -> "已取消";
            case 4 -> "已完成";
            default -> "未知";
        };
    }
}
