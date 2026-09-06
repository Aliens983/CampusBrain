package com.laoliu.cas.appointment.infrastructure.task;

import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final BookingRepository bookingRepository;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void autoCompleteExpiredBookings() {
        try {
            int n = bookingRepository.autoCompleteExpired();
            if (n > 0) {
                log.info("到点自动归还/结束 {} 条预约", n);
            }
        } catch (Exception e) {
            log.warn("自动完结过期预约失败：{}", e.getMessage());
        }
    }
}
