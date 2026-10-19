package com.laoliu.cas.appointment.infrastructure.task;

import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.domain.view.BookingRef;
import com.laoliu.cas.appointment.infrastructure.mq.BookingEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 到点自动完结任务测试：
 * 回补名额后必须清 services 缓存；完结/拒绝必须发布预约变更事件（KB 缓存失效）.
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("到点自动完结任务")
class BookingAutoCompleteTaskTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private CacheManager cacheManager;
    @Mock private Cache servicesCache;
    @Mock private BookingEventPublisher bookingEventPublisher;

    private BookingAutoCompleteTask task;

    @BeforeEach
    void setUp() {
        task = new BookingAutoCompleteTask(bookingRepository, cacheManager, bookingEventPublisher);
    }

    @Test
    @DisplayName("有预约被完结 → 清空 services 缓存，并发布 COMPLETED 事件")
    void shouldEvictCacheAndPublishEventWhenBookingsCompleted() {
        when(bookingRepository.autoCompleteExpired())
                .thenReturn(List.of(new BookingRef(1L, 7L, 2L)));
        when(bookingRepository.autoRejectStalePending()).thenReturn(List.of());
        when(cacheManager.getCache("services")).thenReturn(servicesCache);

        task.autoCompleteExpiredBookings();

        verify(cacheManager).getCache("services");
        verify(servicesCache).clear();
        verify(bookingEventPublisher).publishChanged(7L, 2L, "COMPLETED");
    }

    @Test
    @DisplayName("有待审单被自动拒绝 → 发布 REJECTED 事件（KB 不得继续返回过期答案）")
    void shouldPublishEventWhenStalePendingRejected() {
        when(bookingRepository.autoCompleteExpired()).thenReturn(List.of());
        when(bookingRepository.autoRejectStalePending())
                .thenReturn(List.of(new BookingRef(2L, 8L, 3L)));
        when(cacheManager.getCache("services")).thenReturn(servicesCache);

        task.autoCompleteExpiredBookings();

        verify(bookingEventPublisher).publishChanged(8L, 3L, "REJECTED");
        verify(servicesCache).clear();
    }

    @Test
    @DisplayName("无任何变更（空跑）→ 不触碰缓存，不发事件")
    void shouldNotEvictCacheOrPublishWhenNothingChanged() {
        when(bookingRepository.autoCompleteExpired()).thenReturn(List.of());
        when(bookingRepository.autoRejectStalePending()).thenReturn(List.of());

        task.autoCompleteExpiredBookings();

        verify(cacheManager, never()).getCache(anyString());
        verify(bookingEventPublisher, never()).publishChanged(anyLong(), anyLong(), anyString());
    }
}
