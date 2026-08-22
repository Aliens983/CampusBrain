package com.laoliu.cas.appointment.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.appointment.interfaces.dto.response.BookingDTO;
import com.laoliu.cas.system.api.dto.UserInfoDTO;

import java.util.List;

/**
 * 预约业务服务接口
 *
 * @author forever-king
 */
public interface BookService {

    /** 用户预约服务 */
    UserInfoDTO bookService(Long userId, List<Long> serviceIds);

    /** 获取用户所有预约记录 */
    List<BookingDTO> getAllBookings(Long userId);

    /** 分页获取用户预约记录 */
    IPage<BookingDTO> getAllBookings(Long userId, int page, int pageSize);

    /** 取消预约 */
    boolean cancelBookings(Long userId, List<Long> bookingIds);

    /** 获取单个预约详情（限定归属用户，防止越权） */
    BookingDTO getBookingById(Long userId, Long orderId);
}
