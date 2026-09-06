package com.laoliu.cas.appointment.application.service;

import com.laoliu.cas.appointment.interfaces.dto.response.ConversationRespVO;
import com.laoliu.cas.appointment.interfaces.dto.response.MessageRespVO;

import java.util.List;

/**
 * 咨询沟通应用服务（学生 ⇄ 教师 1:1 在线留言）
 *
 * @author forever-king
 */
public interface ConsultChatService {

    /** 我的会话列表（最近活跃在前） */
    List<ConversationRespVO> listConversations(Long userId, Integer userRole);

    /** 学生从「选咨询师」卡片发起会话（仅教师咨询类咨询师） */
    ConversationRespVO openWithConsultant(Long callerId, Integer callerRole, Long consultantId);

    /** 教师对其名下咨询档期的某位学生发起会话 */
    ConversationRespVO openWithStudent(Long callerId, Integer callerRole, Long studentId);

    /** 学生凭自己的咨询预约单进入会话（我的预约详情） */
    ConversationRespVO openByBooking(Long callerId, Long orderId);

    /** 拉取会话消息（afterId 为空=全量，非空=增量轮询），并顺带置读 */
    List<MessageRespVO> listMessages(Long callerId, Long conversationId, Long afterId);

    /** 发送消息 */
    MessageRespVO sendMessage(Long callerId, Long conversationId, String content);

    /** 打开会话时把发给我的消息置为已读，返回置读条数 */
    int markRead(Long callerId, Long conversationId);

    /** 我的所有会话未读总数（导航红点） */
    long unreadTotal(Long userId);
}
