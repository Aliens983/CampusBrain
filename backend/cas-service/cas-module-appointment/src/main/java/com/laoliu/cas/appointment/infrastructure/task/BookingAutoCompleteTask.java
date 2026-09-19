package com.laoliu.cas.appointment.infrastructure.task;

import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 到点自动归还：每分钟把"已通过且已过结束时间"的咨询/设备预约置为已完成。
 * 库存是叠加算出来的，记录离开活跃集即释放该窗口。
 *
 * @author forever-king
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingAutoCompleteTask {

    /** 与 {@code @Cacheable(value = "services")} 对齐：服务余量缓存名 */
    private static final String SERVICES_CACHE = "services";

    private final BookingRepository bookingRepository;
    private final CacheManager cacheManager;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void autoCompleteExpiredBookings() {
        try {
            int n = bookingRepository.autoCompleteExpired();
            if (n > 0) {
                log.info("到点自动归还/结束 {} 条预约", n);
                // 4.8：容量型预约完结会回补 booked_count，但 services 缓存 TTL 最长 30 分钟，
                // 不主动失效的话，用户在最长半小时内看到的仍是"名额已满"的旧余量。
                // 仅在确有行变更时清缓存，空跑不打扰 Redis。
                Cache cache = cacheManager.getCache(SERVICES_CACHE);
                if (cache != null) {
                    cache.clear();
                }
            }
        } catch (Exception e) {
            log.warn("自动完结过期预约失败：{}", e.getMessage());
        }
    }
}
