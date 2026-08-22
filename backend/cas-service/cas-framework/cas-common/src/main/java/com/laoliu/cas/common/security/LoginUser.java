package com.laoliu.cas.common.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录用户上下文载体（通用组件，跨模块复用）。
 *
 * @author forever-king
 */
@Data
public class LoginUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private Integer role;

    private String email;

    private Map<String, Object> info;

    private LocalDateTime expiresTime;

    @JsonIgnore
    private Map<String, Object> context;

    public void setContext(String key, Object value) {
        if (context == null) {
            context = new HashMap<>();
        }
        context.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getContext(String key, Class<T> type) {
        if (context == null) {
            return null;
        }
        Object value = context.get(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    public String getRoleName() {
        if (role == null) {
            return null;
        }
        return switch (role) {
            case 0 -> "普通用户";
            case 1 -> "管理员";
            case 2 -> "超级管理员";
            default -> "未知";
        };
    }

    public boolean isAdmin() {
        return role != null && (role == 1 || role == 2);
    }
}
