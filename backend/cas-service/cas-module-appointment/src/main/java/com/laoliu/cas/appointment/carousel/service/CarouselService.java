package com.laoliu.cas.appointment.carousel.service;

import com.laoliu.cas.appointment.carousel.dataobject.CarouselDO;
import com.laoliu.cas.appointment.carousel.mapper.CarouselMapper;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.ErrorCode;
import com.laoliu.cas.infra.application.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 轮播图应用服务
 *
 * @author forever-king
 */
@Service
@RequiredArgsConstructor
public class CarouselService {

    private final CarouselMapper carouselMapper;
    private final FileService fileService;

    /** 全部（管理端，含 id） */
    public List<CarouselDO> listAll() {
        return carouselMapper.selectList(null).stream()
                .sorted((a, b) -> {
                    int sa = a.getSort() == null ? 0 : a.getSort();
                    int sb = b.getSort() == null ? 0 : b.getSort();
                    return Integer.compare(sa, sb);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /** 启用的轮播（用户端展示） */
    public List<String> listImages() {
        return carouselMapper.selectEnabled().stream()
                .map(CarouselDO::getImageUrl)
                .collect(Collectors.toList());
    }

    private static final int MAX_COUNT = 6;

    /** 管理端上传并新增一张（最多 6 张） */
    public CarouselDO add(MultipartFile file) {
        if (carouselMapper.selectList(null).size() >= MAX_COUNT) {
            throw new BusinessException(new ErrorCode(40030, "轮播图最多 " + MAX_COUNT + " 张，请先删除部分"));
        }
        String url = fileService.uploadFile(file, "carousel");
        Integer max = carouselMapper.selectList(null).stream()
                .map(CarouselDO::getSort)
                .filter(s -> s != null)
                .max(Integer::compareTo).orElse(0);
        CarouselDO row = CarouselDO.builder()
                .imageUrl(url).sort(max + 1).enabled(1)
                .build();
        carouselMapper.insert(row);
        return row;
    }

    public void delete(Long id) {
        carouselMapper.deleteById(id);
    }

    /** 拖拽后按传入顺序重新排 sort */
    public void reorder(java.util.List<Long> ids) {
        if (ids == null) {
            return;
        }
        for (int i = 0; i < ids.size(); i++) {
            CarouselDO d = new CarouselDO();
            d.setId(ids.get(i));
            d.setSort(i + 1);
            carouselMapper.updateById(d);
        }
    }
}
