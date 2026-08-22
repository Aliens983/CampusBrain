package com.laoliu.cas.appointment.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.EquipmentDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 设备 Mapper
 *
 * @author forever-king
 */
@Mapper
public interface EquipmentMapper extends BaseMapper<EquipmentDO> {

    @Select("SELECT * FROM equipment WHERE service_id = #{serviceId}")
    List<EquipmentDO> findByServiceId(@Param("serviceId") Long serviceId);

    /** 分页查询所有设备，支持按名称模糊搜索 */
    IPage<EquipmentDO> selectPage(Page<EquipmentDO> page,
                                  @Param("name") String name,
                                  @Param("category") String category);

    /** 查询所有不重复的设备分类 */
    @Select("SELECT DISTINCT category FROM equipment WHERE category IS NOT NULL AND category != '' ORDER BY category")
    List<String> findDistinctCategories();
}
