package com.laoliu.cas.appointment.application.service.impl;

import com.laoliu.cas.appointment.application.service.CarouselImage;
import com.laoliu.cas.appointment.application.service.CarouselService;
import com.laoliu.cas.appointment.domain.entity.Carousel;
import com.laoliu.cas.appointment.domain.repository.CarouselRepository;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.ContentErrorCode;
import com.laoliu.cas.infra.api.file.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 轮播图应用服务实现
 *
 * @author forever-king
 */
@Slf4j
@Service
public class CarouselServiceImpl implements CarouselService {

    /** 轮播图存放的上传子目录 */
    private static final String SUBDIR = "carousel";

    private final CarouselRepository carouselRepository;
    private final FileService fileService;

    /**
     * 轮播图数量上限（1.5：不再硬编码，与 V1 预置数据解耦，
     * 默认 6 张保持现网行为，可由 carousel.max-count 调整）
     */
    private final int maxCount;

    public CarouselServiceImpl(CarouselRepository carouselRepository,
                               FileService fileService,
                               @Value("${carousel.max-count:6}") int maxCount) {
        this.carouselRepository = carouselRepository;
        this.fileService = fileService;
        this.maxCount = maxCount;
    }

    @Override
    public List<Carousel> listAll() {
        return carouselRepository.findAllOrdered();
    }

    @Override
    public List<String> listImages() {
        return carouselRepository.findEnabledImageUrls();
    }

    @Override
    public Carousel add(CarouselImage image) {
        List<Carousel> existing = carouselRepository.findAllOrdered();
        if (existing.size() >= maxCount) {
            throw new BusinessException(ContentErrorCode.CAROUSEL_LIMIT_EXCEEDED);
        }
        String url = fileService.uploadFile(image.content(), image.originalFilename(), SUBDIR);
        int nextSort = existing.stream()
                .map(Carousel::getSort)
                .filter(s -> s != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;
        Carousel carousel = Carousel.builder()
                .imageUrl(url)
                .sort(nextSort)
                .enabled(1)
                .build();
        carouselRepository.save(carousel);
        return carousel;
    }

    @Override
    public void delete(Long id) {
        Carousel carousel = carouselRepository.findById(id);
        if (carousel == null) {
            // 幂等：重复删除不报错
            return;
        }
        // 2.11：先删库再清物理文件——行未删掉就不应产生孤儿清理；
        // 文件删除 fail-open（FileService 内部仅告警），极端情况下残留文件可由运维清理
        carouselRepository.deleteById(id);
        fileService.deleteByUrl(carousel.getImageUrl());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reorder(List<Long> orderedIds) {
        if (orderedIds == null) {
            return;
        }
        for (int i = 0; i < orderedIds.size(); i++) {
            carouselRepository.updateSort(orderedIds.get(i), i + 1);
        }
    }
}
