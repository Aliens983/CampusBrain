package com.laoliu.cas.appointment.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.ConsultChatMessageDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 咨询沟通消息 Mapper
 *
 * @author forever-king
 */
@Mapper
public interface ConsultChatMessageMapper extends BaseMapper<ConsultChatMessageDO> {

    /** 拉取会话消息：afterId 为空取全部；非空取 id>afterId 的新消息（轮询增量），均按 id 升序 */
    @Select("""
            <script>
            SELECT * FROM consult_chat_message
            WHERE conversation_id = #{conversationId}
            <if test="afterId != null">AND id &gt; #{afterId}</if>
            ORDER BY id ASC
            </script>
            """)
    List<ConsultChatMessageDO> listMessages(@Param("conversationId") Long conversationId,
                                            @Param("afterId") Long afterId);

    /** 会话最后一条消息（列表预览） */
    @Select("SELECT * FROM consult_chat_message WHERE conversation_id = #{conversationId} ORDER BY id DESC LIMIT 1")
    ConsultChatMessageDO selectLastMessage(@Param("conversationId") Long conversationId);

    /** 某会话发给 viewer 的未读数（sender ≠ viewer 且未读） */
    @Select("""
            SELECT COUNT(*) FROM consult_chat_message
            WHERE conversation_id = #{conversationId} AND sender_id != #{viewerId} AND read_flag = 0
            """)
    long countUnread(@Param("conversationId") Long conversationId, @Param("viewerId") Long viewerId);

    /** 打开会话/轮询时，把发给 viewer 的消息置为已读 */
    @Update("""
            UPDATE consult_chat_message SET read_flag = 1
            WHERE conversation_id = #{conversationId} AND sender_id != #{viewerId} AND read_flag = 0
            """)
    int markConversationRead(@Param("conversationId") Long conversationId, @Param("viewerId") Long viewerId);

    /** 某用户所有会话的总未读数（导航红点用） */
    @Select("""
            SELECT COUNT(*) FROM consult_chat_message m
              JOIN consult_chat_conversation c ON c.id = m.conversation_id
            WHERE (c.student_id = #{userId} OR c.teacher_id = #{userId})
              AND m.sender_id != #{userId} AND m.read_flag = 0
            """)
    long countTotalUnread(@Param("userId") Long userId);
}
