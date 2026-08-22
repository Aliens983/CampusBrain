package com.laoliu.cas.appointment.interfaces.controller.app;

import com.laoliu.cas.appointment.application.service.BookService;
import com.laoliu.cas.appointment.interfaces.dto.request.BookServiceRequest;
import com.laoliu.cas.appointment.interfaces.dto.request.SpecializedBookingRequest;
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
 * 用户端预约接口。
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

    @Operation(summary = "预约会议室", description = "预约会议室服务")
    @PostMapping("/room")
    public CommonResult<BookResultResponse> bookRoom(@Valid @RequestBody SpecializedBookingRequest request) {
        Long serviceId = request.extractServiceId();
        if (serviceId == null) {
            return CommonResult.badRequest("会议室ID不能为空");
        }
        return doSpecializedBooking(serviceId, "会议室预约成功");
    }

    @Operation(summary = "预约设备", description = "预约设备借用服务")
    @PostMapping("/equipment")
    public CommonResult<BookResultResponse> bookEquipment(@Valid @RequestBody SpecializedBookingRequest request) {
        Long serviceId = request.extractServiceId();
        if (serviceId == null) {
            return CommonResult.badRequest("设备ID不能为空");
        }
        return doSpecializedBooking(serviceId, "设备预约成功");
    }

    @Operation(summary = "预约咨询", description = "预约咨询服务")
    @PostMapping("/consultation")
    public CommonResult<BookResultResponse> bookConsultation(@Valid @RequestBody SpecializedBookingRequest request) {
        Long serviceId = request.extractServiceId();
        if (serviceId == null) {
            return CommonResult.badRequest("咨询师ID不能为空");
        }
        return doSpecializedBooking(serviceId, "咨询预约成功");
    }

    /**
     * 执行专项预约并构建响应
     */
    private CommonResult<BookResultResponse> doSpecializedBooking(Long serviceId, String successMsg) {
        Long userId = getUserIdViaTokenApi.getUserId();
        UserInfoDTO userInfo = bookService.bookService(userId, Collections.singletonList(serviceId));
        return CommonResult.success(successMsg, buildBookResult(userInfo, userId));
    }

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
