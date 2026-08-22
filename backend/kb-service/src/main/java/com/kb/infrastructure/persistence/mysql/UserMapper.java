package com.kb.infrastructure.persistence.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kb.infrastructure.persistence.mysql.dataobject.UserDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 MyBatis Mapper
 *
 * @author forever-king
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {

    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM kb_user WHERE username = #{username} AND enabled = 1")
    UserDO selectByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询用户
     */
    @Select("SELECT * FROM kb_user WHERE email = #{email} AND enabled = 1")
    UserDO selectByEmail(@Param("email") String email);
}
