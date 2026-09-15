package com.laoliu.cas.appointment.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.ConsultantDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 咨询师 Mapper
 *
 * @author forever-king
 */
@Mapper
public interface ConsultantMapper extends BaseMapper<ConsultantDO> {

    @Select("SELECT * FROM consultant WHERE service_id = #{serviceId}")
    List<ConsultantDO> findByServiceId(@Param("serviceId") Long serviceId);
}
