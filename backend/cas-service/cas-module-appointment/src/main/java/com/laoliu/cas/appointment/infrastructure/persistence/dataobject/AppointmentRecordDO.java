package com.laoliu.cas.appointment.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.laoliu.cas.appointment.domain.entity.AppointmentRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 预约记录数据对象 - 用于 MyBatis-Plus ORM
 *
 * @author forever-king
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("appointment_record")
public class AppointmentRecordDO {

    @TableId(type = IdType.AUTO)
    private Integer recordId;

    /** 服务项目ID */
    private Integer itemId;

    /** 用户ID */
    private Long userId;

    /** 预约时间 */
    private String appointmentTime;

    /** 预约地点 */
    private String appointmentPlace;

    /** 预约状态（0-待审核，1-通过，2-拒绝，3-取消） */
    private Integer appointmentState;

    /** 预约描述/备注 */
    private String description;

    /** 创建时间 */
    private String createTime;

    public AppointmentRecord toEntity() {
        return AppointmentRecord.builder()
                .recordId(recordId).itemId(itemId).userId(userId)
                .appointmentTime(appointmentTime).appointmentPlace(appointmentPlace)
                .appointmentState(appointmentState).description(description)
                .createTime(createTime)
                .build();
    }

    public static AppointmentRecordDO fromEntity(AppointmentRecord entity) {
        if (entity == null) return null;
        return AppointmentRecordDO.builder()
                .recordId(entity.getRecordId()).itemId(entity.getItemId())
                .userId(entity.getUserId()).appointmentTime(entity.getAppointmentTime())
                .appointmentPlace(entity.getAppointmentPlace())
                .appointmentState(entity.getAppointmentState())
                .description(entity.getDescription()).createTime(entity.getCreateTime())
                .build();
    }
}
