package com.laoliu.cas.appointment.infrastructure.task;

import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.domain.view.BookingRef;
import com.laoliu.cas.appointment.infrastructure.mq.BookingEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 到点自动归还：每分钟把"已通过且已过结束时间"的咨询/设备预约置为已完成，
 * 并把超时未审核的待审单自动拒绝释放资源.
 * 库存是叠加算出来的，记录离开活跃集即释放该窗口.
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
    private final BookingEventPublisher bookingEventPublisher;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void autoCompleteExpiredBookings() {
        try {
            List<BookingRef> completed = bookingRepository.autoCompleteExpired();
            // 超时未审核的待审单同样要清理，否则它们占着的时段/名额永远不会被释放
            List<BookingRef> rejected = bookingRepository.autoRejectStalePending();
            if (completed.isEmpty() && rejected.isEmpty()) {
                return;
            }
            log.info("到点自动归还/结束 {} 条预约，超时自动拒绝并释放 {} 条待审单",
                    completed.size(), rejected.size());
            // 完结/拒绝同样改变可约状态与"我的预约"，必须发事件让 KB 失效问答/语义缓存，
            // 否则最长 30 分钟内继续返回过期状态与余量答案（与审核/取消/强操作链路对齐）。
            // 发布器无事务上下文即直达发送，Broker 故障只记日志不拖垮任务
            completed.forEach(ref -> bookingEventPublisher.publishChanged(
                    ref.userId(), ref.serviceId(), "COMPLETED"));
            rejected.forEach(ref -> bookingEventPublisher.publishChanged(
                    ref.userId(), ref.serviceId(), "REJECTED"));
            // 容量型预约完结/拒绝会回补 booked_count，但 services 缓存 TTL 最长 30 分钟，
            // 不主动失效的话，用户在最长半小时内看到的仍是"名额已满"的旧余量.
            // 仅在确有行变更时清缓存，空跑不打扰 Redis.
            Cache cache = cacheManager.getCache(SERVICES_CACHE);
            if (cache != null) {
                cache.clear();
            }
        } catch (Exception e) {
            // 带堆栈：自动完结长期静默失败时（如表结构变更致 SQL 报错），
            // 一行无堆栈 warn 无法定位，僵尸单将持续累积无人知晓
            log.warn("自动完结过期预约失败：{}", e.getMessage(), e);
        }
    }
}
