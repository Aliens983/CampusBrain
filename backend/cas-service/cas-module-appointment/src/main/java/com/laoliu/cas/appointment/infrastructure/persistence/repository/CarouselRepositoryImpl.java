package com.laoliu.cas.appointment.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.laoliu.cas.appointment.domain.entity.Carousel;
import com.laoliu.cas.appointment.domain.repository.CarouselRepository;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.CarouselDO;
import com.laoliu.cas.appointment.infrastructure.persistence.mapper.CarouselMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 轮播图仓储实现
 *
 * @author forever-king
 */
@Repository
@RequiredArgsConstructor
public class CarouselRepositoryImpl implements CarouselRepository {

    private final CarouselMapper carouselMapper;

    @Override
    public List<Carousel> findAllOrdered() {
        LambdaQueryWrapper<CarouselDO> wrapper = new LambdaQueryWrapper<CarouselDO>()
                .orderByAsc(CarouselDO::getSort)
                .orderByAsc(CarouselDO::getId);
        return carouselMapper.selectList(wrapper).stream()
                .map(CarouselDO::toEntity)
                .toList();
    }

    @Override
    public List<String> findEnabledImageUrls() {
        return carouselMapper.selectEnabled().stream()
                .map(CarouselDO::getImageUrl)
                .toList();
    }

    @Override
    public void save(Carousel carousel) {
        CarouselDO row = CarouselDO.fromEntity(carousel);
        carouselMapper.insert(row);
        // 回填自增主键，供应用层返回
        carousel.setId(row.getId());
    }

    @Override
    public void deleteById(Long id) {
        carouselMapper.deleteById(id);
    }

    @Override
    public void updateSort(Long id, Integer sort) {
        CarouselDO update = new CarouselDO();
        update.setId(id);
        update.setSort(sort);
        carouselMapper.updateById(update);
    }
}
