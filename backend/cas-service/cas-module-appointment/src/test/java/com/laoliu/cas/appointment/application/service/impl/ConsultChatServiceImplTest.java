package com.laoliu.cas.appointment.application.service.impl;

import com.laoliu.cas.appointment.domain.entity.ConsultChatConversation;
import com.laoliu.cas.appointment.domain.entity.ConsultChatMessage;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.domain.repository.ConsultChatRepository;
import com.laoliu.cas.appointment.domain.repository.ConsultantRepository;
import com.laoliu.cas.appointment.interfaces.dto.response.ConversationResponse;
import com.laoliu.cas.common.enums.UserRoleEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 咨询沟通会话列表测试。
 * <p>
 * 重点锁定 4.7 的 N+1 收敛：listConversations 必须走 3 个批量仓储方法各一次，
 * 且映射/兜底语义（对端名、最后消息、未读数）与逐条版一致。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("咨询沟通会话列表")
class ConsultChatServiceImplTest {

    @Mock private ConsultChatRepository consultChatRepository;
    @Mock private ConsultantRepository consultantRepository;
    @Mock private BookingRepository bookingRepository;

    @InjectMocks private ConsultChatServiceImpl service;

    private ConsultChatConversation conversation(Long id, Long studentId, Long teacherId) {
        return ConsultChatConversation.builder()
                .id(id).studentId(studentId).teacherId(teacherId).build();
    }

    @Test
    @DisplayName("4.7 列表：对端名/最后消息/未读数各批量取一次，不走逐条查询，缺省按空值兜底")
    void shouldLoadConversationListInThreeBatchQueries() {
        Long viewerId = 7L;
        ConsultChatConversation c1 = conversation(11L, 7L, 8L);
        ConsultChatConversation c2 = conversation(12L, 7L, 9L);
        when(consultChatRepository.listByUserId(viewerId)).thenReturn(List.of(c1, c2));
        when(consultChatRepository.findUserNames(any())).thenReturn(
                Map.of(8L, "王老师", 9L, "李老师"));
        LocalDateTime lastTime = LocalDateTime.of(2026, 9, 19, 10, 0);
        when(consultChatRepository.findLastMessages(any())).thenReturn(Map.of(
                11L, ConsultChatMessage.builder()
                        .id(100L).conversationId(11L).senderId(8L)
                        .content("你好").createdAt(lastTime).build()));
        when(consultChatRepository.countUnread(anyCollection(), eq(viewerId)))
                .thenReturn(Map.of(11L, 2L));

        List<ConversationResponse> result =
                service.listConversations(viewerId, UserRoleEnum.USER.getCode());

        assertEquals(2, result.size());
        // 有最后消息的 c1 排前面（按最后时间倒序）
        ConversationResponse first = result.get(0);
        assertEquals(11L, first.getId());
        assertEquals(8L, first.getPeerUserId());
        assertEquals("teacher", first.getPeerRole());
        assertEquals("王老师", first.getPeerName());
        assertEquals("你好", first.getLastMessage());
        assertEquals(lastTime, first.getLastTime());
        assertEquals(2L, first.getUnreadCount());
        // c2 无消息/未读数为 0：Map 缺省兜底
        ConversationResponse second = result.get(1);
        assertEquals(12L, second.getId());
        assertEquals("李老师", second.getPeerName());
        assertNull(second.getLastMessage());
        assertNull(second.getLastTime());
        assertEquals(0L, second.getUnreadCount());

        // 三个批量方法各恰好一次；逐条版方法一次都不能调（N+1 回归护栏）
        verify(consultChatRepository, times(1)).findUserNames(any());
        verify(consultChatRepository, times(1)).findLastMessages(any());
        verify(consultChatRepository, times(1)).countUnread(anyCollection(), eq(viewerId));
        verify(consultChatRepository, never()).findUserName(anyLong());
        verify(consultChatRepository, never()).lastMessage(anyLong());
        verify(consultChatRepository, never()).countUnread(anyLong(), anyLong());
    }

    @Test
    @DisplayName("教师视角：对端是学生，peerRole=student 且对端名取自 studentId")
    void shouldResolvePeerAsStudentForTeacherViewer() {
        Long viewerId = 8L;
        when(consultChatRepository.listByUserId(viewerId))
                .thenReturn(List.of(conversation(11L, 7L, 8L)));
        when(consultChatRepository.findUserNames(any())).thenReturn(Map.of(7L, "张同学"));
        when(consultChatRepository.findLastMessages(any())).thenReturn(Map.of());
        when(consultChatRepository.countUnread(anyCollection(), eq(viewerId))).thenReturn(Map.of());

        List<ConversationResponse> result =
                service.listConversations(viewerId, UserRoleEnum.TEACHER.getCode());

        assertEquals(1, result.size());
        assertEquals(7L, result.get(0).getPeerUserId());
        assertEquals("student", result.get(0).getPeerRole());
        assertEquals("张同学", result.get(0).getPeerName());
    }

    @Test
    @DisplayName("空会话列表直接返回，不发任何批量 SQL（防 IN () 语法错误与空查）")
    void shouldShortCircuitOnEmptyConversationList() {
        when(consultChatRepository.listByUserId(7L)).thenReturn(List.of());

        List<ConversationResponse> result =
                service.listConversations(7L, UserRoleEnum.USER.getCode());

        assertTrue(result.isEmpty());
        verify(consultChatRepository, never()).findUserNames(any());
        verify(consultChatRepository, never()).findLastMessages(any());
        verify(consultChatRepository, never()).countUnread(anyCollection(), eq(7L));
    }
}
