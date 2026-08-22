package com.laoliu.cas.appointment.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    /** 分页查询所有咨询师，支持按名称/部门模糊搜索 */
    IPage<ConsultantDO> selectPage(Page<ConsultantDO> page,
                                   @Param("name") String name,
                                   @Param("department") String department);
}
