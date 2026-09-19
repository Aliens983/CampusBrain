package com.laoliu.cas.appointment.application.service.impl;

import com.laoliu.cas.appointment.application.service.CarouselService;
import com.laoliu.cas.appointment.domain.entity.Carousel;
import com.laoliu.cas.appointment.domain.repository.CarouselRepository;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.ErrorCode;
import com.laoliu.cas.infra.api.file.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 轮播图应用服务实现
 *
 * @author forever-king
 */
@Service
@RequiredArgsConstructor
public class CarouselServiceImpl implements CarouselService {

    /** 轮播图数量上限 */
    private static final int MAX_COUNT = 6;

    private final CarouselRepository carouselRepository;
    private final FileService fileService;

    @Override
    public List<Carousel> listAll() {
        return carouselRepository.findAllOrdered();
    }

    @Override
    public List<String> listImages() {
        return carouselRepository.findEnabledImageUrls();
    }

    @Override
    public Carousel add(MultipartFile file) {
        List<Carousel> existing = carouselRepository.findAllOrdered();
        if (existing.size() >= MAX_COUNT) {
            throw new BusinessException(new ErrorCode(40030, "轮播图最多 " + MAX_COUNT + " 张，请先删除部分"));
        }
        String url = fileService.uploadFile(file, "carousel");
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
        carouselRepository.deleteById(id);
    }

    @Override
    public void reorder(List<Long> orderedIds) {
        if (orderedIds == null) {
            return;
        }
        for (int i = 0; i < orderedIds.size(); i++) {
            carouselRepository.updateSort(orderedIds.get(i), i + 1);
        }
    }
}
