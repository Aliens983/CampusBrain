package com.laoliu.cas.appointment.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.ServiceCategoryDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 服务业务分类 Mapper - 操作 service_category 表（只读种子）
 *
 * @author forever-king
 */
@Mapper
public interface ServiceCategoryMapper extends BaseMapper<ServiceCategoryDO> {
}
