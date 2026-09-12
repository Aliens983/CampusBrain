package com.kb.infrastructure.rag.rewrite;

import com.kb.domain.chat.BookingSlots;
import com.kb.domain.rag.LlmService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 上下文感知改写器测试 —— 多轮对话的核心行为
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("上下文感知查询改写器")
class ContextualQueryRewriterTest {

    @Mock private QueryRewriter llmRewriter;

    @InjectMocks private ContextualQueryRewriter rewriter;

    @Test
    @DisplayName("首轮提问：抽全条件，不触发上下文补全")
    void shouldExtractSlotsOnFirstTurn() {
        ContextualQueryRewriter.RewriteResult result =
                rewriter.rewrite("仓前校区上午9-10点是否有容量预约教师", List.of(), new BookingSlots());

        assertFalse(result.contextApplied());
        assertEquals("cq", result.slots().getCampus());
        assertEquals("teacher", result.slots().getCategory());
        assertEquals("09:00", result.slots().getStartTime());
        assertEquals("10:00", result.slots().getEndTime());
    }

    @Test
    @DisplayName("追问换校区：只改校区，继承分类与时段（核心场景）")
    void shouldInheritCategoryAndTimeWhenCampusChanges() {
        BookingSlots previous = BookingSlots.builder()
                .campus("cq").category("teacher").startTime("09:00").endTime("10:00")
                .build();

        ContextualQueryRewriter.RewriteResult result =
                rewriter.rewrite("换成下沙校区呢？", List.of(), previous);

        assertTrue(result.contextApplied(), "应识别为追问并继承历史条件");
        assertEquals("xs", result.slots().getCampus(), "校区应更新为下沙");
        assertEquals("teacher", result.slots().getCategory(), "分类应继承上一轮");
        assertEquals("09:00", result.slots().getStartTime(), "时段应继承上一轮");
        assertEquals("10:00", result.slots().getEndTime());
        assertTrue(result.query().contains("下沙校区"), "改写后的问题应带上继承的条件");
        assertTrue(result.query().contains("09:00-10:00"));
    }

    @Test
    @DisplayName("追问换日期：继承校区与时段")
    void shouldInheritWhenDateChanges() {
        BookingSlots previous = BookingSlots.builder()
                .campus("cq").category("teacher").startTime("09:00").endTime("10:00")
                .build();

        ContextualQueryRewriter.RewriteResult result =
                rewriter.rewrite("那明天呢？", List.of(), previous);

        assertTrue(result.contextApplied());
        assertEquals("cq", result.slots().getCampus());
        assertEquals("09:00", result.slots().getStartTime());
        assertTrue(result.slots().getDate() != null, "日期应取本轮新抽到的值");
    }

    @Test
    @DisplayName("规则无法判断时回退到 LLM 指代消解")
    void shouldFallbackToLlmRewrite() {
        when(llmRewriter.rewrite(anyString(), anyList())).thenReturn("年假怎么申请？");
        List<LlmService.ChatMessage> history = List.of(
                LlmService.ChatMessage.user("年假有多少天？"),
                LlmService.ChatMessage.assistant("年假为 10 天。"));

        ContextualQueryRewriter.RewriteResult result =
                rewriter.rewrite("那怎么申请呢？", history, new BookingSlots());

        assertEquals("年假怎么申请？", result.query());
        assertFalse(result.contextApplied());
    }

    @Test
    @DisplayName("纯知识问答且无槽位时不打扰 LLM")
    void shouldSkipLlmWhenNoHistoryAndNoSlots() {
        ContextualQueryRewriter.RewriteResult result =
                rewriter.rewrite("什么是向量检索", List.of(), new BookingSlots());

        assertEquals("什么是向量检索", result.query());
        verify(llmRewriter, never()).rewrite(any(), any());
    }
}
