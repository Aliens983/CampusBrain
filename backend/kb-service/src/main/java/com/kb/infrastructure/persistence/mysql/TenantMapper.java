package com.kb.infrastructure.persistence.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kb.infrastructure.persistence.mysql.dataobject.TenantDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TenantMapper extends BaseMapper<TenantDO> {

    @Select("SELECT * FROM kb_tenant WHERE code = #{code}")
    TenantDO selectByCode(@Param("code") String code);

    @Select("SELECT * FROM kb_tenant WHERE owner_id = #{ownerId}")
    List<TenantDO> selectByOwnerId(@Param("ownerId") Long ownerId);
}
