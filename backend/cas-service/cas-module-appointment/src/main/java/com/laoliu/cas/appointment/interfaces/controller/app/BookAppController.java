package com.laoliu.cas.appointment.interfaces.controller.app;

import com.laoliu.cas.appointment.application.service.BookService;
import com.laoliu.cas.appointment.interfaces.dto.request.BookServiceRequest;
import com.laoliu.cas.appointment.interfaces.dto.response.BookingDTO;
import com.laoliu.cas.appointment.interfaces.dto.response.BookResultResponse;
import com.laoliu.cas.common.api.GetUserIdViaTokenApi;
import com.laoliu.cas.common.pojo.PageParam;
import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.common.result.PageResult;
import com.laoliu.cas.system.api.dto.UserInfoDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * 用户端预约接口
 *
 * @author forever-king
 */
@Tag(name = "预约服务（用户）")
@RestController
@RequestMapping("/app/bookings")
@RequiredArgsConstructor
public class BookAppController {

    private final BookService bookService;
    private final GetUserIdViaTokenApi getUserIdViaTokenApi;

    @Operation(summary = "预定服务", description = "用户预约多个服务，传入服务ID列表")
    @PostMapping
    public CommonResult<BookResultResponse> bookService(
            @Valid @RequestBody BookServiceRequest request) {
        Long userId = getUserIdViaTokenApi.getUserId();
        UserInfoDTO userInfo = bookService.bookService(userId, request.getServiceIds());
        return CommonResult.success("预约成功", buildBookResult(userInfo, userId));
    }

    @Operation(summary = "查看所有预约（分页）", description = "分页获取当前用户的所有预约记录")
    @GetMapping
    public CommonResult<PageResult<BookingDTO>> getBook(@Valid PageParam pageParam) {
        Long userId = getUserIdViaTokenApi.getUserId();
        var page = bookService.getAllBookings(userId, pageParam.getPageNo(), pageParam.getPageSize());
        return CommonResult.success(PageResult.of(page));
    }

    @Operation(summary = "取消预约", description = "取消用户已预约的服务，传入预约ID")
    @PatchMapping("/{id}/cancel")
    public CommonResult<Void> cancelBooking(
            @Parameter(description = "预约ID", required = true) @PathVariable Long id) {
        Long userId = getUserIdViaTokenApi.getUserId();
        boolean success = bookService.cancelBookings(userId, Collections.singletonList(id));
        if (!success) {
            return CommonResult.badRequest("取消预约失败");
        }
        return CommonResult.success("取消预约成功", null);
    }

    @Operation(summary = "获取预约详情", description = "根据预约ID获取单条预约的详细信息")
    @GetMapping("/{id}")
    public CommonResult<BookingDTO> getBookingDetail(@PathVariable Long id) {
        Long userId = getUserIdViaTokenApi.getUserId();
        BookingDTO booking = bookService.getBookingById(userId, id);
        if (booking == null) {
            return CommonResult.notFound("预约记录不存在");
        }
        return CommonResult.success(booking);
    }

    // 注：原 /room、/equipment、/consultation 三个「专项预约」端点已移除。
    // 它们把 roomId / equipmentId / consultantId 误当作 serviceId 直接传入 bookService，
    // 且丢弃 date / startTime / endTime / purpose 等预约要素，语义错误且是误用陷阱。
    // 资源类预约请改用各自的正确端点：
    //   · 教室 → RoomAppController         POST /app/rooms/{roomId}/book
    //   · 设备 → EquipmentAppController    POST /app/equipment/{equipmentId}/book
    //   · 咨询 → ConsultationAppController POST /app/consultations/{consultantId}/book

    /**
     * 构建预约结果响应
     */
    private BookResultResponse buildBookResult(UserInfoDTO userInfo, Long userId) {
        return BookResultResponse.builder()
                .username(userInfo.getName())
                .email(userInfo.getEmail())
                .grade(userInfo.getGrade())
                .allBookedServices(bookService.getAllBookings(userId))
                .build();
    }
}
