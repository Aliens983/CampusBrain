package com.laoliu.cas.system.application.service;

import com.laoliu.cas.common.result.PageResult;
import com.laoliu.cas.system.interfaces.dto.response.BookingRecordRespVO;
import com.laoliu.cas.system.interfaces.dto.response.UserInfoAndServicesViaMPRespVO;

/**
 * @author forever-king
 */
public interface UserService {
    /** 获取用户信息和预约记录 */
    UserInfoAndServicesViaMPRespVO getUserInfoAndBookings(Long userId);

    /**
     * 分页获取用户的预约记录。
     */
    PageResult<BookingRecordRespVO> getUserBookings(Long userId, int page, int pageSize);
}
