package com.laoliu.cas.appointment.interfaces.convert;

import com.laoliu.cas.appointment.domain.entity.Carousel;
import com.laoliu.cas.appointment.application.dto.response.CarouselResponse;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Carousel 实体 ↔ CarouselResponse 转换器（MapStruct）
 *
 * @author forever-king
 */
@Mapper
public interface CarouselConvert {

    CarouselConvert INSTANCE = Mappers.getMapper(CarouselConvert.class);

    CarouselResponse convert(Carousel carousel);

    List<CarouselResponse> convertList(List<Carousel> list);
}
