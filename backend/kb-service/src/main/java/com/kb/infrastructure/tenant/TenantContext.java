package com.kb.infrastructure.tenant;

/**
 * 租户上下文 — 基于 ThreadLocal 的租户 ID 传递。
 * <p>
 * 在请求入口（{@link TenantFilter}）设置，请求结束自动清除。
 * 异步场景需配合 {@code TaskDecorator} 传递到子线程。
 * </p>
 *
 * <h3>使用方式</h3>
 * <pre>
 * Long tenantId = TenantContext.getTenantId();
 * if (tenantId != null) {
 *     // 在查询中附加 tenant_id 过滤条件
 * }
 * </pre>
 *
 * @author forever-king
 */
public final class TenantContext {

    private static final ThreadLocal<Long> TENANT_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> TENANT_CODE_HOLDER = new ThreadLocal<>();

    private TenantContext() {}

    /** 设置当前请求的租户 ID */
    public static void setTenant(Long tenantId) {
        TENANT_HOLDER.set(tenantId);
    }

    /** 设置当前请求的租户编码 */
    public static void setTenantCode(String code) {
        TENANT_CODE_HOLDER.set(code);
    }

    /** 获取当前租户 ID（可能为 null，表示全局模式） */
    public static Long getTenantId() {
        return TENANT_HOLDER.get();
    }

    /** 获取当前租户编码 */
    public static String getTenantCode() {
        return TENANT_CODE_HOLDER.get();
    }

    /** 清除（必须在请求结束时调用，防止内存泄漏） */
    public static void clear() {
        TENANT_HOLDER.remove();
        TENANT_CODE_HOLDER.remove();
    }

    /** 当前是否处于多租户模式 */
    public static boolean hasTenant() {
        return TENANT_HOLDER.get() != null;
    }
}
