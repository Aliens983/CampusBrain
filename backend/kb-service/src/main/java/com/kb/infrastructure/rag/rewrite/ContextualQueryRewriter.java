package com.kb.infrastructure.rag.rewrite;

import com.kb.domain.chat.BookingSlots;
import com.kb.domain.rag.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 上下文感知的查询改写器 —— 多轮对话的核心
 * <p>
 * 双策略：
 * <ol>
 *   <li><b>规则优先</b>：当本轮只补充了部分条件、其余可从历史槽位继承时
 *       （典型如"换成下沙校区呢？"），直接把继承来的条件拼成独立问题。
 *       确定性高、不依赖 LLM，未配 Key 也能正确理解上下文。</li>
 *   <li><b>LLM 兜底</b>：规则判断不出是追问时（如纯代词指代"它怎么样"），
 *       交给 {@link QueryRewriter} 做自然语言指代消解。</li>
 * </ol>
 *
 * <p>示例：
 * <pre>
 * 第 1 轮：仓前校区上午9-10点是否有容量预约教师
 *         → 槽位 {campus=cq, category=teacher, time=09:00-10:00}
 * 第 2 轮：换成下沙校区呢？
 *         → 本轮只抽到 campus=xs，继承 category/date/time
 *         → 改写为「换成下沙校区呢？（预约条件：校区：下沙校区；类型：教师咨询；时段：09:00-10:00）」
 * </pre>
 *
 * @author forever-king
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContextualQueryRewriter {

    private final QueryRewriter llmRewriter;

    /**
     * 改写结果
     *
     * @param query          改写后的独立问题（用于检索与提问）
     * @param slots          合并后的完整槽位（用于查询预约系统与回写会话）
     * @param contextApplied 是否由历史槽位补全（true 表示这是一次追问）
     */
    public record RewriteResult(String query, BookingSlots slots, boolean contextApplied) {
    }

    public RewriteResult rewrite(String rawQuery,
                                 List<LlmService.ChatMessage> history,
                                 BookingSlots previous) {
        BookingSlots base = previous == null ? new BookingSlots() : previous;
        BookingSlots extracted = BookingSlotExtractor.extract(rawQuery);
        BookingSlots merged = base.merge(extracted);

        if (isFollowUp(extracted, base)) {
            String standalone = composeStandalone(rawQuery, merged);
            log.debug("上下文补全（规则）: [{}] -> [{}]", rawQuery, standalone);
            return new RewriteResult(standalone, merged, true);
        }

        if (history != null && !history.isEmpty()) {
            String llmRewritten = llmRewriter.rewrite(rawQuery, history);
            if (llmRewritten != null && !llmRewritten.isBlank() && !llmRewritten.equals(rawQuery)) {
                log.debug("上下文补全（LLM）: [{}] -> [{}]", rawQuery, llmRewritten);
                return new RewriteResult(llmRewritten, merged, false);
            }
        }
        return new RewriteResult(rawQuery, merged, false);
    }

    /**
     * 判定为"追问"：本轮确实补充了新条件，且至少有一个维度需要沿用历史槽位
     */
    private boolean isFollowUp(BookingSlots extracted, BookingSlots previous) {
        if (previous.isEmpty() || extracted.isEmpty()) {
            return false;
        }
        return (extracted.getCampus() == null && previous.getCampus() != null)
                || (extracted.getCategory() == null && previous.getCategory() != null)
                || (extracted.getDate() == null && previous.getDate() != null)
                || (extracted.getStartTime() == null && previous.getStartTime() != null)
                || (extracted.getServiceId() == null && previous.getServiceId() != null)
                || (extracted.getConsultantId() == null && previous.getConsultantId() != null)
                || (extracted.getRoomId() == null && previous.getRoomId() != null)
                || (extracted.getEquipmentId() == null && previous.getEquipmentId() != null);
    }

    /**
     * 把继承来的条件拼到原话后面，形成可独立理解的查询
     */
    private String composeStandalone(String rawQuery, BookingSlots merged) {
        String describe = merged.describe();
        if (describe == null || describe.isBlank()) {
            return rawQuery;
        }
        return rawQuery.trim() + "（预约条件：" + describe + "）";
    }
}
