package com.laoliu.cas.appointment.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.interfaces.dto.response.BookingResponse;
import com.laoliu.cas.system.api.dto.UserInfoDTO;

import java.util.List;

/**
 * 预约业务服务接口
 *
 * @author forever-king
 */
public interface BookService {

    /**
     * 下单结果：除用户信息外，携带本次实际新建的订单号列表（幂等去重掉的重复单不包含）。
     * 供调用方（含预约助手）拿到真实 orderId，而不是再去查"用户最新一单"。
     */
    record BookingSubmitResult(UserInfoDTO userInfo, List<Long> orderIds) {
    }

    /** 用户预约服务 */
    BookingSubmitResult bookService(Long userId, List<Long> serviceIds);

    /** 获取用户所有预约记录 */
    List<BookingResponse> getAllBookings(Long userId);

    /** 分页获取用户预约记录 */
    IPage<BookingResponse> getAllBookings(Long userId, int page, int pageSize);

    /** 取消预约 */
    boolean cancelBookings(Long userId, List<Long> bookingIds);

    /** 获取单个预约详情（限定归属用户，防止越权） */
    BookingResponse getBookingById(Long userId, Long orderId);
}
