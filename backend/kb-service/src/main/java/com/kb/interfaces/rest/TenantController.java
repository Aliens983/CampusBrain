package com.kb.interfaces.rest;

import com.kb.domain.tenant.Tenant;
import com.kb.domain.tenant.TenantRepository;
import com.kb.infrastructure.security.SecurityFrameworkUtils;
import com.kb.interfaces.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 租户/工作空间管理 API。
 * <p>
 * <b>租户切换机制：</b>前端在后续请求中通过 HTTP Header 传递
 * {@code X-Tenant-Id: <tenantId>} 来指定当前工作空间。
 * 后端 {@code TenantFilter} 自动解析并写入 {@code TenantContext}。
 * 不提供服务端 session 级切换——每次请求独立携带租户上下文。
 * </p>
 *
 * @author forever-king
 */
@Tag(name = "工作空间管理", description = "多租户工作空间的创建和查询。" +
        " 切换工作空间请通过 HTTP Header 'X-Tenant-Id' 传递。")
@RestController
@RequestMapping("/kb/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantRepository tenantRepository;

    @Operation(summary = "创建工作空间", description = "创建新的组织/工作空间，创建者自动成为管理员")
    @PostMapping
    public ApiResponse<Map<String, Object>> createTenant(@RequestBody Map<String, Object> body) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        String name = (String) body.get("name");
        String code = (String) body.getOrDefault("code",
                name != null ? name.toLowerCase().replaceAll("\\s+", "-") : "default");

        if (tenantRepository.findByCode(code).isPresent()) {
            return ApiResponse.badRequest("工作空间编码已被使用");
        }

        Tenant tenant = Tenant.builder()
                .name(name)
                .code(code)
                .ownerId(userId)
                .status("ACTIVE")
                .maxMembers(50)
                .build();

        tenant = tenantRepository.save(tenant);

        return ApiResponse.success(Map.of(
                "id", tenant.getId(),
                "name", tenant.getName(),
                "code", tenant.getCode(),
                "status", tenant.getStatus(),
                "usageNote", "请在后续请求中设置 HTTP Header: X-Tenant-Id: " + tenant.getId()
        ));
    }

    @Operation(summary = "获取我的工作空间", description = "获取当前用户拥有的所有工作空间")
    @GetMapping("/mine")
    public ApiResponse<List<Map<String, Object>>> myTenants() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        List<Map<String, Object>> list = tenantRepository.findByOwnerId(userId).stream()
                .map(t -> Map.<String, Object>of(
                        "id", t.getId(),
                        "name", t.getName(),
                        "code", t.getCode(),
                        "status", t.getStatus(),
                        "maxMembers", t.getMaxMembers()
                ))
                .toList();
        return ApiResponse.success(list);
    }

    @Operation(summary = "获取当前工作空间", description = "获取当前 X-Tenant-Id Header 对应的工作空间信息")
    @GetMapping("/current")
    public ApiResponse<Map<String, Object>> currentTenant() {
        Long tenantId = com.kb.infrastructure.tenant.TenantContext.getTenantId();
        if (tenantId == null) {
            return ApiResponse.success(Map.of("mode", "personal", "note", "未指定工作空间，使用个人模式"));
        }
        return tenantRepository.findById(tenantId)
                .map(t -> Map.<String, Object>of(
                        "id", t.getId(), "name", t.getName(),
                        "code", t.getCode(), "status", t.getStatus()))
                .map(ApiResponse::success)
                .orElse(ApiResponse.notFound("工作空间不存在"));
    }
}
