package com.kb.application.service;

import com.kb.domain.chat.AssistantEvent;
import com.kb.domain.chat.BookingSlots;
import com.kb.domain.chat.ChatSession;
import com.kb.domain.chat.ChatSessionRepository;
import com.kb.domain.chat.PendingBooking;
import com.kb.domain.conversation.Conversation;
import com.kb.domain.conversation.ConversationRepository;
import com.kb.infrastructure.client.CasClient;
import com.kb.infrastructure.client.CasResult;
import com.kb.infrastructure.client.dto.CasBookingResult;
import com.kb.infrastructure.metrics.BusinessMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * 待确认预约动作执行器（Q-01 从 QaApplicationService 拆出）。
 * <p>
 * 预约采用两段式：工具只生成草稿，检测到用户"确认/取消"后由本组件真正调用
 * CAS 下单/丢弃，保证 AI 不会擅自替用户预约。确认/取消意图的词法判定也归本组件，
 * 与拆出前的行为完全一致（含 3.3.3 的"应答词 + 新请求分句"排除规则）。
 * </p>
 *
 * @author forever-king
 */
@Component
@RequiredArgsConstructor
public class PendingBookingExecutor {

    private final CasClient casClient;
    private final ChatSessionRepository chatSessionRepository;
    private final ConversationRepository conversationRepository;
    private final BusinessMetrics metrics;

    /** 用户对本轮"是否确认"的答复 */
    public enum ConfirmIntent { CONFIRM, REJECT, NONE }

    /**
     * 判定"确认"的关键词。刻意只收多字词：单字（如"行""对"）在中文里歧义太大，
     * 容易把"换个时间行不行"误判成确认。
     * <p>
     * 3.3.3：已移除"提交"——它高度歧义（"怎么提交？""提交按钮在哪"是疑问句而非确认），
     * 确认提交由"确认/确定/好的/预约吧/帮我约"等明确表达承担；{@link #detectConfirmIntent}
     * 另对"好的，帮我查余量"这类"应答词 + 新请求分句"做排除。
     */
    private static final List<String> POSITIVE_WORDS = List.of(
            "确认", "确定", "是的", "好的", "可以", "没问题", "就这样", "就这个", "就按",
            "预约吧", "帮我约", "帮我订", "ok", "yes");

    /**
     * 判定"取消"的关键词，其中"不确认/不确定/不行"必须排在肯定词之前判断。
     */
    private static final List<String> NEGATIVE_WORDS = List.of(
            "不确认", "不确定", "不用", "不要", "不是", "不行", "取消", "算了", "不约",
            "放弃", "先别", "别了", "再想想", "拒绝", "no");

    /**
     * 3.3.3：应答词之后若接上这些"新请求"信号，说明用户是在借应答口吻发起另一个问题，
     * 而不是确认当前草稿（如"好的，怎么预约？""可以，帮我查下还有多少名额"）。
     */
    private static final List<String> FOLLOW_UP_MARKERS = List.of(
            "怎么", "如何", "请问", "帮我查", "查一下", "查下", "看看", "能不能",
            "可不可以", "多少", "哪里", "哪儿", "还有", "换一个", "换个", "再说");

    /** 纯应答词（仅当它们作为开头、后面又跟了新请求时，才需要排除误判） */
    private static final List<String> ACK_PREFIXES = List.of(
            "好的", "好吧", "是的", "可以", "没问题", "确定", "确认", "ok", "yes");

