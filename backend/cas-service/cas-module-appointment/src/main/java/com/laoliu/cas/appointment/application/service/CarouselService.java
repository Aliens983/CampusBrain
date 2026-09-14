package com.laoliu.cas.appointment.application.service;

import com.laoliu.cas.appointment.domain.entity.Carousel;
import org.springframework.web.multipart.MultipartFile;

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
     * 上传图片并新增一张轮播图（最多 6 张）
     *
     * @param file 图片文件
     * @return 新增的轮播图（含回填 id）
     */
    Carousel add(MultipartFile file);

    /** 按 id 删除轮播图 */
    void delete(Long id);

    /** 按传入的 id 顺序重排 sort */
    void reorder(List<Long> orderedIds);
}
