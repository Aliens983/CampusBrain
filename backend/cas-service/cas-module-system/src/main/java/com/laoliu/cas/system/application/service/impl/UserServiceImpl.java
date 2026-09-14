package com.laoliu.cas.system.application.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.common.result.PageResult;
import com.laoliu.cas.system.application.service.UserService;
import com.laoliu.cas.system.domain.entity.User;
import com.laoliu.cas.system.domain.repository.UserRepository;
import com.laoliu.cas.system.interfaces.dto.response.BookingRecordResponse;
import com.laoliu.cas.system.interfaces.dto.response.UserInfoAndServicesViaMPResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author forever-king
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserInfoAndServicesViaMPResponse getUserInfoAndBookings(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        UserInfoAndServicesViaMPResponse respVO = new UserInfoAndServicesViaMPResponse();
        respVO.setUser(user);
        respVO.setBookings(userRepository.getAllBookings(userId));
        return respVO;
    }

    @Override
    public PageResult<BookingRecordResponse> getUserBookings(Long userId, int page, int pageSize) {
        IPage<BookingRecordResponse> bookingsPage = userRepository.getAllBookings(userId, page, pageSize);
        return PageResult.of(bookingsPage);
    }
}
