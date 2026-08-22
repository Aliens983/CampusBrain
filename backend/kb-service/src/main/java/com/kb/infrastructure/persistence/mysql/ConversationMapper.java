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

    @Select("SELECT * FROM conversation WHERE session_id = #{sessionId} ORDER BY created_at DESC LIMIT #{limit}")
    List<ConversationDO> selectRecentBySessionId(@Param("sessionId") String sessionId,
                                                  @Param("limit") int limit);

    @Select("SELECT * FROM conversation WHERE session_id = #{sessionId} ORDER BY created_at ASC")
    List<ConversationDO> selectBySessionId(@Param("sessionId") String sessionId);

    @Update("UPDATE conversation SET feedback = #{feedback} WHERE id = #{id}")
    int updateFeedback(@Param("id") Long id, @Param("feedback") String feedback);

    @Delete("DELETE FROM conversation WHERE session_id = #{sessionId}")
    int deleteBySessionId(@Param("sessionId") String sessionId);
}
