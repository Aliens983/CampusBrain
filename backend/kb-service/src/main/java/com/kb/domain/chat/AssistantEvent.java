package com.kb.domain.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 助手向前端推送的结构化事件（SSE）
 * <p>
 * 除答案 token 与引用外，助手还需要把"当前理解的预约条件"与
 * "待确认的预约草稿"告知前端，以便渲染上下文条和确认卡片。
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantEvent {

    /** 当前槽位已更新 */
    public static final String TYPE_SLOTS = "slots";

    /** 有待用户确认的预约草稿 */
    public static final String TYPE_CONFIRM = "confirm";

    /** 已执行的预约动作结果 */
    public static final String TYPE_ACTION = "action";

    /** 事件类型：slots / confirm / action */
    private String type;

    /** 事件负载 */
    private Object payload;

    public static AssistantEvent slots(BookingSlots slots) {
        return AssistantEvent.builder().type(TYPE_SLOTS).payload(slots).build();
    }

    public static AssistantEvent confirm(PendingBooking draft) {
        return AssistantEvent.builder().type(TYPE_CONFIRM).payload(draft).build();
    }

    public static AssistantEvent action(Object result) {
        return AssistantEvent.builder().type(TYPE_ACTION).payload(result).build();
    }
}
