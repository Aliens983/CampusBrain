package com.kb.domain.chat;

/**
 * 当前请求的会话上下文持有者（线程绑定）
 * <p>
 * LangChain4j 的 {@code @Tool} 是单例 Bean，无法直接接收请求级参数；
 * 这里用线程绑定的方式把「当前 sessionId / userId / 槽位」透传给工具方法，
 * 使工具在 LLM 漏传参数时能回退到会话上下文中已累积的条件。
 * </p>
 * <p>
 * 由 {@code QaApplicationService} 在问答开始时 set、结束时 clear（finally 保证）。
 *
 * @author forever-king
 */
public final class ChatContextHolder {

    private static final ThreadLocal<ChatContext> CONTEXT = new ThreadLocal<>();

    private ChatContextHolder() {
    }

    /**
     * 当前请求的会话上下文
     */
    public record ChatContext(String sessionId, Long userId, BookingSlots slots) {

        public String campus() {
            return slots == null ? null : slots.getCampus();
        }

        public String category() {
            return slots == null ? null : slots.getCategory();
        }

        public String date() {
            return slots == null ? null : slots.getDate();
        }

        public String startTime() {
            return slots == null ? null : slots.getStartTime();
        }

        public String endTime() {
            return slots == null ? null : slots.getEndTime();
        }
    }

    public static void set(ChatContext context) {
        CONTEXT.set(context);
    }

    public static ChatContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
