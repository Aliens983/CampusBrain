package com.laoliu.cas.system.domain.repository;

import com.laoliu.cas.system.domain.entity.User;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.system.interfaces.dto.response.BookingRecordRespVO;

import java.util.List;
import java.util.Optional;

/**
 * 用户领域仓库接口
 *
 * @author forever-king
 */
public interface UserRepository {

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    List<User> findAll();

    User save(User user);

    void updatePasswordByEmail(String email, String encodedPassword);

    void updatePasswordById(Long userId, String encodedPassword);

    String getEncodePasswordByEmail(String email);

    String getEncodePasswordById(Long userId);

    String getRoleByUserId(Long userId);

    Long getUserIdByEmail(String email);

    void updateRoleToCommonUser(Long userId);

    void updateRoleToAdmin(Long userId);

    List<User> getAllUsers();

    /**
     * 分页查询所有用户（支持按姓名、邮箱模糊搜索，按角色筛选）。
     */
    IPage<User> getAllUsers(int page, int pageSize, String name, String email, Integer role);

    List<BookingRecordRespVO> getAllBookings(Long userId);

    /**
     * 分页查询用户的预约记录。
     */
    IPage<BookingRecordRespVO> getAllBookings(Long userId, int page, int pageSize);
}
