package com.laoliu.cas.appointment.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.laoliu.cas.appointment.domain.entity.ConsultChatConversation;
import com.laoliu.cas.appointment.domain.entity.ConsultChatMessage;
import com.laoliu.cas.appointment.domain.repository.ConsultChatRepository;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.ConsultChatConversationDO;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.ConsultChatMessageDO;
import com.laoliu.cas.appointment.infrastructure.persistence.mapper.ConsultChatConversationMapper;
import com.laoliu.cas.appointment.infrastructure.persistence.mapper.ConsultChatMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 咨询沟通仓储实现（consult_chat_conversation / consult_chat_message）
 *
 * @author forever-king
 */
@Repository
@RequiredArgsConstructor
public class ConsultChatRepositoryImpl implements ConsultChatRepository {

    private final ConsultChatConversationMapper conversationMapper;
    private final ConsultChatMessageMapper messageMapper;

    @Override
    public Optional<ConsultChatConversation> findByPair(Long studentId, Long teacherId) {
        ConsultChatConversationDO dox = conversationMapper.selectOne(
                new LambdaQueryWrapper<ConsultChatConversationDO>()
                        .eq(ConsultChatConversationDO::getStudentId, studentId)
                        .eq(ConsultChatConversationDO::getTeacherId, teacherId));
        return Optional.ofNullable(dox).map(ConsultChatConversationDO::toEntity);
    }

    @Override
    public Optional<ConsultChatConversation> findConversationById(Long id) {
        return Optional.ofNullable(conversationMapper.selectById(id))
                .map(ConsultChatConversationDO::toEntity);
    }

    @Override
    public List<ConsultChatConversation> listByUserId(Long userId) {
        return conversationMapper.selectList(
                        new LambdaQueryWrapper<ConsultChatConversationDO>()
                                .and(w -> w.eq(ConsultChatConversationDO::getStudentId, userId)
                                        .or()
                                        .eq(ConsultChatConversationDO::getTeacherId, userId))
                                .orderByDesc(ConsultChatConversationDO::getId))
                .stream()
                .map(ConsultChatConversationDO::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public ConsultChatConversation getOrCreateConversation(Long studentId, Long teacherId) {
        Optional<ConsultChatConversation> exists = findByPair(studentId, teacherId);
        if (exists.isPresent()) {
            return exists.get();
        }
        ConsultChatConversationDO dox = ConsultChatConversationDO.builder()
                .studentId(studentId)
                .teacherId(teacherId)
                .createdAt(LocalDateTime.now())
                .build();
        try {
            conversationMapper.insert(dox);
        } catch (DuplicateKeyException e) {
            return findByPair(studentId, teacherId)
                    .orElseThrow(() -> new IllegalStateException("并发创建会话失败"));
        }
        return dox.toEntity();
    }

    @Override
    public ConsultChatMessage sendMessage(Long conversationId, Long senderId, String content) {
        ConsultChatMessageDO dox = ConsultChatMessageDO.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .content(content)
                .readFlag(false)
                .createdAt(LocalDateTime.now())
                .build();
        messageMapper.insert(dox);
        return dox.toEntity();
    }

    @Override
    public List<ConsultChatMessage> listMessages(Long conversationId, Long afterId) {
        return messageMapper.listMessages(conversationId, afterId).stream()
                .map(ConsultChatMessageDO::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ConsultChatMessage> lastMessage(Long conversationId) {
        return Optional.ofNullable(messageMapper.selectLastMessage(conversationId))
                .map(ConsultChatMessageDO::toEntity);
    }

    @Override
    public long countUnread(Long conversationId, Long viewerId) {
        return messageMapper.countUnread(conversationId, viewerId);
    }

    @Override
    public int markConversationRead(Long conversationId, Long viewerId) {
        return messageMapper.markConversationRead(conversationId, viewerId);
    }

    @Override
    public long countTotalUnread(Long userId) {
        return messageMapper.countTotalUnread(userId);
    }

    @Override
    public Optional<String> findUserName(Long userId) {
        String name = conversationMapper.selectUserName(userId);
        return StringUtils.hasText(name) ? Optional.of(name) : Optional.empty();
    }

    @Override
    public boolean isTeacherConsultant(Long teacherUserId) {
        return conversationMapper.countTeacherConsultant(teacherUserId) > 0;
    }

    @Override
    public boolean studentConsultedTeacher(Long studentUserId, Long teacherUserId) {
        return conversationMapper.countStudentConsultedTeacher(studentUserId, teacherUserId) > 0;
    }
}
