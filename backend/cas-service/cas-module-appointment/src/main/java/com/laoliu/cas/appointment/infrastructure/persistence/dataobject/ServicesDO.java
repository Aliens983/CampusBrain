package com.laoliu.cas.appointment.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.laoliu.cas.appointment.domain.entity.Service;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务数据对象 - 用于 MyBatis-Plus ORM
 * 
 * @author forever-king
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("services")
public class ServicesDO {

    @TableId(type = IdType.AUTO)
    private Long serviceId;

    /** 服务名称 */
    private String serviceName;

    /** 服务描述 */
    private String serviceDescribe;

    /** 服务状态（0-禁用，1-启用） */
    private Integer serviceState;

    public Service toEntity() {
        return Service.builder()
                .serviceId(serviceId).serviceName(serviceName)
                .serviceDescribe(serviceDescribe).serviceState(serviceState)
                .build();
    }

    public static ServicesDO fromEntity(Service entity) {
        if (entity == null) return null;
        return ServicesDO.builder()
                .serviceId(entity.getServiceId()).serviceName(entity.getServiceName())
                .serviceDescribe(entity.getServiceDescribe()).serviceState(entity.getServiceState())
                .build();
    }
}
