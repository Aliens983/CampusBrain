package com.laoliu.cas.appointment.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.ConsultChatConversationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 咨询沟通会话 Mapper
 *
 * @author forever-king
 */
@Mapper
public interface ConsultChatConversationMapper extends BaseMapper<ConsultChatConversationDO> {

    /** 该教师账号是否绑定有「教师咨询」分类的咨询师（决定学生能否向 TA 发起会话） */
    @Select("""
            SELECT COUNT(*) FROM consultant c
              JOIN services s ON s.service_id = c.service_id
              JOIN service_category sc ON sc.id = s.category_id
            WHERE c.user_id = #{teacherUserId} AND sc.code = 'teacher'
            """)
    long countTeacherConsultant(@Param("teacherUserId") Long teacherUserId);

    /**
     * 该学生是否咨询过该教师（教师对学生发起会话时校验：存在该教师名下咨询档期的预约）。
     * 仅统计活动单（0 待审核 / 1 已通过）；已拒绝/已取消/已完成等终态废单不得作为
     * 发起会话的凭据，与服务层 openByBooking 的状态白名单同口径。
     */
    @Select("""
            SELECT COUNT(*) FROM item i
              JOIN consultant c ON c.id = i.consultant_id
            WHERE i.user_id = #{studentUserId} AND c.user_id = #{teacherUserId}
              AND i.manage_status IN (0, 1)
            """)
    long countStudentConsultedTeacher(@Param("studentUserId") Long studentUserId,
                                      @Param("teacherUserId") Long teacherUserId);

    /** 用户显示名（聊天对端名称） */
    @Select("SELECT name FROM `user` WHERE id = #{userId}")
    String selectUserName(@Param("userId") Long userId);

    /**
     * 批量取用户显示名（4.7 N+1 收敛：会话列表此前每会话发一次 selectUserName）。
     * 调用方须保证 userIds 非空；库中无此用户或 name 为空时不在结果集中。
     */
    @Select("""
            <script>
            SELECT id AS userId, name AS userName
            FROM `user`
            WHERE id IN
            <foreach collection="userIds" item="uid" open="(" separator="," close=")">#{uid}</foreach>
            </script>
            """)
    List<UserNameRow> selectUserNames(@Param("userIds") Collection<Long> userIds);

    /** 用户显示名投影行（userId → name） */
    record UserNameRow(Long userId, String userName) {
    }
}
