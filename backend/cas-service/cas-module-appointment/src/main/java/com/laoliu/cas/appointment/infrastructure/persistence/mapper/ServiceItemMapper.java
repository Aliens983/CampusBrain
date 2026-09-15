package com.laoliu.cas.appointment.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.ServiceItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 服务表 Mapper - 操作 services 表
 *
 * @author forever-king
 */
@Mapper
public interface ServiceItemMapper extends BaseMapper<ServiceItemDO> {

    int insertSelective(ServiceItemDO record);

    int updateByPrimaryKeySelective(ServiceItemDO record);

    int updateByPrimaryKey(ServiceItemDO record);

    List<ServiceItemDO> selectUserServices(@Param("userId") Long userId);

    /**
     * 分页查询用户预约的服务
     */
    IPage<ServiceItemDO> selectUserServicesWithPage(@Param("userId") Long userId, Page<ServiceItemDO> page);
}
