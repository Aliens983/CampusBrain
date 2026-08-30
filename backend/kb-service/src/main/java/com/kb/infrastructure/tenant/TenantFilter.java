package com.kb.infrastructure.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 租户拦截过滤器 — 从 HTTP Header 中解析租户上下文
 * <p>
 * 优先级仅次于 TraceIdFilter，确保后续所有组件都能访问租户信息
 * </p>
 *
 * <h3>Header 格式</h3>
 * <pre>
 * X-Tenant-Id: 123
 * # 或
 * X-Tenant-Code: acme-corp
 * </pre>
 * 若两者都不传，默认为 null（全局/个人模式，兼容单租户部署）
 * </p>
 *
 * @author forever-king
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TenantFilter extends OncePerRequestFilter {

    private static final String HEADER_TENANT_ID = "X-Tenant-Id";
    private static final String HEADER_TENANT_CODE = "X-Tenant-Code";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String tenantIdStr = request.getHeader(HEADER_TENANT_ID);
            String tenantCode = request.getHeader(HEADER_TENANT_CODE);

            if (tenantIdStr != null && !tenantIdStr.isBlank()) {
                try {
                    TenantContext.setTenant(Long.parseLong(tenantIdStr.trim()));
                } catch (NumberFormatException e) {
                    log.warn("Invalid X-Tenant-Id header: {}", tenantIdStr);
                }
            }

            if (tenantCode != null && !tenantCode.isBlank()) {
                TenantContext.setTenantCode(tenantCode.trim());
            }

            if (TenantContext.hasTenant()) {
                log.debug("Request scoped to tenant: id={}, code={}",
                        TenantContext.getTenantId(), TenantContext.getTenantCode());
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
