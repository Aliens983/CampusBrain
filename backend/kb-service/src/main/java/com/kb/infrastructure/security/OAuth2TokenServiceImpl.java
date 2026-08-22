package com.kb.infrastructure.security;

import cn.hutool.core.util.IdUtil;
import com.kb.infrastructure.persistence.mysql.OAuth2AccessTokenMapper;
import com.kb.infrastructure.persistence.mysql.OAuth2RefreshTokenMapper;
import com.kb.infrastructure.persistence.mysql.dataobject.OAuth2AccessTokenDO;
import com.kb.infrastructure.persistence.mysql.dataobject.OAuth2RefreshTokenDO;
import com.kb.infrastructure.persistence.redis.OAuth2AccessTokenRedisDAO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * OAuth2 Token 服务实现
 * <p>
 * 核心逻辑：
 * <ul>
 *   <li>创建：先生成 Refresh Token → 再生成 Access Token（关联它）</li>
 *   <li>校验：Redis 优先 → MySQL 兜底 → 回写 Redis</li>
 *   <li>刷新：删旧 Access Token → 创建新 Access Token（Refresh Token 不轮换）</li>
 *   <li>删除：删 Access Token（MySQL + Redis）+ 级联删 Refresh Token</li>
 * </ul>
 * </p>
 *
 * @author forever-king
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2TokenServiceImpl implements OAuth2TokenService {

    private final OAuth2AccessTokenMapper accessTokenMapper;
    private final OAuth2RefreshTokenMapper refreshTokenMapper;
    private final OAuth2AccessTokenRedisDAO redisDAO;

    /** Access Token 有效期（秒），默认 2 小时 */
    @Value("${app.security.access-token-validity-seconds:7200}")
    private int accessTokenValiditySeconds;

    /** Refresh Token 有效期（秒），默认 7 天 */
    @Value("${app.security.refresh-token-validity-seconds:604800}")
    private int refreshTokenValiditySeconds;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OAuth2AccessTokenDO createTokens(Long userId, String username,
                                             String role, String nickname) {
        OAuth2RefreshTokenDO refreshDO = createRefreshToken(userId, role);
        return createAccessToken(refreshDO, username, role, nickname);
    }

    @Override
    public OAuth2AccessTokenDO checkAccessToken(String accessToken) {
        OAuth2AccessTokenDO token = redisDAO.get(accessToken);
        if (token != null && !token.isExpired()) {
            return token;
        }
        token = accessTokenMapper.selectByAccessToken(accessToken);
        if (token == null) {
            throw new RuntimeException("访问令牌不存在");
        }
        if (token.isExpired()) {
            throw new RuntimeException("访问令牌已过期");
        }
        redisDAO.set(token);
        return token;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OAuth2AccessTokenDO refreshAccessToken(String refreshToken) {
        OAuth2RefreshTokenDO refreshDO = refreshTokenMapper.selectByRefreshToken(refreshToken);
        if (refreshDO == null) {
            throw new RuntimeException("刷新令牌不存在");
        }
        if (refreshDO.isExpired()) {
            refreshTokenMapper.deleteByRefreshToken(refreshToken);
            throw new RuntimeException("刷新令牌已过期，请重新登录");
        }
        accessTokenMapper.deleteByRefreshToken(refreshToken);
        redisDAO.deleteByRefreshToken(refreshToken);
        return createAccessToken(refreshDO, null, refreshDO.getRole(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAccessToken(String accessToken) {
        OAuth2AccessTokenDO tokenDO = accessTokenMapper.selectByAccessToken(accessToken);
        if (tokenDO == null) {
            return;
        }
        accessTokenMapper.deleteById(tokenDO.getId());
        redisDAO.delete(accessToken);
        refreshTokenMapper.deleteByRefreshToken(tokenDO.getRefreshToken());
        log.info("Token removed: userId={}, accessToken={}", tokenDO.getUserId(),
                accessToken.substring(0, 8) + "...");
    }

    private OAuth2RefreshTokenDO createRefreshToken(Long userId, String role) {
        OAuth2RefreshTokenDO refreshDO = new OAuth2RefreshTokenDO();
        refreshDO.setRefreshToken(IdUtil.fastSimpleUUID());
        refreshDO.setUserId(userId);
        refreshDO.setRole(role);
        refreshDO.setExpiresTime(LocalDateTime.now().plusSeconds(refreshTokenValiditySeconds));
        refreshTokenMapper.insert(refreshDO);
        return refreshDO;
    }

    private OAuth2AccessTokenDO createAccessToken(OAuth2RefreshTokenDO refreshDO,
                                                    String username, String role,
                                                    String nickname) {
        OAuth2AccessTokenDO accessDO = new OAuth2AccessTokenDO();
        accessDO.setAccessToken(IdUtil.fastSimpleUUID());
        accessDO.setRefreshToken(refreshDO.getRefreshToken());
        accessDO.setUserId(refreshDO.getUserId());
        accessDO.setRole(role != null ? role : refreshDO.getRole());
        accessDO.setNickname(nickname);
        accessDO.setExpiresTime(LocalDateTime.now().plusSeconds(accessTokenValiditySeconds));
        accessTokenMapper.insert(accessDO);
        redisDAO.set(accessDO);
        return accessDO;
    }
}
