package com.laoliu.cas.appointment.infrastructure.task;

import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 到点自动完结任务的缓存失效测试（4.8）：
 * 回补名额后必须清 services 缓存，空跑不得打扰缓存。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("到点自动完结任务")
class BookingAutoCompleteTaskTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private CacheManager cacheManager;
    @Mock private Cache servicesCache;

    private BookingAutoCompleteTask task;

    @BeforeEach
    void setUp() {
        task = new BookingAutoCompleteTask(bookingRepository, cacheManager);
    }

    @Test
    @DisplayName("有预约被完结 → 清空 services 缓存，让回补的名额立即可见")
    void shouldEvictServicesCacheWhenBookingsCompleted() {
        when(bookingRepository.autoCompleteExpired()).thenReturn(3);
        when(cacheManager.getCache("services")).thenReturn(servicesCache);

        task.autoCompleteExpiredBookings();

        verify(cacheManager).getCache("services");
        verify(servicesCache).clear();
    }

    @Test
    @DisplayName("无预约完结（空跑）→ 不触碰缓存，避免每分钟无谓清 Redis")
    void shouldNotEvictCacheWhenNothingCompleted() {
        when(bookingRepository.autoCompleteExpired()).thenReturn(0);

        task.autoCompleteExpiredBookings();

        verify(cacheManager, never()).getCache(anyString());
        verify(servicesCache, never()).clear();
    }
}
