package com.laoliu.cas.appointment.application.service.impl;

import com.laoliu.cas.appointment.application.service.CarouselImage;
import com.laoliu.cas.appointment.domain.entity.Carousel;
import com.laoliu.cas.appointment.domain.repository.CarouselRepository;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.infra.api.file.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CarouselServiceImpl 单元测试（1.5 / 2.10 / 2.11）。
 * <p>
 * 覆盖：数量上限可配置化（40030）、Servlet 解耦后的字节载体上传、
 * 删除时物理文件清理顺序与幂等、reorder 序号从 1 开始。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
class CarouselServiceImplTest {

    private static final int MAX_COUNT = 6;

    @Mock
    private CarouselRepository carouselRepository;

    @Mock
    private FileService fileService;

    private CarouselServiceImpl carouselService;

    @BeforeEach
    void setUp() {
        carouselService = new CarouselServiceImpl(carouselRepository, fileService, MAX_COUNT);
    }

    @Test
    @DisplayName("1.5 未达上限：字节内容上传成功，sort 取最大值 + 1，默认启用")
    void shouldUploadAndSaveWhenBelowLimit() {
        // Given：已有两张，sort 为 1、3（验证取最大而非 size）
        when(carouselRepository.findAllOrdered()).thenReturn(List.of(
                Carousel.builder().id(1L).imageUrl("/u1.png").sort(1).enabled(1).build(),
                Carousel.builder().id(2L).imageUrl("/u2.png").sort(3).enabled(1).build()));
        when(fileService.uploadFile(any(byte[].class), eq("a.png"), eq("carousel")))
                .thenReturn("/uploads/carousel/xxx.png");

        // When
        Carousel saved = carouselService.add(new CarouselImage("a.png", new byte[]{1, 2, 3}));

        // Then
        assertEquals("/uploads/carousel/xxx.png", saved.getImageUrl());
        assertEquals(4, saved.getSort());
        assertEquals(1, saved.getEnabled());
        verify(carouselRepository, times(1)).save(any(Carousel.class));
    }

    @Test
    @DisplayName("1.5 达到上限：抛 40030 CAROUSEL_LIMIT_EXCEEDED，不上传不落库")
    void shouldRejectWhenLimitReached() {
        // Given：存量已达上限
        List<Carousel> existing = new ArrayList<>();
        for (long i = 1; i <= MAX_COUNT; i++) {
            existing.add(Carousel.builder().id(i).imageUrl("/u" + i + ".png").sort((int) i).enabled(1).build());
        }
        when(carouselRepository.findAllOrdered()).thenReturn(existing);

        // When / Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> carouselService.add(new CarouselImage("a.png", new byte[]{1})));
        assertEquals(40030, ex.getCode());
        verify(fileService, never()).uploadFile(any(byte[].class), any(), any());
        verify(carouselRepository, never()).save(any());
    }

    @Test
    @DisplayName("2.10 应用层只面对 CarouselImage：上传文件名与字节原样透传存储层")
    void shouldPassByteContentThroughToFileService() {
        when(carouselRepository.findAllOrdered()).thenReturn(List.of());
        byte[] content = "img-bytes".getBytes();
        when(fileService.uploadFile(content, "banner.jpg", "carousel"))
                .thenReturn("/uploads/carousel/banner.png");

        carouselService.add(new CarouselImage("banner.jpg", content));

        verify(fileService).uploadFile(content, "banner.jpg", "carousel");
    }

    @Test
    @DisplayName("2.11 删除存在的轮播图：先删库记录，再按记录 URL 清理物理文件")
    void shouldDeleteRecordAndThenPhysicalFile() {
        // Given
        Carousel carousel = Carousel.builder().id(7L).imageUrl("/uploads/carousel/old.png").sort(2).enabled(1).build();
        when(carouselRepository.findById(7L)).thenReturn(carousel);

        // When
        carouselService.delete(7L);

        // Then
        verify(carouselRepository).deleteById(7L);
        verify(fileService).deleteByUrl("/uploads/carousel/old.png");
    }

    @Test
    @DisplayName("2.11 删除不存在的 id：幂等返回，不删库不触碰文件")
    void shouldBeIdempotentWhenRecordMissing() {
        when(carouselRepository.findById(99L)).thenReturn(null);

        carouselService.delete(99L);

        verify(carouselRepository, never()).deleteById(any());
        verify(fileService, never()).deleteByUrl(any());
    }

    @Test
    @DisplayName("reorder：按列表顺序写回从 1 开始的连续 sort")
    void shouldReorderWithOneBasedPositions() {
        carouselService.reorder(List.of(3L, 1L, 2L));

        verify(carouselRepository).updateSort(3L, 1);
        verify(carouselRepository).updateSort(1L, 2);
        verify(carouselRepository).updateSort(2L, 3);
    }

    @Test
    @DisplayName("reorder(null)：直接返回，不产生任何写操作")
    void shouldIgnoreNullReorder() {
        carouselService.reorder(null);
        verify(carouselRepository, never()).updateSort(any(), any(Integer.class));
    }
}
