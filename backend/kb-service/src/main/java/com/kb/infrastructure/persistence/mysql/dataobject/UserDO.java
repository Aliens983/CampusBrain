package com.kb.infrastructure.persistence.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户 Data Object — 映射 kb_user 表
 *
 * @author forever-king
 */
@Data
@TableName("kb_user")
public class UserDO {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    private String username;

    /** 邮箱 */
    private String email;

    /** BCrypt 加密密码 */
    private String passwordHash;

    /** 昵称 */
    private String nickname;

    /** 角色：ADMIN / USER */
    private String role;

    /** 是否启用 */
    private Boolean enabled;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
