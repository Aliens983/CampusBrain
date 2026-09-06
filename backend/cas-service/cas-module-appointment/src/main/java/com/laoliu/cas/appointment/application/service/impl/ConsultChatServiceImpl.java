package com.laoliu.cas.appointment.application.service.impl;

import com.laoliu.cas.appointment.application.service.ConsultChatService;
import com.laoliu.cas.appointment.domain.entity.ConsultChatConversation;
import com.laoliu.cas.appointment.domain.entity.ConsultChatMessage;
import com.laoliu.cas.appointment.domain.entity.Consultant;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.domain.repository.ConsultChatRepository;
import com.laoliu.cas.appointment.domain.repository.ConsultantRepository;
import com.laoliu.cas.appointment.interfaces.dto.response.ConversationRespVO;
import com.laoliu.cas.appointment.interfaces.dto.response.MessageRespVO;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceStatusResponse;
import com.laoliu.cas.common.enums.UserRoleEnum;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.ChatErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 咨询沟通应用服务实现
 *
 * @author forever-king
 */
@Service
@RequiredArgsConstructor
public class ConsultChatServiceImpl implements ConsultChatService {

    private final ConsultChatRepository consultChatRepository;
    private final ConsultantRepository consultantRepository;
    private final BookingRepository bookingRepository;

