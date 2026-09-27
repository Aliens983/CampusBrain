package com.laoliu.cas.appointment.infrastructure.persistence.mapper;

import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.ScheduleCancelRequestDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * @author forever-king
 */
@Mapper
public interface ScheduleCancelRequestMapper {

    @Insert("INSERT INTO schedule_cancel_request(consultant_id, slot_id, slot_date, start_time, end_time, reason, teacher_user_id) " +
            "VALUES(#{consultantId}, #{slotId}, #{slotDate}, #{startTime}, #{endTime}, #{reason}, #{teacherUserId})")
    int insert(@Param("consultantId") Long consultantId, @Param("slotId") Long slotId,
               @Param("slotDate") java.time.LocalDate slotDate, @Param("startTime") String startTime,
               @Param("endTime") String endTime, @Param("reason") String reason,
               @Param("teacherUserId") Long teacherUserId);

    @Select("SELECT r.*, c.name AS consultantName, u.name AS teacherName " +
            "FROM schedule_cancel_request r JOIN consultant c ON c.id = r.consultant_id " +
            "JOIN `user` u ON u.id = r.teacher_user_id WHERE r.teacher_user_id = #{teacherUserId} " +
            "ORDER BY r.create_time DESC LIMIT 200")
    List<ScheduleCancelRequestDO> findByTeacher(@Param("teacherUserId") Long teacherUserId);

    @Select("SELECT r.*, c.name AS consultantName, u.name AS teacherName " +
            "FROM schedule_cancel_request r JOIN consultant c ON c.id = r.consultant_id " +
            "JOIN `user` u ON u.id = r.teacher_user_id WHERE r.status = 0 " +
            "ORDER BY r.create_time ASC LIMIT 500")
    List<ScheduleCancelRequestDO> findPending();

    @Select("SELECT id, consultant_id AS consultantId, slot_id AS slotId, slot_date AS slotDate, " +
            "start_time AS startTime, end_time AS endTime, reason, status, teacher_user_id AS teacherUserId, " +
            "auditor_id AS auditorId, audit_remark AS auditRemark, audit_time AS auditTime, create_time AS createTime " +
            "FROM schedule_cancel_request WHERE id = #{id} FOR UPDATE")
    ScheduleCancelRequestDO selectByIdForUpdate(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM schedule_cancel_request WHERE slot_id = #{slotId} AND status = 0")
    int countPendingForSlot(@Param("slotId") Long slotId);

    @Select("SELECT id, consultant_id AS consultantId, slot_id AS slotId, slot_date AS slotDate, " +
            "start_time AS startTime, end_time AS endTime, reason, status, teacher_user_id AS teacherUserId, " +
            "auditor_id AS auditorId, audit_remark AS auditRemark, audit_time AS auditTime, create_time AS createTime " +
            "FROM schedule_cancel_request WHERE slot_id = #{slotId} AND status = 0 FOR UPDATE")
    ScheduleCancelRequestDO selectPendingBySlotForUpdate(@Param("slotId") Long slotId);

    @Update("UPDATE schedule_cancel_request SET status = 1, auditor_id = #{auditorId}, " +
            "audit_remark = #{remark}, audit_time = NOW() WHERE id = #{id} AND status = 0")
    int approve(@Param("id") Long id, @Param("auditorId") Long auditorId, @Param("remark") String remark);

    @Update("UPDATE schedule_cancel_request SET status = 2, auditor_id = #{auditorId}, " +
            "audit_remark = #{remark}, audit_time = NOW() WHERE id = #{id} AND status = 0")
    int reject(@Param("id") Long id, @Param("auditorId") Long auditorId, @Param("remark") String remark);
}
