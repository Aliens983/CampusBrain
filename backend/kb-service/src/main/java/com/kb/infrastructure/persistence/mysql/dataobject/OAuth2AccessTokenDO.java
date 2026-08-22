package com.kb.infrastructure.persistence.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Access Token Data Object — UUID 短有效期令牌
 *
 * @author forever-king
 */
@Data
@TableName("oauth2_access_token")
@JsonIgnoreProperties(ignoreUnknown = true)
public class OAuth2AccessTokenDO {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** UUID 访问令牌（32位无连字符） */
    private String accessToken;

    /** 关联的刷新令牌 */
    private String refreshToken;

    /** 用户ID */
    private Long userId;

    /** 用户角色 */
    private String role;

    /** 预加载的用户昵称 */
    private String nickname;

    /** 过期时间 */
    private LocalDateTime expiresTime;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /**
     * 判断是否已过期
     */
    public boolean isExpired() {
        return expiresTime != null && expiresTime.isBefore(LocalDateTime.now());
    }
}
