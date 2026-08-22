package com.kb.infrastructure.persistence.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kb.infrastructure.persistence.mysql.dataobject.OAuth2AccessTokenDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Access Token MyBatis Mapper
 *
 * @author forever-king
 */
@Mapper
public interface OAuth2AccessTokenMapper extends BaseMapper<OAuth2AccessTokenDO> {

    /**
     * 根据令牌值查询
     */
    @Select("SELECT * FROM oauth2_access_token WHERE access_token = #{accessToken}")
    OAuth2AccessTokenDO selectByAccessToken(@Param("accessToken") String accessToken);

    /**
     * 根据刷新令牌查询关联的所有访问令牌
     */
    @Select("SELECT * FROM oauth2_access_token WHERE refresh_token = #{refreshToken}")
    List<OAuth2AccessTokenDO> selectByRefreshToken(@Param("refreshToken") String refreshToken);

    /**
     * 删除与刷新令牌关联的所有访问令牌
     */
    @Delete("DELETE FROM oauth2_access_token WHERE refresh_token = #{refreshToken}")
    int deleteByRefreshToken(@Param("refreshToken") String refreshToken);

    /**
     * 删除过期的访问令牌
     */
    @Delete("DELETE FROM oauth2_access_token WHERE expires_time < NOW()")
    int deleteExpired();
}
