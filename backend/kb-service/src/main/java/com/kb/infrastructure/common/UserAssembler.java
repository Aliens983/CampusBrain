package com.kb.infrastructure.common;

import com.kb.domain.user.KbUser;

import java.util.Map;

/**
 * 用户领域对象 → DTO 转换器。
 * @author forever-king
 */
public final class UserAssembler {

    private UserAssembler() {}

    public static Map<String, Object> toDTO(KbUser user) {
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail() != null ? user.getEmail() : "",
                "role", user.getRole(),
                "nickname", user.getNickname() != null ? user.getNickname() : "",
                "enabled", user.getEnabled()
        );
    }
}
