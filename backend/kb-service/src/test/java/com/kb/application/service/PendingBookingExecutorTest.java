package com.kb.application.service;

import com.kb.domain.chat.ChatSessionRepository;
import com.kb.domain.conversation.ConversationRepository;
import com.kb.infrastructure.client.CasClient;
import com.kb.infrastructure.metrics.BusinessMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 确认/取消意图判定边界测试.
 * <p>
 * 核心风险是"误确认"：待确认态下一句带"确认"的查询句若被判成确认，
 * 会在用户并无下单意图时触发真实预约.词表判定必须优先否决查询语境.
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("待确认预约意图判定")
class PendingBookingExecutorTest {

    @Mock private CasClient casClient;
    @Mock private ChatSessionRepository chatSessionRepository;
    @Mock private ConversationRepository conversationRepository;
    @Mock private BusinessMetrics metrics;

    private PendingBookingExecutor executor() {
        return new PendingBookingExecutor(casClient, chatSessionRepository, conversationRepository, metrics);
    }

    @Nested
    @DisplayName("查询语境否决：问进度不得触发真实下单")
    class QueryContextVeto {

        @Test
        @DisplayName("「确认一下我的预约到哪了」→ NONE（含\"哪\"）")
        void shouldNotConfirmWhenAskingProgressWithQue() {
            assertEquals(PendingBookingExecutor.ConfirmIntent.NONE,
                    executor().detectConfirmIntent("确认一下我的预约到哪了"));
        }

        @Test
        @DisplayName("「确定下订单状态」→ NONE（含\"状态\"）")
        void shouldNotConfirmWhenAskingOrderStatus() {
            assertEquals(PendingBookingExecutor.ConfirmIntent.NONE,
                    executor().detectConfirmIntent("确定下订单状态"));
        }

        @Test
        @DisplayName("「帮我确认一下我的预约到哪了」→ NONE（请求-动作分支亦不得放行）")
        void shouldNotConfirmWhenRequestActionAsksProgress() {
            assertEquals(PendingBookingExecutor.ConfirmIntent.NONE,
                    executor().detectConfirmIntent("帮我确认一下我的预约到哪了"));
        }

        @Test
        @DisplayName("「帮我查一下预约成功了吗」→ NONE")
        void shouldNotConfirmWhenAskingToCheck() {
            assertEquals(PendingBookingExecutor.ConfirmIntent.NONE,
                    executor().detectConfirmIntent("帮我查一下预约成功了吗"));
        }
    }

    @Nested
    @DisplayName("正常确认/取消判定不回归")
    class NormalIntent {

        @Test
        @DisplayName("「确认」→ CONFIRM")
        void shouldConfirmOnBareConfirm() {
            assertEquals(PendingBookingExecutor.ConfirmIntent.CONFIRM,
                    executor().detectConfirmIntent("确认"));
        }

        @Test
        @DisplayName("「确认预约」→ CONFIRM")
        void shouldConfirmOnConfirmBooking() {
            assertEquals(PendingBookingExecutor.ConfirmIntent.CONFIRM,
                    executor().detectConfirmIntent("确认预约"));
        }

        @Test
        @DisplayName("「好的」→ CONFIRM")
        void shouldConfirmOnAck() {
            assertEquals(PendingBookingExecutor.ConfirmIntent.CONFIRM,
                    executor().detectConfirmIntent("好的"));
        }

        @Test
        @DisplayName("「帮我预约」→ CONFIRM（请求-动作 + 明确下单动作）")
        void shouldConfirmOnExplicitBookRequest() {
            assertEquals(PendingBookingExecutor.ConfirmIntent.CONFIRM,
                    executor().detectConfirmIntent("帮我预约"));
        }

        @Test
        @DisplayName("「取消一下我的预约」→ REJECT（\"一下\"不得误伤取消语义）")
        void shouldRejectOnCancelRequest() {
            assertEquals(PendingBookingExecutor.ConfirmIntent.REJECT,
                    executor().detectConfirmIntent("取消一下我的预约"));
        }

        @Test
        @DisplayName("「算了，不要了」→ REJECT")
        void shouldRejectOnGiveUp() {
            assertEquals(PendingBookingExecutor.ConfirmIntent.REJECT,
                    executor().detectConfirmIntent("算了，不要了"));
        }

        @Test
        @DisplayName("疑问句「明天可以预约吗」→ NONE")
        void shouldNoneOnQuestion() {
            assertEquals(PendingBookingExecutor.ConfirmIntent.NONE,
                    executor().detectConfirmIntent("明天可以预约吗"));
        }

        @Test
        @DisplayName("空句 → NONE")
        void shouldNoneOnBlank() {
            assertEquals(PendingBookingExecutor.ConfirmIntent.NONE,
                    executor().detectConfirmIntent("  "));
        }
    }
}
