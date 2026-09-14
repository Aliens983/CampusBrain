package com.kb.infrastructure.persistence.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kb.infrastructure.persistence.mysql.dataobject.ConversationDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * MyBatis-Plus mapper for conversation table.
 *
 * @author forever-king
 */
@Mapper
public interface ConversationMapper extends BaseMapper<ConversationDO> {

    /**
     * 取某用户会话最近 N 条消息，按时间<b>正序</b>返回（便于直接拼成对话历史）。
     * 内层先按倒序取最近 N 条，外层再翻转回正序。
     * 归属过滤：只返回 user_id 匹配的消息（4.1.13）。
     */
    @Select("SELECT * FROM (SELECT * FROM conversation WHERE session_id = #{sessionId} " +
            "AND user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}) t " +
            "ORDER BY created_at ASC, id ASC")
    List<ConversationDO> selectRecentBySessionId(@Param("sessionId") String sessionId,
                                                  @Param("userId") Long userId,
                                                  @Param("limit") int limit);

    /**
     * 查询某用户在指定会话中的全部消息（按时间正序）。
     */
    @Select("SELECT * FROM conversation WHERE session_id = #{sessionId} " +
            "AND user_id = #{userId} ORDER BY created_at ASC")
    List<ConversationDO> selectBySessionId(@Param("sessionId") String sessionId,
                                            @Param("userId") Long userId);

    /**
     * 查询会话归属人：同一 session 的所有消息 user_id 一致，取最早一条即可。
     * 返回 null 表示该会话尚无消息或存在无主（V2 之前的存量）数据。
     */
    @Select("SELECT user_id FROM conversation WHERE session_id = #{sessionId} " +
            "ORDER BY id ASC LIMIT 1")
    Long selectOwnerUserIdBySessionId(@Param("sessionId") String sessionId);

    /**
     * 首写者认领无主存量消息：把该会话下 user_id 为空的消息绑定到当前用户。
     */
    @Update("UPDATE conversation SET user_id = #{userId} " +
            "WHERE session_id = #{sessionId} AND user_id IS NULL")
    int bindOwnerlessMessages(@Param("sessionId") String sessionId,
                              @Param("userId") Long userId);

    /**
     * 反馈更新带归属条件，返回影响行数（0 表示消息不存在或不属于当前用户）。
     */
    @Update("UPDATE conversation SET feedback = #{feedback} " +
            "WHERE id = #{id} AND user_id = #{userId}")
    int updateFeedback(@Param("id") Long id,
                       @Param("feedback") String feedback,
                       @Param("userId") Long userId);

    @Delete("DELETE FROM conversation WHERE session_id = #{sessionId} AND user_id = #{userId}")
    int deleteBySessionId(@Param("sessionId") String sessionId,
                          @Param("userId") Long userId);
}
