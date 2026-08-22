package com.kb.infrastructure.persistence.mysql.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Refresh Token Data Object — UUID 长有效期令牌
 *
 * @author forever-king
 */
@Data
@TableName("oauth2_refresh_token")
public class OAuth2RefreshTokenDO {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** UUID 刷新令牌（32位无连字符） */
    private String refreshToken;

    /** 用户ID */
    private Long userId;

    /** 用户角色 */
    private String role;

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
