package com.laoliu.cas.system.domain.entity;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户领域实体
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
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long id;

    /** 姓名 */
    private String name;

    /** 年级 */
    private String grade;

    /** 性别 */
    private String sex;

    /** 年龄 */
    private Integer age;

    /** 邮箱 */
    private String email;

    /** 密码（BCrypt 哈希）：永不进日志，toString 排除，避免误打实体时外泄 */
    @ToString.Exclude
    private String password;

    /** 角色 */
    private Integer role;
}
