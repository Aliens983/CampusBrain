package com.kb.infrastructure.rag.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工具调用守卫判定测试。
 * <p>
 * 覆盖 2026-09-26 真实故障中模型连续输出的各类"空承诺"文本（中文短句、整句英文、
 * 空白），同时确保带真实数据的长回答与正常寒暄不会被误判而触发无意义的强制重试。
 *
 * @author forever-king
 */
@DisplayName("Function Calling 空承诺守卫")
class ToolCallGuardTest {

    @Test
    @DisplayName("空白输出视为空承诺")
    void blankIsEmptyPromise() {
        assertThat(ToolCallGuard.isEmptyPromise(null)).isTrue();
        assertThat(ToolCallGuard.isEmptyPromise("")).isTrue();
        assertThat(ToolCallGuard.isEmptyPromise("   \n\t ")).isTrue();
    }

    @Test
    @DisplayName("真实故障中的中文承诺句全部命中")
    void chinesePromiseHits() {
        assertThat(ToolCallGuard.isEmptyPromise("我来帮您查询仓前校区 2025-03-26 上午 9:00–10:00 的教师咨询余量。"))
                .isTrue();
        assertThat(ToolCallGuard.isEmptyPromise("好的，我理解您想查询仓前校区3月26日上午9:00–10:00的教师咨询余量。我这就为您查询。"))
                .isTrue();
        assertThat(ToolCallGuard.isEmptyPromise("还没有，我这就为您查询仓前校区3月26日上午9:00–10:00的教师咨询余量。"))
                .isTrue();
        assertThat(ToolCallGuard.isEmptyPromise("请稍等，马上为您查询。")).isTrue();
    }

    @Test
    @DisplayName("真实故障中的英文搪塞句命中")
    void englishPromiseHits() {
        assertThat(ToolCallGuard.isEmptyPromise(
                "I'll query the teacher consultation availability for Cangqian campus on that date."))
                .isTrue();
        assertThat(ToolCallGuard.isEmptyPromise(
                "I'llquerytheteacherconsultationavailabilityforCangqiancampusonthatdate."))
                .isTrue();
    }

    @Test
    @DisplayName("2026-09-29 复发：look up/check 类英文承诺句命中")
    void englishLookUpPromiseHits() {
        assertThat(ToolCallGuard.isEmptyPromise("I'll look up the available consultants for you."))
                .isTrue();
        assertThat(ToolCallGuard.isEmptyPromise("I'll look up the available equipment for you."))
                .isTrue();
        assertThat(ToolCallGuard.isEmptyPromise("Let me check the available rooms first."))
                .isTrue();
        assertThat(ToolCallGuard.isEmptyPromise("I’m going to search for available services now."))
                .isTrue();
        // 含 look 的计划旁白（工具已执行）也要补救
        assertThat(ToolCallGuard.isIncompleteNarrative("Let me look up the detailed time slots."))
                .isTrue();
    }

    @Test
    @DisplayName("英文正常能力介绍与含 still/will 的句子不被误判")
    void englishNormalAnswerNotPromised() {
        // 能力介绍用 I can（非将来时承诺），不应触发重试
        assertThat(ToolCallGuard.isEmptyPromise(
                "I can help you look up available consultants. Which campus do you prefer?"))
                .isFalse();
        // 单词边界：still/will 里的 ill 不能误命中
        assertThat(ToolCallGuard.isEmptyPromise(
                "I still need the date to proceed; which day would you like to book?"))
                .isFalse();
    }

    @Test
    @DisplayName("带真实数据的长回答不命中")
    void dataBackedAnswerNotPromised() {
        String real = "查询结果出来了：仓前校区2026-09-26的教师咨询，目前5位咨询师当天可约时段情况如下：\n"
                + "-肖老师（心理咨询中心）—2个\n-周老师（心理咨询中心）—0个\n"
                + "-刘老师（心理咨询中心）—3个\n-石老师（学业辅导中心）—1个\n"
                + "-管老师（学业辅导中心）—0个\n建议您可以：1.换一个日期再查。";
        assertThat(ToolCallGuard.isEmptyPromise(real)).isFalse();
    }

    @Test
    @DisplayName("正常寒暄与直接回答不命中")
    void normalChatNotPromised() {
        assertThat(ToolCallGuard.isEmptyPromise("你好！我是校园助手，可以帮你查询可预约服务。")).isFalse();
        // 即使短句里出现承诺词，但内容已是带数据的完整答复（长度超阈值）也不命中
        assertThat(ToolCallGuard.isEmptyPromise(
                "已经帮您查到了，仓前校区明天上午9点到10点共有三位咨询师可约，分别是肖老师、周老师和刘老师，"
                        + "余量分别为2、1、3个时段，需要我帮你预约其中一位吗？"))
                .isFalse();
    }

    @Test
    @DisplayName("工具已返回但回答停在计划式旁白时命中补救条件")
    void incompleteNarrativeHits() {
        assertThat(ToolCallGuard.isIncompleteNarrative(
                "我查到了仓前校区今天（2026-09-26）的教师咨询可约情况，但还需要确认具体时段是否覆盖 "
                        + "9:00-10:00。让我看看有剩余时段的两位老师的详细时间。")).isTrue();
        assertThat(ToolCallGuard.isIncompleteNarrative("我再确认一下具体时段。")).isTrue();
    }

    @Test
    @DisplayName("基于工具数据的完整结论不触发旁白补救")
    void completeConclusionNotNarrative() {
        String conclusion = "仓前校区今天9:00-10:00可以预约教师咨询：肖老师和刘老师在该时段各有1个可约名额，"
                + "周老师已约满。需要我帮你预约肖老师吗？";
        assertThat(ToolCallGuard.isIncompleteNarrative(conclusion)).isFalse();
        assertThat(ToolCallGuard.isEmptyPromise(conclusion)).isFalse();
    }

    @Test
    @DisplayName("强制重试指令与兜底文案非空且为中文")
    void instructionsArePresent() {
        assertThat(ToolCallGuard.forcedRetryInstruction()).contains("立即调用", "工具");
        assertThat(ToolCallGuard.retryExhaustedMessage()).contains("稍后");
    }
}
