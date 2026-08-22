package com.kb.infrastructure.persistence.mysql;

import com.kb.domain.user.KbUser;
import com.kb.domain.user.UserRepository;
import com.kb.infrastructure.persistence.mysql.dataobject.UserDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户仓储实现
 *
 * @author forever-king
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;

    @Override
    public Optional<KbUser> findByUsername(String username) {
        UserDO userDO = userMapper.selectByUsername(username);
        if (userDO == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(userDO));
    }

    @Override
    public Optional<KbUser> findById(Long id) {
        UserDO userDO = userMapper.selectById(id);
        if (userDO == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(userDO));
    }

    @Override
    public Optional<KbUser> findByEmail(String email) {
        UserDO userDO = userMapper.selectByEmail(email);
        if (userDO == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(userDO));
    }

    @Override
    public KbUser save(KbUser user) {
        UserDO userDO = new UserDO();
        userDO.setId(user.getId());
        userDO.setUsername(user.getUsername());
        userDO.setEmail(user.getEmail());
        userDO.setPasswordHash(user.getPasswordHash());
        userDO.setNickname(user.getNickname());
        userDO.setRole(user.getRole());
        userDO.setEnabled(user.getEnabled());
        if (userDO.getId() == null) {
            userMapper.insert(userDO);
        } else {
            userMapper.updateById(userDO);
        }
        return toDomain(userDO);
    }

    @Override
    public List<KbUser> findAll() {
        return userMapper.selectList(null).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long count() {
        return userMapper.selectCount(null);
    }

    private KbUser toDomain(UserDO userDO) {
        return KbUser.builder()
                .id(userDO.getId())
                .username(userDO.getUsername())
                .email(userDO.getEmail())
                .passwordHash(userDO.getPasswordHash())
                .nickname(userDO.getNickname())
                .role(userDO.getRole())
                .enabled(userDO.getEnabled())
                .createdAt(userDO.getCreatedAt())
                .build();
    }
}
