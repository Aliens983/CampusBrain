package com.laoliu.cas.appointment.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.ServicesDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 服务表 Mapper - 操作 services 表
 *
 * @author forever-king
 */
@Mapper
public interface ServiceMapper extends BaseMapper<ServicesDO> {

    ServicesDO selectByPrimaryKey(Long id);

    List<ServicesDO> selectAll();

    /**
     * 分页查询所有服务。
     */
    IPage<ServicesDO> selectAllWithPage(Page<ServicesDO> page);

    int insertSelective(ServicesDO record);

    int updateByPrimaryKeySelective(ServicesDO record);

    int updateByPrimaryKey(ServicesDO record);

    List<ServicesDO> selectUserServices(@Param("userId") Long userId);

    /**
     * 分页查询用户预约的服务。
     */
    IPage<ServicesDO> selectUserServicesWithPage(@Param("userId") Long userId, Page<ServicesDO> page);

    @Select("SELECT * FROM services WHERE service_state = 1")
    List<ServicesDO> selectEnabledServices();

    @Select("SELECT * FROM services WHERE service_id = #{serviceId}")
    ServicesDO selectByServiceId(@Param("serviceId") Long serviceId);
}
