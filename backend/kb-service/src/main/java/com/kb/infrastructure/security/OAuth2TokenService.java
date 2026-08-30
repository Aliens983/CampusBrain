package com.kb.infrastructure.security;

import com.kb.infrastructure.persistence.mysql.dataobject.OAuth2AccessTokenDO;

/**
 * OAuth2 Token 服务接口
 * <p>
 * 提供双 Token 的创建、校验、刷新、删除功能
 * 实现为 OAuth2TokenServiceImpl
 * </p>
 *
 * @author forever-king
 */
public interface OAuth2TokenService {

    /**
     * 创建双 Token（Access + Refresh）
     *
     * @param userId 用户ID
     * @param username 用户名
     * @param role 用户角色
     * @param nickname 用户昵称
     * @return 创建的 Access Token（含关联的 refreshToken 值）
     */
    OAuth2AccessTokenDO createTokens(Long userId, String username, String role, String nickname);

    /**
     * 校验 Access Token
     *
     * @param accessToken Token 字符串
     * @return 有效的 Token 记录
     * @throws RuntimeException Token 不存在或已过期
     */
    OAuth2AccessTokenDO checkAccessToken(String accessToken);

    /**
     * 用 Refresh Token 刷新 Access Token
     *
     * @param refreshToken 刷新令牌
     * @return 新的 Access Token
     */
    OAuth2AccessTokenDO refreshAccessToken(String refreshToken);

    /**
     * 删除 Access Token（登出）
     *
     * @param accessToken Token 字符串
     */
    void removeAccessToken(String accessToken);
}
