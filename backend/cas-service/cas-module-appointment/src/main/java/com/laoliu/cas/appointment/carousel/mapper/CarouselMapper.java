package com.laoliu.cas.appointment.carousel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laoliu.cas.appointment.carousel.dataobject.CarouselDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 轮播图 Mapper
 *
 * @author forever-king
 */
@Mapper
public interface CarouselMapper extends BaseMapper<CarouselDO> {

    @Select("SELECT * FROM carousel WHERE enabled = 1 ORDER BY sort ASC, id ASC")
    List<CarouselDO> selectEnabled();
}
