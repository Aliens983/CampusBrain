package com.laoliu.cas.system.application.service;

import com.laoliu.cas.common.result.PageResult;
import com.laoliu.cas.system.interfaces.dto.response.BookingRecordResponse;
import com.laoliu.cas.system.interfaces.dto.response.UserInfoAndServicesViaMPResponse;

/**
 * @author forever-king
 */
public interface UserService {
    /** 获取用户信息和预约记录 */
    UserInfoAndServicesViaMPResponse getUserInfoAndBookings(Long userId);

    /**
     * 分页获取用户的预约记录
     */
    PageResult<BookingRecordResponse> getUserBookings(Long userId, int page, int pageSize);
}
