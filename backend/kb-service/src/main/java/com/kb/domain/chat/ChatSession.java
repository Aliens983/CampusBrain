package com.kb.domain.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 会话上下文（多轮对话的"记忆"）
 * <p>
 * 除持久化的消息历史外，额外维护两类状态：
 * <ol>
 *   <li>{@link BookingSlots} —— 累积的预约条件（校区/分类/日期/时段/资源），用于追问时的指代消解；</li>
 *   <li>{@link PendingBooking} —— 待用户确认的预约草稿，用于"先确认再下单"的闭环。</li>
 * </ol>
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String sessionId;

    private Long userId;

    /** 累积的预约意图槽位 */
    private BookingSlots slots;

    /** 待确认的预约草稿（无则为 null） */
    private PendingBooking pendingBooking;

    /** 最近一次更新时间（毫秒） */
    private long updatedAt;

    public static ChatSession create(String sessionId, Long userId) {
        return ChatSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .slots(new BookingSlots())
                .updatedAt(System.currentTimeMillis())
                .build();
    }

    public BookingSlots slotsOrEmpty() {
        return slots == null ? new BookingSlots() : slots;
    }
}
