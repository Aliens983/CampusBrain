package com.laoliu.cas.system.api.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户信息 DTO - 用于跨模块传输（2.3：随 cas-module-system-api 独立成 artifact，
 * 不再依赖 system 模块内部领域实体；实体 → DTO 的映射由 system 模块内的 API 实现负责）
 *
 * @author forever-king
 */
@Data
public class UserInfoDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String email;
    private Integer role;
    private Integer age;
    private String sex;
    private String grade;
}
