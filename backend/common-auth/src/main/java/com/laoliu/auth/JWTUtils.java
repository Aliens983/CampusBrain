package com.laoliu.auth;

import com.laoliu.auth.dto.LoginUser;
import com.laoliu.auth.exception.AuthException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * JWT 工具类（统一认证组件，供网关与各服务复用）
 *
 * @author forever-king
 */
@Slf4j
@Component
public class JWTUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    public String generateToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateToken(LoginUser loginUser) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(loginUser.getId()))
                .claim("userId", loginUser.getId())
                .claim("name", loginUser.getName())
                .claim("role", loginUser.getRole())
                .claim("email", loginUser.getEmail())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public Claims parseToken(String token) {
        try {
            Jws<Claims> claimsJws = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build().parseSignedClaims(token);

            return claimsJws.getPayload();
        } catch (ExpiredJwtException e) {
            log.error("JWT token has expired: {}", e.getMessage());
            throw new AuthException(401, "登录已过期");
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
            throw new AuthException(401, "无效的登录凭证");
        } catch (MalformedJwtException e) {
            log.error("Malformed JWT token: {}", e.getMessage());
            throw new AuthException(401, "无效的登录凭证");
        } catch (IllegalArgumentException e) {
            log.error("JWT token is empty: {}", e.getMessage());
            throw new AuthException(401, "无效的登录凭证");
        } catch (Exception e) {
            log.error("Error parsing JWT token: {}", e.getMessage());
            throw new AuthException(401, "无效的登录凭证");
        }
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.get("userId").toString());
    }

    public LoginUser getLoginUserFromToken(String token) {
        try {
            Claims claims = parseToken(token);

            LoginUser loginUser = new LoginUser();
            loginUser.setId(Long.parseLong(claims.get("userId").toString()));

            Object nameObj = claims.get("name");
            if (nameObj != null) {
                loginUser.setName(nameObj.toString());
            }

            Object roleObj = claims.get("role");
            if (roleObj != null) {
                if (roleObj instanceof Number) {
                    loginUser.setRole(((Number) roleObj).intValue());
                } else {
                    loginUser.setRole(Integer.parseInt(roleObj.toString()));
                }
            }

            Object emailObj = claims.get("email");
            if (emailObj != null) {
                loginUser.setEmail(emailObj.toString());
            }

            return loginUser;
        } catch (Exception e) {
            log.error("Error extracting user from token: {}", e.getMessage());
            return null;
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] digest = md.digest(keyBytes);
            return Keys.hmacShaKeyFor(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 algorithm not available", e);
        }
    }
}
