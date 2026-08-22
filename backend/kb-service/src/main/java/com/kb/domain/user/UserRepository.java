package com.kb.domain.user;

import java.util.Optional;

/**
 * 用户仓储接口
 *
 * @author forever-king
 */
public interface UserRepository {

    /**
     * 根据用户名查询用户
     */
    Optional<KbUser> findByUsername(String username);

    /**
     * 根据ID查询用户
     */
    Optional<KbUser> findById(Long id);

    /**
     * 根据邮箱查询用户
     */
    Optional<KbUser> findByEmail(String email);

    /**
     * 保存用户
     */
    KbUser save(KbUser user);

    /**
     * 获取所有用户
     */
    java.util.List<KbUser> findAll();

    /**
     * 用户总数
     */
    long count();
}
