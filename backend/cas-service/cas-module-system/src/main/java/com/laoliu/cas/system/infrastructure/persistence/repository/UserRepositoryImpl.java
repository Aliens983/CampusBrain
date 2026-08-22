package com.laoliu.cas.system.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laoliu.cas.system.domain.entity.User;
import com.laoliu.cas.system.domain.repository.UserRepository;
import com.laoliu.cas.system.infrastructure.persistence.dataobject.BookingRecordDO;
import com.laoliu.cas.system.infrastructure.persistence.dataobject.UserDO;
import com.laoliu.cas.system.infrastructure.persistence.mapper.UserMapper;
import com.laoliu.cas.system.interfaces.convert.BookingRecordConvert;
import com.laoliu.cas.system.interfaces.dto.response.BookingRecordRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户仓库实现
 *
 * @author forever-king
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userMapper.selectById(id))
                .map(UserDO::toEntity);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        Long userId = userMapper.getUserIdByEmail(email);
        if (userId == null) {
            return Optional.empty();
        }
        return findById(userId);
    }

    @Override
    public List<User> findAll() {
        return userMapper.selectList(null).stream()
                .map(UserDO::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public User save(User user) {
        UserDO dataObject = UserDO.fromEntity(user);
        if (user.getId() == null) {
            userMapper.insert(dataObject);
            user.setId(dataObject.getId());
        } else {
            userMapper.updateById(dataObject);
        }
        return user;
    }

    @Override
    public void updatePasswordByEmail(String email, String encodedPassword) {
        userMapper.updatePasswordByEmail(email, encodedPassword);
    }

    @Override
    public void updatePasswordById(Long userId, String encodedPassword) {
        userMapper.updatePasswordById(userId, encodedPassword);
    }

    @Override
    public String getEncodePasswordByEmail(String email) {
        return userMapper.getEncodePasswordByEmail(email);
    }

    @Override
    public String getEncodePasswordById(Long userId) {
        return userMapper.getEncodePasswordById(userId);
    }

    @Override
    public String getRoleByUserId(Long userId) {
        return userMapper.getRoleByUserId(userId);
    }

    @Override
    public Long getUserIdByEmail(String email) {
        return userMapper.getUserIdByEmail(email);
    }

    @Override
    public void updateRoleToCommonUser(Long userId) {
        userMapper.updateRoleToCommonUser(userId);
    }

    @Override
    public void updateRoleToAdmin(Long userId) {
        userMapper.updateRoleToAdmin(userId);
    }

    @Override
    public List<User> getAllUsers() {
        return userMapper.getAllUsers().stream()
                .map(UserDO::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public IPage<User> getAllUsers(int page, int pageSize, String name, String email, Integer role) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(UserDO::getName, name);
        }
        if (StringUtils.hasText(email)) {
            wrapper.like(UserDO::getEmail, email);
        }
        if (role != null) {
            wrapper.eq(UserDO::getRole, role);
        }
        wrapper.orderByDesc(UserDO::getId);

        Page<UserDO> pageParam = new Page<>(page, pageSize);
        IPage<UserDO> doPage = userMapper.selectPage(pageParam, wrapper);
        return doPage.convert(UserDO::toEntity);
    }

    @Override
    public List<BookingRecordRespVO> getAllBookings(Long userId) {
        return BookingRecordConvert.INSTANCE.convertList(userMapper.getAllBookings(userId));
    }

    @Override
    public IPage<BookingRecordRespVO> getAllBookings(Long userId, int page, int pageSize) {
        Page<BookingRecordDO> pageParam = new Page<>(page, pageSize);
        IPage<BookingRecordDO> doPage = userMapper.getAllBookingsWithPage(userId, pageParam);
        return doPage.convert(BookingRecordConvert.INSTANCE::convert);
    }
}
