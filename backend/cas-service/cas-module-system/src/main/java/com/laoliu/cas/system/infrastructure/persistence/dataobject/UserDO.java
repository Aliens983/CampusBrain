package com.laoliu.cas.system.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.laoliu.cas.system.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户数据对象 - 用于 MyBatis-Plus ORM
 *
 * @author forever-king
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("user")
public class UserDO implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    private String name;

    /** 年级 */
    private String grade;

    /** 性别 */
    private String sex;

    /** 年龄 */
    private Integer age;

    /** 邮箱 */
    private String email;

    /** 密码 */
    private String password;

    /** 角色（1-普通用户，2-管理员，3-超级管理员） */
    private Integer role;

    public User toEntity() {
        return User.builder()
                .id(id).name(name).grade(grade).sex(sex).age(age)
                .email(email).password(password).role(role)
                .build();
    }

    public static UserDO fromEntity(User entity) {
        if (entity == null) {
            return null;
        }
        return UserDO.builder()
                .id(entity.getId()).name(entity.getName()).grade(entity.getGrade())
                .sex(entity.getSex()).age(entity.getAge()).email(entity.getEmail())
                .password(entity.getPassword()).role(entity.getRole())
                .build();
    }
}
