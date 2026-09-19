package com.laoliu.cas.appointment.domain.repository;

import com.laoliu.cas.appointment.domain.entity.Carousel;

import java.util.List;

/**
 * 轮播图仓储
 *
 * @author forever-king
 */
public interface CarouselRepository {

    /** 全部轮播图（按 sort、id 升序） */
    List<Carousel> findAllOrdered();

    /** 全部启用轮播图的图片 URL（按 sort、id 升序） */
    List<String> findEnabledImageUrls();

    /** 新增轮播图，回填自增 id */
    void save(Carousel carousel);

    /** 按 id 查询（2.11：删除前需取回 imageUrl 清理物理文件） */
    Carousel findById(Long id);

    /** 按 id 删除 */
    void deleteById(Long id);

    /** 更新指定轮播图的排序值 */
    void updateSort(Long id, Integer sort);
}
