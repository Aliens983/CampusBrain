package com.laoliu.cas.appointment.application.service.impl;

import com.laoliu.cas.appointment.application.service.ConsultChatService;
import com.laoliu.cas.appointment.domain.entity.ConsultChatConversation;
import com.laoliu.cas.appointment.domain.entity.ConsultChatMessage;
import com.laoliu.cas.appointment.domain.entity.Consultant;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.domain.repository.ConsultChatRepository;
import com.laoliu.cas.appointment.domain.repository.ConsultantRepository;
import com.laoliu.cas.appointment.application.dto.response.ConversationResponse;
import com.laoliu.cas.appointment.application.dto.response.MessageResponse;
import com.laoliu.cas.appointment.domain.view.BookingQueryView;
import com.laoliu.cas.common.enums.ManageStatus;
import com.laoliu.cas.common.enums.UserRoleEnum;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.ChatErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    public List<ConversationResponse> listConversations(Long userId, Integer userRole) {
        requireChatRole(userRole);
        List<ConsultChatConversation> conversations = consultChatRepository.listByUserId(userId);
        if (conversations.isEmpty()) {
            return List.of();
        }
        // 4.7 N+1 收敛：对端名称、最后消息、未读数各一条批量 SQL 取回，
        // 取代此前「每会话 3 次查询」（10 个会话从 30 次 SQL 降到 3 次）
        Set<Long> peerIds = conversations.stream()
                .map(conv -> Objects.equals(conv.getStudentId(), userId)
                        ? conv.getTeacherId() : conv.getStudentId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<Long> conversationIds = conversations.stream()
                .map(ConsultChatConversation::getId)
                .toList();
        Map<Long, String> peerNames = consultChatRepository.findUserNames(peerIds);
        Map<Long, ConsultChatMessage> lastMessages = consultChatRepository.findLastMessages(conversationIds);
        Map<Long, Long> unreadCounts = consultChatRepository.countUnread(conversationIds, userId);

        return conversations.stream()
                .map(conv -> toConversationVO(conv, userId, peerNames, lastMessages, unreadCounts))
                // 有消息的会话按最后消息时间倒序，无消息的沉底
                .sorted(Comparator
                        .comparing((ConversationResponse vo) -> vo.getLastTime() == null)
                        .thenComparing(ConversationResponse::getLastTime,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    @Transactional
    public ConversationResponse openWithConsultant(Long callerId, Integer callerRole, Long consultantId) {
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
    public ConversationResponse openWithStudent(Long callerId, Integer callerRole, Long studentId) {
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
    public ConversationResponse openByBooking(Long callerId, Long orderId) {
        BookingQueryView booking = bookingRepository.getServiceStatusByOrderIdAndUserId(callerId, orderId);
        if (booking == null) {
            throw new BusinessException(ChatErrorCode.BOOKING_NOT_FOUND);
        }
        // 仅 待审核(0)/已通过(1) 的活动单可发起沟通；已拒绝/已取消/已完成等终态废单
        // 不得借此与咨询师建立会话（状态判据与教师侧 countStudentConsultedTeacher SQL 统一）
        Integer status = booking.getManageStatus();
        if (status == null
                || (status != ManageStatus.SUBMIT.getCode() && status != ManageStatus.APPROVED.getCode())) {
            throw new BusinessException(ChatErrorCode.BOOKING_STATUS_NOT_ALLOWED);
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
    public List<MessageResponse> listMessages(Long callerId, Long conversationId, Long afterId) {
        requireParticipant(conversationId, callerId);
        // 拉取时顺带把发给我的置读，未读数随之清零
        consultChatRepository.markConversationRead(conversationId, callerId);
        return consultChatRepository.listMessages(conversationId, afterId).stream()
                .map(msg -> toMessageVO(msg, callerId))
                .toList();
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(Long callerId, Long conversationId, String content) {
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

    private ConversationResponse toConversationVO(ConsultChatConversation conversation, Long viewerId) {
        boolean viewerIsStudent = Objects.equals(conversation.getStudentId(), viewerId);
        Long peerUserId = viewerIsStudent ? conversation.getTeacherId() : conversation.getStudentId();
        String peerName = peerUserId == null ? ""
                : consultChatRepository.findUserName(peerUserId).orElse("");
        ConsultChatMessage last = consultChatRepository.lastMessage(conversation.getId()).orElse(null);
        long unread = consultChatRepository.countUnread(conversation.getId(), viewerId);
        return buildConversationVO(conversation, viewerId, peerUserId, peerName, last, unread);
    }

    /**
     * 批量版（4.7）：对端名称 / 最后消息 / 未读数由 listConversations 一次性预取，
     * Map 中缺失即分别按空串 / null / 0 兜底，语义与逐条版完全一致。
     */
    private ConversationResponse toConversationVO(ConsultChatConversation conversation, Long viewerId,
                                                  Map<Long, String> peerNames,
                                                  Map<Long, ConsultChatMessage> lastMessages,
                                                  Map<Long, Long> unreadCounts) {
        boolean viewerIsStudent = Objects.equals(conversation.getStudentId(), viewerId);
        Long peerUserId = viewerIsStudent ? conversation.getTeacherId() : conversation.getStudentId();
        String peerName = peerUserId == null ? "" : peerNames.getOrDefault(peerUserId, "");
        ConsultChatMessage last = lastMessages.get(conversation.getId());
        long unread = unreadCounts.getOrDefault(conversation.getId(), 0L);
        return buildConversationVO(conversation, viewerId, peerUserId, peerName, last, unread);
    }

    private ConversationResponse buildConversationVO(ConsultChatConversation conversation, Long viewerId,
                                                     Long peerUserId, String peerName,
                                                     ConsultChatMessage last, long unread) {
        boolean viewerIsStudent = Objects.equals(conversation.getStudentId(), viewerId);
        String peerRole = viewerIsStudent ? "teacher" : "student";

        return ConversationResponse.builder()
                .id(conversation.getId())
                .peerUserId(peerUserId)
                .peerRole(peerRole)
                .peerName(peerName)
                .lastMessage(last == null ? null : last.getContent())
                .lastTime(last == null ? null : last.getCreatedAt())
                .unreadCount(unread)
                .build();
    }

    private MessageResponse toMessageVO(ConsultChatMessage message, Long viewerId) {
        return MessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .isMine(Objects.equals(message.getSenderId(), viewerId))
                .build();
    }
}
