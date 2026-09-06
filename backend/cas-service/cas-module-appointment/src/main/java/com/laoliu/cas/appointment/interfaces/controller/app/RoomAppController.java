package com.laoliu.cas.appointment.interfaces.controller.app;

import com.laoliu.cas.appointment.application.service.impl.RoomServiceImpl;
import com.laoliu.cas.appointment.interfaces.dto.request.RoomBookRequest;
import com.laoliu.cas.appointment.interfaces.dto.response.RoomResponse;
import com.laoliu.cas.common.api.GetUserIdViaTokenApi;
import com.laoliu.cas.common.result.CommonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教室查询/教室时段预约接口（用户）
 *
 * @author forever-king
 */
@Tag(name = "教室（用户）")
@RestController
@RequestMapping("/app/rooms")
@RequiredArgsConstructor
public class RoomAppController {

    private final RoomServiceImpl roomService;
    private final GetUserIdViaTokenApi getUserIdViaTokenApi;

    @Operation(summary = "某服务下的教室列表", description = "按所属服务（空闲教室）查询教室")
    @GetMapping
    public CommonResult<List<RoomResponse>> listRooms(@RequestParam(required = false) Long serviceId) {
        return CommonResult.success(roomService.listByService(serviceId));
    }

    @Operation(summary = "预约教室时段", description = "一间教室同一时间段只允许一人；自选日期与起止时段")
    @PostMapping("/{roomId}/book")
    public CommonResult<Void> bookRoom(
            @Parameter(description = "教室ID", required = true) @PathVariable Long roomId,
            @Valid @RequestBody RoomBookRequest request) {
        Long userId = getUserIdViaTokenApi.getUserId();
        roomService.bookRoom(userId, roomId, request);
        return CommonResult.success("教室预约已提交，等待管理员审核", null);
    }
}
