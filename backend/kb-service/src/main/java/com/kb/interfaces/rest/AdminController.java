package com.kb.interfaces.rest;

import com.kb.domain.conversation.ConversationRepository;
import com.kb.domain.document.DocumentRepository;
import com.kb.domain.user.KbUser;
import com.kb.domain.user.UserRepository;
import com.kb.interfaces.dto.ApiResponse;
import com.kb.interfaces.dto.UserDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 管理后台 API — 仅 ADMIN 角色可访问。
 * @author forever-king
 */
@Tag(name = "管理后台", description = "用户管理、系统统计等管理接口（需要 ADMIN 权限）")
@RestController
@RequestMapping("/kb/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final ConversationRepository conversationRepository;
    private final StringRedisTemplate redisTemplate;

    @Operation(summary = "获取用户列表", description = "获取所有注册用户列表")
    @GetMapping("/users")
    public ApiResponse<List<UserDTO>> listUsers() {
        List<UserDTO> users = userRepository.findAll().stream()
                .map(UserDTO::from)
                .toList();
        return ApiResponse.success(users);
    }

    @Operation(summary = "更新用户状态", description = "启用或禁用指定用户")
    @PutMapping("/users/{id}")
    public ApiResponse<Void> updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {
        KbUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (updates.containsKey("enabled")) {
            user.setEnabled((Boolean) updates.get("enabled"));
        }
        if (updates.containsKey("role")) {
            user.setRole((String) updates.get("role"));
        }
        userRepository.save(user);
        return ApiResponse.success();
    }

    @Operation(summary = "系统统计", description = "获取用户数、文档数、问答量等统计信息")
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        String todayKey = "stats:requests:" + LocalDate.now();
        String todayStr = redisTemplate.opsForValue().get(todayKey);
        long todayRequests = todayStr != null ? Long.parseLong(todayStr) : 0;

        return ApiResponse.success(Map.of(
                "userCount", userRepository.count(),
                "documentCount", documentRepository.count(),
                "qaCount", conversationRepository.count(),
                "todayRequests", todayRequests
        ));
    }

    /** 记录一次 API 请求（由拦截器或 Aspect 调用） */
    public void incrementTodayRequests() {
        String todayKey = "stats:requests:" + LocalDate.now();
        redisTemplate.opsForValue().increment(todayKey);
        redisTemplate.expire(todayKey, 25, TimeUnit.HOURS);
    }
}
