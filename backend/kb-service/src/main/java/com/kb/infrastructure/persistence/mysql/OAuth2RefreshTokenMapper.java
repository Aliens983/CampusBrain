package com.kb.infrastructure.persistence.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kb.infrastructure.persistence.mysql.dataobject.OAuth2RefreshTokenDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Refresh Token MyBatis Mapper
 *
 * @author forever-king
 */
@Mapper
public interface OAuth2RefreshTokenMapper extends BaseMapper<OAuth2RefreshTokenDO> {

    /**
     * 根据令牌值查询
     */
    @Select("SELECT * FROM oauth2_refresh_token WHERE refresh_token = #{refreshToken}")
    OAuth2RefreshTokenDO selectByRefreshToken(@Param("refreshToken") String refreshToken);

    /**
     * 删除刷新令牌
     */
    @Delete("DELETE FROM oauth2_refresh_token WHERE refresh_token = #{refreshToken}")
    int deleteByRefreshToken(@Param("refreshToken") String refreshToken);
}
