package com.laoliu.cas.appointment.application.service;

import com.laoliu.cas.appointment.domain.entity.Carousel;

import java.util.List;

/**
 * 首页轮播图应用服务
 *
 * @author forever-king
 */
public interface CarouselService {

    /** 全部轮播图（管理端，按 sort、id 升序） */
    List<Carousel> listAll();

    /** 启用的轮播图片 URL 列表（用户端展示） */
    List<String> listImages();

    /**
     * 上传图片并新增一张轮播图（数量上限见 carousel.max-count 配置，默认 6 张）。
     * <p>2.10：入参为应用层自有载体 {@link CarouselImage}，不依赖 Servlet MultipartFile，
     * 定时任务/消息消费/单测可直接复用。
     *
     * @param image 图片文件名与字节
     * @return 新增的轮播图（含回填 id）
     */
    Carousel add(CarouselImage image);

    /** 按 id 删除轮播图（同步清理物理图片文件） */
    void delete(Long id);

    /** 按传入的 id 顺序重排 sort（单事务，中途失败整体回滚防乱序） */
    void reorder(List<Long> orderedIds);
}