    @Override
    public List<ConversationRespVO> listConversations(Long userId, Integer userRole) {
        requireChatRole(userRole);
        return consultChatRepository.listByUserId(userId).stream()
                .map(conv -> toConversationVO(conv, userId))
                // 有消息的会话按最后消息时间倒序，无消息的沉底
                .sorted(Comparator
                        .comparing((ConversationRespVO vo) -> vo.getLastTime() == null)
                        .thenComparing(ConversationRespVO::getLastTime,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    @Transactional
    public ConversationRespVO openWithConsultant(Long callerId, Integer callerRole, Long consultantId) {
        if (callerRole == null || UserRoleEnum.getByCode(callerRole) != UserRoleEnum.USER) {
            throw new BusinessException(ChatErrorCode.ROLE_NOT_ALLOWED);
        }
        Consultant consultant = consultantRepository.findById(consultantId)
                .orElseThrow(() -> new BusinessException(ChatErrorCode.PEER_NOT_CONSULT_TEACHER));
        Long teacherId = consultant.getUserId();
        if (teacherId == null || !consultChatRepository.isTeacherConsultant(teacherId)) {
            throw new BusinessException(ChatErrorCode.PEER_NOT_CONSULT_TEACHER);
        }
        ConsultChatConversation conversation =
                consultChatRepository.getOrCreateConversation(callerId, teacherId);
        return toConversationVO(conversation, callerId);
    }

    @Override
    @Transactional
    public ConversationRespVO openWithStudent(Long callerId, Integer callerRole, Long studentId) {
        if (callerRole == null || UserRoleEnum.getByCode(callerRole) != UserRoleEnum.TEACHER) {
            throw new BusinessException(ChatErrorCode.ROLE_NOT_ALLOWED);
        }
        if (Objects.equals(callerId, studentId)) {
            throw new BusinessException(ChatErrorCode.SELF_CHAT);
        }
        if (!consultChatRepository.studentConsultedTeacher(studentId, callerId)) {
            throw new BusinessException(ChatErrorCode.PEER_NOT_CONSULTED);
        }
        ConsultChatConversation conversation =
                consultChatRepository.getOrCreateConversation(studentId, callerId);
        return toConversationVO(conversation, callerId);
    }

    @Override
    @Transactional
    public ConversationRespVO openByBooking(Long callerId, Long orderId) {
        ServiceStatusResponse booking = bookingRepository.getServiceStatusByOrderIdAndUserId(callerId, orderId);
        if (booking == null) {
            throw new BusinessException(ChatErrorCode.BOOKING_NOT_FOUND);
        }
        Long teacherId = bookingRepository.selectConsultantOwnerByOrderId(orderId);
        if (teacherId == null) {
            throw new BusinessException(ChatErrorCode.BOOKING_NOT_CONSULT);
        }
        ConsultChatConversation conversation = consultChatRepository.getOrCreateConversation(callerId, teacherId);
        return toConversationVO(conversation, callerId);
    }

    @Override
    @Transactional
    public List<MessageRespVO> listMessages(Long callerId, Long conversationId, Long afterId) {
        requireParticipant(conversationId, callerId);
        // 拉取时顺带把发给我的置读，未读数随之清零
        consultChatRepository.markConversationRead(conversationId, callerId);
        return consultChatRepository.listMessages(conversationId, afterId).stream()
                .map(msg -> toMessageVO(msg, callerId))
                .toList();
    }

    @Override
    @Transactional
    public MessageRespVO sendMessage(Long callerId, Long conversationId, String content) {
        requireParticipant(conversationId, callerId);
        String text = content == null ? "" : content.trim();
        if (!StringUtils.hasText(text)) {
            throw new BusinessException(ChatErrorCode.MESSAGE_CONTENT_BLANK);
        }
        ConsultChatMessage message = consultChatRepository.sendMessage(conversationId, callerId, text);
        return toMessageVO(message, callerId);
    }

    @Override
    @Transactional
    public int markRead(Long callerId, Long conversationId) {
        requireParticipant(conversationId, callerId);
        return consultChatRepository.markConversationRead(conversationId, callerId);
    }

    @Override
    public long unreadTotal(Long userId) {
        return consultChatRepository.countTotalUnread(userId);
    }

    // ------------------------------------------------------------------ helpers

    private void requireChatRole(Integer role) {
        if (role == null) {
            throw new BusinessException(ChatErrorCode.ROLE_NOT_ALLOWED);
        }
        UserRoleEnum userRole = UserRoleEnum.getByCode(role);
        if (userRole != UserRoleEnum.USER && userRole != UserRoleEnum.TEACHER) {
            throw new BusinessException(ChatErrorCode.ROLE_NOT_ALLOWED);
        }
    }

    private ConsultChatConversation requireParticipant(Long conversationId, Long userId) {
        ConsultChatConversation conversation = consultChatRepository.findConversationById(conversationId)
                .orElseThrow(() -> new BusinessException(ChatErrorCode.CONVERSATION_NOT_FOUND));
        boolean participant = Objects.equals(conversation.getStudentId(), userId)
                || Objects.equals(conversation.getTeacherId(), userId);
        if (!participant) {
            throw new BusinessException(ChatErrorCode.CONVERSATION_NOT_FOUND);
        }
        return conversation;
    }

    private ConversationRespVO toConversationVO(ConsultChatConversation conversation, Long viewerId) {
        boolean viewerIsStudent = Objects.equals(conversation.getStudentId(), viewerId);
        Long peerUserId = viewerIsStudent ? conversation.getTeacherId() : conversation.getStudentId();
        String peerRole = viewerIsStudent ? "teacher" : "student";
        String peerName = consultChatRepository.findUserName(peerUserId).orElse("");

        ConsultChatMessage last = consultChatRepository.lastMessage(conversation.getId()).orElse(null);
        long unread = consultChatRepository.countUnread(conversation.getId(), viewerId);

        return ConversationRespVO.builder()
                .id(conversation.getId())
                .peerUserId(peerUserId)
                .peerRole(peerRole)
                .peerName(peerName)
                .lastMessage(last == null ? null : last.getContent())
                .lastTime(last == null ? null : last.getCreatedAt())
                .unreadCount(unread)
                .build();
    }

    private MessageRespVO toMessageVO(ConsultChatMessage message, Long viewerId) {
        return MessageRespVO.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .isMine(Objects.equals(message.getSenderId(), viewerId))
                .build();
    }
}
