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

    /**
     * 管理端更新服务：只写可编辑列（不含 booked_count，见 XML 注释）.
     * booked_count 由 BookingRepository 的条件更新原子维护，禁止全量回写.
     */
    int updateByPrimaryKeySelective(ServiceItemDO record);

    List<ServiceItemDO> selectUserServices(@Param("userId") Long userId);

    /**
     * 分页查询用户预约的服务
     */
    IPage<ServiceItemDO> selectUserServicesWithPage(@Param("userId") Long userId, Page<ServiceItemDO> page);
}
