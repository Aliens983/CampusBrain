package com.kb.infrastructure.rag.tool;

import com.kb.domain.chat.BookingSlots;
import com.kb.domain.chat.ChatContextHolder;
import com.kb.domain.chat.ChatSession;
import com.kb.domain.chat.ChatSessionRepository;
import com.kb.domain.chat.PendingBooking;
import com.kb.infrastructure.client.CasClient;
import com.kb.infrastructure.client.CasResult;
import com.kb.infrastructure.client.dto.CasConsultantOption;
import com.kb.infrastructure.client.dto.CasTimeSlot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 教师咨询预约的<b>确定性兜底编排</b>。
 * <p>
 * 真实故障（2026-10-02）：deepseek-chat 对「帮我预约 X 老师…」这类动作式指令会
 * 系统性地只回一句英文承诺（{@code I'll look up ...}）而不发起任何 function call，
 * 首轮 + 强制重试两轮均如此，最终只能给用户「查询失败」兜底。查询式问题（「有哪些教师」）
 * 则能正常调工具——问题只出在动作式指令上，提示词难以稳定纠正。
 * </p>
 * <p>
 * 本类在「两轮模型都零工具空承诺」后接管，仅处理槽位已齐全的教师咨询预约，
 * 服务端直连 CAS 走完 查咨询师 → 查时段 → 生成待确认草稿（仍需用户二次确认，不直接下单），
 * 全程不依赖模型，保证链路必有确定性结果。其他场景一律返回 {@code null}，交回原有兜底。
 * </p>
 *
 * @author forever-king
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeterministicBookingFallback {

    private final CasClient casClient;
    private final AppointmentTool appointmentTool;
    private final ChatSessionRepository chatSessionRepository;

    /**
     * 教师称呼前向回溯时的停用字：时间/日期/校区/语气等不可能出现在姓名里的字。
     * 用于把「下沙校区姚老师」中的「姚」、「9点欧阳老师」中的「欧阳」正确切出来。
     */
    private static final String NAME_STOP_CHARS =
            "校区园楼房楼层的上下上午晚早今明天日号月年周星期前后点分鐘钟整点半一二两三四五六七八九十";

    /**
     * 尝试确定性地完成教师咨询预约准备。
     *
     * @param query 用户原话（用于抽取教师姓名）
     * @return 直接下发给用户的中文文本；null 表示本兜底不适用，调用方走原有兜底文案
     */
    public String tryPrepareTeacherBooking(String query) {
        ChatContextHolder.ChatContext ctx = ChatContextHolder.get();
        BookingSlots slots = ctx == null ? null : ctx.slots();
        // 仅处理槽位齐全的教师咨询；缺参场景由 ToolCallGuard.slotClarification 反问
        if (slots == null || !"teacher".equals(slots.getCategory())
                || isBlank(slots.getCampus()) || isBlank(slots.getDate())
                || isBlank(slots.getStartTime()) || isBlank(slots.getEndTime())) {
            return null;
        }
        String campus = slots.getCampus();
        String date = slots.getDate();
        String start = slots.getStartTime();
        String end = slots.getEndTime();
        String campusName = BookingSlots.campusName(campus);

        // 1. 查询咨询师并唯一锁定
        String keyword = extractTeacherName(query);
        if (keyword == null) {
            keyword = slots.getKeyword();
        }
        CasResult<List<CasConsultantOption>> result =
                casClient.getAssistantConsultants(campus, keyword, date);
        if (!result.isSuccess()) {
            // CAS 明确不可用时透传其用户可读提示（如「预约服务暂不可用」），比通用兜底更具体
            return result.getMessage();
        }
        List<CasConsultantOption> consultants = result.getData();
        if (consultants == null || consultants.isEmpty()) {
            String who = keyword == null ? "" : "「" + keyword + "」相关的";
            return "我在" + campusName + "没有查到" + who
                    + "可预约咨询师" + weekendSuffix(date)
                    + "。请确认老师姓名或校区后再告诉我。";
        }

        CasConsultantOption consultant = pickConsultant(consultants, slots.getConsultantId(), keyword);
        if (consultant == null) {
            // 多人重名/无法唯一确定：列清楚让用户选，不臆测
            StringBuilder sb = new StringBuilder("查到多位相关老师，请告诉我您想约哪一位：\n");
            for (CasConsultantOption c : consultants) {
                sb.append("- ").append(c.getName());
                if (c.getDepartment() != null && !c.getDepartment().isBlank()) {
                    sb.append("（").append(c.getDepartment()).append("）");
                }
                if (c.getServiceName() != null && !c.getServiceName().isBlank()) {
                    sb.append(" · ").append(c.getServiceName());
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        }
        if (consultant.getServiceId() == null) {
            log.warn("确定性预约兜底：咨询师 {} 缺少 serviceId，无法生成草稿", consultant.getConsultantId());
            return null;
        }

        // 2. 查询当天时段，锁定与槽位匹配的一条
        CasResult<List<CasTimeSlot>> slotResult =
                casClient.getConsultantSlots(consultant.getConsultantId(), date);
        if (!slotResult.isSuccess()) {
            return slotResult.getMessage();
        }
        List<CasTimeSlot> daySlots = slotResult.getData();
        if (daySlots == null || daySlots.isEmpty()) {
            return displayName(consultant.getName()) + "在 " + date + " 没有可预约时段"
                    + weekendSuffix(date) + "，您可以换个工作日日期再约。";
        }
        CasTimeSlot target = null;
        for (CasTimeSlot t : daySlots) {
            if (start.equals(t.getStartTime()) && end.equals(t.getEndTime())) {
                target = t;
                break;
            }
        }
        if (target == null) {
            List<String> options = daySlots.stream()
                    .map(t -> t.getStartTime() + "-" + t.getEndTime())
                    .toList();
            return displayName(consultant.getName()) + "在 " + date + " 没有 " + start + "-" + end
                    + " 这个时段，当天可约的是：" + String.join("、", options)
                    + "。请问您想约哪个时段？";
        }

        // 3. 生成待确认草稿（prepareBooking 只做预览并把草稿挂到会话，不会真正下单）
        String toolReturn = appointmentTool.prepareBooking(
                consultant.getServiceId(), consultant.getConsultantId(), target.getSlotId(),
                null, null, null, date, start, end, null);

        PendingBooking pending = loadPending(ctx);
        if (pending == null) {
            // CAS 业务失败（非 200 / 草稿校验不通过）时 prepareBooking 返回用户可读中文原因
            log.warn("确定性预约兜底：草稿未生成，toolReturn={}", toolReturn);
            return toolReturn;
        }
        log.info("模型两轮未调工具，确定性兜底已生成预约草稿: consultant={}, slot={}, date={}",
                consultant.getConsultantId(), target.getSlotId(), date);
        return "好的，已为您准备好预约确认单：\n" + pending.getSummary()
                + "\n请确认是否提交预约（回复「确认」/「取消」，或点击下方卡片按钮）。";
    }

    /**
     * 在候选列表中唯一锁定一位咨询师：优先会话已锁定的 ID，其次姓名包含关键词且唯一。
     *
     * @return 唯一确定的咨询师；无法唯一确定时返回 null（由调用方追问）
     */
    private CasConsultantOption pickConsultant(List<CasConsultantOption> consultants,
                                                Long lockedConsultantId, String keyword) {
        if (lockedConsultantId != null) {
            for (CasConsultantOption c : consultants) {
                if (lockedConsultantId.equals(c.getConsultantId())) {
                    return c;
                }
            }
        }
        if (isBlank(keyword)) {
            return consultants.size() == 1 ? consultants.get(0) : null;
        }
        List<CasConsultantOption> matched = consultants.stream()
                .filter(c -> c.getName() != null && c.getName().contains(keyword))
                .toList();
        if (matched.size() == 1) {
            return matched.get(0);
        }
        if (matched.isEmpty() && consultants.size() == 1) {
            // 关键词未命中姓名但全校区当天仅此一位（如口语化称呼），可安全锁定
            return consultants.get(0);
        }
        return null;
    }

    private PendingBooking loadPending(ChatContextHolder.ChatContext ctx) {
        if (ctx.sessionId() == null) {
            return null;
        }
        ChatSession session = chatSessionRepository.loadForUser(ctx.sessionId(), ctx.userId());
        return session == null ? null : session.getPendingBooking();
    }

    /**
     * 抽取「X老师」中的姓名关键词：定位「老师」后从其前一个汉字向前回溯，
     * 最多取 4 个汉字，遇到时间/校区等停用字即止（如「下沙校区姚老师」→「姚」，
     * 「欧阳老师」→「欧阳」）；抽不到返回 null。
     */
    static String extractTeacherName(String query) {
        if (query == null) {
            return null;
        }
        int idx = query.indexOf("老师");
        StringBuilder name = new StringBuilder();
        while (idx > 0 && name.length() < 4) {
            char ch = query.charAt(idx - 1);
            if (ch < '一' || ch > '龥' || NAME_STOP_CHARS.indexOf(ch) >= 0) {
                break;
            }
            name.insert(0, ch);
            idx--;
        }
        return name.length() > 0 ? name.toString() : null;
    }

    /** 日期为周末时附加说明：0 时段是不排班而非约满 */
    private static String weekendSuffix(String date) {
        try {
            DayOfWeek dow = LocalDate.parse(date).getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                return "（" + date + " 是周末，教师咨询仅工作日排班，本来就不排班）";
            }
        } catch (DateTimeParseException ignored) {
            // 日期格式异常时不附加提示
        }
        return "";
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** 咨询师姓名库本身可能已带「老师」后缀，避免拼接出「姚老师老师」 */
    private static String displayName(String name) {
        return name == null ? "该老师" : (name.endsWith("老师") ? name : name + "老师");
    }
}
