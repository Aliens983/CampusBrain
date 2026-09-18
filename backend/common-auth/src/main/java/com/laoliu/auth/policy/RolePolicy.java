package com.laoliu.auth.policy;

/**
 * 全局角色策略（Q-02 收敛的唯一角色映射源）
 * <p>
 * 三个服务（gateway / cas / kb）此前各自维护角色判据：
 * CAS 用数字 0/1/2/3，KB 反向硬编码 {@code "1"/"2" -> ADMIN}，
 * 数字→权限名的 switch 又在 JWTFilter 里复制一份。新增角色或调整语义时
 * 必须同时改多处。现统一为：
 * <ul>
 *   <li>线上契约（JWT claim {@code role}、网关内网头 {@code X-User-Role}）只用数字 code；</li>
 *   <li>数字 code 与 Spring Security 权限名 {@code ROLE_*} 的映射只在此处定义；</li>
 *   <li>管理员判定（1 管理员 / 2 超级管理员）也只在此处定义。</li>
 * </ul>
 *
 * @author forever-king
 */
public enum RolePolicy {

    /** 普通用户 */
    USER(0, "ROLE_USER"),
    /** 管理员 */
    ADMIN(1, "ROLE_ADMIN"),
    /** 超级管理员（同样具备管理员权限） */
    SUPER_ADMIN(2, "ROLE_SUPER_ADMIN"),
    /** 教师 */
    TEACHER(3, "ROLE_TEACHER");

    private final int code;
    private final String authority;

    RolePolicy(int code, String authority) {
        this.code = code;
        this.authority = authority;
    }

    public int getCode() {
        return code;
    }

    public String getAuthority() {
        return authority;
    }

    /**
     * 数字 code 解析为策略；null 或未定义的 code 一律按普通用户处理
     * （权限收敛方向：解析失败只给最小权限，绝不抬权）。
     */
    public static RolePolicy of(Integer code) {
        if (code == null) {
            return USER;
        }
        for (RolePolicy policy : values()) {
            if (policy.code == code) {
                return policy;
            }
        }
        return USER;
    }

    /**
     * 解析网关透传的数字角色头（{@code X-User-Role}，必为整数字符串）。
     * 非法值按 {@link #of(Integer)} 的最小权限原则降级为普通用户。
     */
    public static RolePolicy ofHeader(String numericRole) {
        if (numericRole == null || numericRole.isBlank()) {
            return USER;
        }
        try {
            return of(Integer.parseInt(numericRole.trim()));
        } catch (NumberFormatException e) {
            return USER;
        }
    }

    /** 管理员（管理员 / 超级管理员）判定，全服务唯一口径 */
    public static boolean isAdminCode(Integer code) {
        return code != null && (code == ADMIN.code || code == SUPER_ADMIN.code);
    }
}
