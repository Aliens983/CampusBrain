package com.laoliu.cas.appointment.domain.entity;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 服务领域实体
 * 纯净，不依赖任何框架注解
 *
 * @author forever-king
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode()
@ToString
@Builder
public class Service implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 服务ID */
    private Long serviceId;
    /** 服务名称 */
    private String serviceName;
    /** 服务描述 */
    private String serviceDescribe;
    /** 服务状态 */
    private Integer serviceState;

    /**
     * 领域行为：检查服务是否可用
     */
    public boolean isAvailable() {
        return Objects.equals(this.serviceState, 1);
    }

    /**
     * 领域行为：禁用服务
     */
    public void disable() {
        this.serviceState = 0;
    }

    /**
     * 领域行为：启用服务
     */
    public void enable() {
        this.serviceState = 1;
    }
}
