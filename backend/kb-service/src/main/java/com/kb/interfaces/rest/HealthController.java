package com.kb.interfaces.rest;

import com.kb.interfaces.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health check endpoint.
 * @author forever-king
 */
@Tag(name = "系统健康", description = "服务健康检查接口")
@RestController
@RequestMapping("/kb")
public class HealthController {

    @Operation(summary = "健康检查", description = "返回服务运行状态和版本信息")
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(Map.of(
                "status", "UP",
                "service", "knowledge-base-platform",
                "version", "1.0.0"
        ));
    }
}
