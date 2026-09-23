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
     * 判定"确认"的关键词。刻意只收高置信词，并配合{@link #QUESTION_MARKERS}与{@link #REQUEST_ACTION_MARKERS}
     * 双重拦截，杜绝误下单（3.3 深度审查 P0）：
     * <ul>
     *   <li>3.3.3 已移除"提交"这类歧义词；</li>
     *   <li>3.3 再移除"可以"（"明天可以预约吗"含"可以"曾被误判为确认）；</li>
     *   <li>英文词 ok/yes 采用整词匹配，避免 "book a room" 命中 "ok"。</li>
     * </ul>
     */
    private static final List<String> POSITIVE_WORDS = List.of(
            "确认", "确定", "好的", "是的", "没问题", "就这样", "就这个", "就按",
            "预约吧", "帮我约", "帮我订", "ok", "yes");

    /**
     * 判定"取消"的关键词。英文 no 采用整词匹配，避免 "I want to know" 命中 "no"。
     */
    private static final List<String> NEGATIVE_WORDS = List.of(
            "不确认", "不确定", "不用", "不要", "不是", "不行", "取消", "算了", "不约",
            "放弃", "先别", "别了", "再想想", "拒绝", "no");

    /**
     * 3.3（深度审查 P0）：疑问标志。命中即不是确认/取消——"明天可以预约吗"、"能帮我查下余量吗"
     * 都是询问而非应答，先于否定/肯定判定，避免把草稿误下单或误丢弃。
     */
    private static final List<String> QUESTION_MARKERS = List.of(
            "吗", "呢", "？", "?", "能不能", "可不可以", "是否可以", "怎么", "如何",
            "多少", "哪些", "什么时候", "几点", "有没有", "能否");

    /**
     * 3.3（深度审查 P0）：请求动作标志。q 以这些词开头且不含明确下单动作（约/订/提交/下单）时，
     * 是"请帮我处理某事"的询问而非确认——"帮我确定一下时间"不能触发真实下单。
     */
    private static final List<String> REQUEST_ACTION_MARKERS = List.of(
            "帮我", "麻烦", "请教", "请问", "帮忙");

    /**
     * 3.3：超过 30 字的"长确认句"（如"好的，我确认这个预约，请帮我提交"）不再一律判 NONE，
     * 只要含明确下单动作且非疑问仍判确认，避免草稿被误销毁；否则交给 LLM。
     */
    private static final List<String> STRONG_CONFIRM_LONG = List.of(
            "确认预约", "确定预约", "提交预约", "帮我提交", "帮我约", "帮我订", "预约吧", "下单");

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
     * <p>
     * 3.3（深度审查 P0）修复：疑问/请求-动作双重拦截 + 英文整词匹配 + 长句强确认，
     * 从根上消除"帮用户擅自下单/取消"的误判面。
     */
    public ConfirmIntent detectConfirmIntent(String query) {
        if (query == null || query.isBlank()) {
            return ConfirmIntent.NONE;
        }
        String q = query.trim().toLowerCase(Locale.ROOT);

        // 3.3：疑问句一律不判确认/取消（"明天可以预约吗"、"怎么确认"、"能否改期"）
        if (QUESTION_MARKERS.stream().anyMatch(q::contains)) {
            return ConfirmIntent.NONE;
        }

        // 3.3：请求-动作句（"帮我确定一下时间"）不判确认，除非含明确下单动作
        if (REQUEST_ACTION_MARKERS.stream().anyMatch(q::startsWith)) {
            if (q.contains("约") || q.contains("订") || q.contains("提交") || q.contains("下单")) {
                return ConfirmIntent.CONFIRM;
            }
            return ConfirmIntent.NONE;
        }

        // 长句基本不是简单的确认/取消，交给 LLM 正常理解；但含明确下单动作的确认长句放行
        if (q.length() > 30) {
            return STRONG_CONFIRM_LONG.stream().anyMatch(q::contains)
                    ? ConfirmIntent.CONFIRM : ConfirmIntent.NONE;
        }

        // 先判否定：避免"不确认""不用了"被肯定词命中
        for (String w : NEGATIVE_WORDS) {
            if (matchesWord(q, w)) {
                return ConfirmIntent.REJECT;
            }
        }
        for (String w : POSITIVE_WORDS) {
            if (matchesWord(q, w)) {
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

    /** 英文短词整词匹配（避免 "book a room" 命中 "ok"、"know" 命中 "no"），中文直接 contains */
    private static boolean matchesWord(String q, String word) {
        if (word.matches("[a-zA-Z]+")) {
            return q.matches(".*\\b" + word + "\\b.*");
        }
        return q.contains(word);
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
        // 3.3（深度审查 P0 附带）：草稿已过期（如确认消息被长时间延迟/客户端断连后补发）绝不能再下单，
        // 直接丢弃并提示，杜绝"过期草稿仍被真实执行"的竞态。
        if (pending.isExpired()) {
            discardPending(session, pending);
            return "您刚才的预约操作已过期，请重新发起预约。";
        }
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
                answer = "操作失败：" + (result.getMessage() == null ? "预约服务返回异常" : result.getMessage())
                        + "。请稍后重新回复「确认」再试一次。";
            }
        }

        // CAS 调用失败（503 不可用 / 业务失败）时不能清空 pending：此前失败也落 null，
        // 用户再次「确认」时已无待执行动作，只能从头重填整张预约单。改为保留 pending
        // （仍受 10 分钟 TTL 约束）与槽位，用户可直接再回复「确认」重试；
        // 仅用户主动放弃 或 CAS 真正成功才收尾清空。
        boolean casSucceeded = confirmed
                && result != null && result.isSuccess() && result.getData() != null;
        if (!confirmed || casSucceeded) {
            session.setPendingBooking(null);
            // 预约完成后清空槽位，避免下一次提问沿用已完成的预约条件；
            // 失败重试路径保留槽位，重新确认时沿用原预约条件
            if (confirmed) {
                session.setSlots(new BookingSlots());
            }
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