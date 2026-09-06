package com.laoliu.cas.appointment.domain.repository;

import com.laoliu.cas.appointment.domain.entity.ConsultChatConversation;
import com.laoliu.cas.appointment.domain.entity.ConsultChatMessage;

import java.util.List;
import java.util.Optional;

/**
 * 咨询沟通仓储接口（学生 ⇄ 教师 1:1 在线留言）
 *
 * @author forever-king
 */
public interface ConsultChatRepository {

    /** 查询某对（学生,教师）是否已有会话 */
    Optional<ConsultChatConversation> findByPair(Long studentId, Long teacherId);

    /** 按主键查会话 */
    Optional<ConsultChatConversation> findConversationById(Long id);

    /** 查询某人作为任意一方参与的会话（按创建倒序） */
    List<ConsultChatConversation> listByUserId(Long userId);

    /** 幂等创建会话（冲突唯一键时返回已存在的那条） */
    ConsultChatConversation getOrCreateConversation(Long studentId, Long teacherId);

    /** 发送一条消息（返回落库后的消息） */
    ConsultChatMessage sendMessage(Long conversationId, Long senderId, String content);

    /** 拉取某会话消息（afterId 为空取全部；非空取 id>afterId 的新消息，均按 id 升序） */
    List<ConsultChatMessage> listMessages(Long conversationId, Long afterId);

    /** 某会话最后一条消息（用于会话列表预览） */
    Optional<ConsultChatMessage> lastMessage(Long conversationId);

    /** 某会话中发给 viewer 的未读数（sender ≠ viewer 且未读） */
    long countUnread(Long conversationId, Long viewerId);

    /** 打开会话时把所有发给 viewer 的消息置为已读，返回置读条数 */
    int markConversationRead(Long conversationId, Long viewerId);

    /** 某用户所有会话的总未读数（导航红点用） */
    long countTotalUnread(Long userId);

    /** 用户显示名（聊天对端） */
    Optional<String> findUserName(Long userId);

    /** 该教师账号是否为「教师咨询」分类咨询师（是否开放学生发起沟通） */
    boolean isTeacherConsultant(Long teacherUserId);

    /** 该学生是否咨询过该教师（教师在名下档期里回复时校验） */
    boolean studentConsultedTeacher(Long studentUserId, Long teacherUserId);
}
