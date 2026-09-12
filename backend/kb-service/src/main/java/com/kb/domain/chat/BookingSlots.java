package com.kb.domain.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 预约意图槽位（Slot Filling）
 * <p>
 * 多轮对话中，用户往往只补充变化的那一个条件（"换成下沙校区呢？"、"那明天呢？"）。
 * 本类负责把这些零散条件累积成一个完整的预约意图，
 * 使助手在追问时无需用户重复已说过的信息。
 * </p>
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingSlots implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 校区编码 cq仓前 / xs下沙 */
    private String campus;

    /** 业务分类 teacher教师咨询 / equipment设备借用 / space教室空间 / activity活动报名 */
    private String category;

    /** 日期 yyyy-MM-dd */
    private String date;

    /** 开始时间 HH:mm */
    private String startTime;

    /** 结束时间 HH:mm */
    private String endTime;

    /** 服务ID（已锁定到具体服务时） */
    private Long serviceId;

    /** 咨询师ID */
    private Long consultantId;

    /** 咨询时段ID */
    private Long slotId;

    /** 教室ID */
    private Long roomId;

    /** 设备ID */
    private Long equipmentId;

    /** 借用数量 */
    private Integer quantity;

    /** 关键词（服务/咨询师/设备名） */
    private String keyword;

    /**
     * 用新一轮抽取出的槽位合并进当前槽位
     * <p>
     * 规则：新一轮非空的字段覆盖旧值；新一轮为空的字段继承旧值。
     * 这样"换成下沙校区呢？"只需抽到 campus=xs，日期与时段自动沿用上一轮。
     *
     * @param newer 本轮新抽取的槽位（可为 null）
     * @return 合并后的新对象（不修改原对象）
     */
    public BookingSlots merge(BookingSlots newer) {
        if (newer == null) {
            return copy();
        }
        BookingSlots merged = copy();
        if (newer.campus != null) merged.campus = newer.campus;
        if (newer.category != null) merged.category = newer.category;
        if (newer.date != null) merged.date = newer.date;
        if (newer.startTime != null) merged.startTime = newer.startTime;
        if (newer.endTime != null) merged.endTime = newer.endTime;
        if (newer.serviceId != null) merged.serviceId = newer.serviceId;
        if (newer.consultantId != null) merged.consultantId = newer.consultantId;
        if (newer.slotId != null) merged.slotId = newer.slotId;
        if (newer.roomId != null) merged.roomId = newer.roomId;
        if (newer.equipmentId != null) merged.equipmentId = newer.equipmentId;
        if (newer.quantity != null) merged.quantity = newer.quantity;
        if (newer.keyword != null) merged.keyword = newer.keyword;
        return merged;
    }

    public BookingSlots copy() {
        return BookingSlots.builder()
                .campus(campus).category(category).date(date)
                .startTime(startTime).endTime(endTime)
                .serviceId(serviceId).consultantId(consultantId).slotId(slotId)
                .roomId(roomId).equipmentId(equipmentId)
                .quantity(quantity).keyword(keyword)
                .build();
    }

    public boolean isEmpty() {
        return campus == null && category == null && date == null
                && startTime == null && endTime == null
                && serviceId == null && consultantId == null && slotId == null
                && roomId == null && equipmentId == null && quantity == null
                && keyword == null;
    }

    /** 资源是否已锁定到具体条目（咨询师/教室/设备） */
    public boolean hasResource() {
        return consultantId != null || roomId != null || equipmentId != null;
    }

    /**
     * 生成人类可读的上下文摘要，注入给 LLM 与展示给前端
     */
    public String describe() {
        List<String> parts = new ArrayList<>();
        if (campus != null) {
            parts.add("校区：" + campusName(campus));
        }
        if (category != null) {
            parts.add("类型：" + categoryName(category));
        }
        if (serviceId != null) {
            parts.add("服务ID：" + serviceId);
        }
        if (consultantId != null) {
            parts.add("咨询师ID：" + consultantId);
        }
        if (roomId != null) {
            parts.add("教室ID：" + roomId);
        }
        if (equipmentId != null) {
            parts.add("设备ID：" + equipmentId + (quantity != null ? " ×" + quantity : ""));
        }
        if (date != null) {
            parts.add("日期：" + date);
        }
        if (startTime != null && endTime != null) {
            parts.add("时段：" + startTime + "-" + endTime);
        } else if (startTime != null) {
            parts.add("开始：" + startTime);
        }
        if (keyword != null) {
            parts.add("关键词：" + keyword);
        }
        return String.join("；", parts);
    }

    public static String campusName(String campus) {
        if (campus == null) {
            return null;
        }
        return switch (campus.toLowerCase(Locale.ROOT)) {
            case "cq" -> "仓前校区";
            case "xs" -> "下沙校区";
            default -> campus;
        };
    }

    public static String categoryName(String category) {
        if (category == null) {
            return null;
        }
        return switch (category.toLowerCase(Locale.ROOT)) {
            case "teacher" -> "教师咨询";
            case "equipment" -> "设备借用";
            case "space" -> "教室空间";
            case "activity" -> "活动报名";
            default -> category;
        };
    }
}