    /**
     * 判定用户本轮输入是确认、取消还是无关话题（交给后续正常问答理解）。
     */
    public ConfirmIntent detectConfirmIntent(String query) {
        if (query == null || query.isBlank()) {
            return ConfirmIntent.NONE;
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        // 长句基本不是简单的确认/取消，交给 LLM 正常理解
        if (q.length() > 30) {
            return ConfirmIntent.NONE;
        }
        // 先判否定：避免"不确认""不用了"被肯定词命中
        for (String w : NEGATIVE_WORDS) {
            if (q.contains(w)) {
                return ConfirmIntent.REJECT;
            }
        }
        for (String w : POSITIVE_WORDS) {
            if (q.contains(w)) {
                // 3.3.3："好的/可以 + 后续新请求小句"不判确认（形如"好的，怎么预约？"）；
                // 但"好的帮我约/确认预约吧"这类仍含明确下单动作的，保持判定为确认
                if (isAckFollowedByNewRequest(q)) {
                    return ConfirmIntent.NONE;
                }
                return ConfirmIntent.CONFIRM;
            }
        }
        return ConfirmIntent.NONE;
    }

    /**
     * 3.3.3：判断是否"应答词开头 + 另起的新请求"。
     * 条件：以纯应答词开头，其后既非空、也不是明确下单动作（约/订），
     * 且出现分句标点或新请求标志词。
     */
    private boolean isAckFollowedByNewRequest(String q) {
        for (String ack : ACK_PREFIXES) {
            if (!q.startsWith(ack)) {
                continue;
            }
            String rest = q.substring(ack.length()).trim();
            if (rest.isEmpty()) {
                return false;
            }
            // 明确下单动作仍按确认处理
            if (rest.contains("约") || rest.contains("订")) {
                return false;
            }
            boolean hasClauseBreak = rest.contains("，") || rest.contains(",")
                    || rest.contains("。") || rest.contains("?") || rest.contains("？")
                    || rest.contains("!") || rest.contains("！");
            boolean hasFollowUpWord = FOLLOW_UP_MARKERS.stream().anyMatch(rest::contains);
            return hasClauseBreak || hasFollowUpWord;
        }
        return false;
    }

    /**
     * 执行待确认动作：用户确认则真正调 CAS 下单/取消，否则放弃草稿。
     * 收尾（清空待确认、可选清槽位、推送事件、落库）与拆出前完全一致。
     */
    public String executePending(String sid, Long userId, String query, ChatSession session,
                                 PendingBooking pending, boolean confirmed,
                                 Consumer<String> onToken,
                                 Consumer<List<Conversation.CitationRef>> onCitations,
                                 Consumer<Long> onMessageId,
                                 Consumer<AssistantEvent> onEvent,
                                 long startTime) {
        conversationRepository.save(sid, "user", query, userId);

        String answer;
        CasResult<CasBookingResult> result = null;
        if (!confirmed) {
            // 放弃：BOOK 动作需要通知 CAS 丢弃草稿，CANCEL 动作本来就还没执行。
            // CAS 不可用时 fallback 返回 503 结果（放弃草稿是尽力而为的清理动作，忽略结果）。
            if (PendingBooking.ACTION_BOOK.equals(pending.getAction()) && pending.getDraftId() != null) {
                casClient.discardBookingDraft(pending.getDraftId());
            }
            answer = "好的，已取消本次操作，没有产生任何变更。"
                    + "如果你想换时间、换校区或换其他服务，直接告诉我就行。";
        } else {
            result = PendingBooking.ACTION_CANCEL.equals(pending.getAction())
                    ? casClient.cancelBooking(pending.getOrderId())
                    : casClient.confirmBookingDraft(pending.getDraftId());
            if (result == null) {
                answer = "操作失败：预约服务暂时不可用，请稍后再试。";
            } else if (result.isSuccess() && result.getData() != null) {
                answer = result.getData().getMessage();
            } else {
                answer = "操作失败：" + (result.getMessage() == null ? "预约服务返回异常" : result.getMessage());
            }
        }

        session.setPendingBooking(null);
        // 预约完成后清空槽位，避免下一次提问沿用已完成的预约条件
        if (confirmed) {
            session.setSlots(new BookingSlots());
        }
        chatSessionRepository.save(session);

        if (onToken != null) {
            onToken.accept(answer);
        }
        if (onEvent != null) {
            if (confirmed && result != null && result.getData() != null) {
                onEvent.accept(AssistantEvent.action(result.getData()));
            }
            onEvent.accept(AssistantEvent.slots(session.slotsOrEmpty()));
        }
        if (onCitations != null) {
            onCitations.accept(List.of());
        }

        Long messageId = conversationRepository.saveWithReferences(
                sid, "assistant", answer, List.of(), userId);
        if (onMessageId != null && messageId != null) {
            onMessageId.accept(messageId);
        }

        metrics.recordQaRequest();
        metrics.recordQaLatency(System.currentTimeMillis() - startTime);
        return answer;
    }

    /**
     * 用户转移话题时丢弃上一份草稿（尽力而为的 CAS 清理），避免后续误确认。
     */
    public void discardPending(ChatSession session, PendingBooking pending) {
        if (PendingBooking.ACTION_BOOK.equals(pending.getAction()) && pending.getDraftId() != null) {
            // 尽力而为的清理：CAS 不可用时 fallbackFactory 内部已记录原因
            casClient.discardBookingDraft(pending.getDraftId());
        }
        session.setPendingBooking(null);
        chatSessionRepository.save(session);
    }
}