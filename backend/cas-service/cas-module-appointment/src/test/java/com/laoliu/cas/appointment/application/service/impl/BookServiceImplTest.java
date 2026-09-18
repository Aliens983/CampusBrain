package com.laoliu.cas.appointment.application.service.impl;

import com.laoliu.cas.appointment.infrastructure.metrics.BookingMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import com.laoliu.cas.appointment.application.service.BookService;
import com.laoliu.cas.appointment.domain.entity.ServiceItem;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.domain.repository.ServiceItemRepository;
import com.laoliu.cas.appointment.infrastructure.mq.BookingEventPublisher;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceStatusResponse;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.BookErrorCode;
import com.laoliu.cas.common.exception.code.ServiceErrorCode;
import com.laoliu.cas.system.api.UserInfoApi;
import com.laoliu.cas.system.api.dto.UserInfoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BookServiceImpl 单元测试
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("预约服务单元测试")
class BookServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ServiceItemRepository serviceRepository;

    @Mock
    private UserInfoApi userInfoApi;

    @Mock
    private BookingEventPublisher bookingEventPublisher;

    private BookService bookService;

    private static final Long USER_ID = 100L;
    private static final Long SERVICE_ID = 1L;
    private static final Long SERVICE_ID_2 = 2L;
    private static final Long ORDER_ID = 10L;

    @BeforeEach
    void setUp() {
        bookService = new BookServiceImpl(bookingRepository, serviceRepository, userInfoApi, bookingEventPublisher, new BookingMetrics(new SimpleMeterRegistry()));
    }

    @Nested
    @DisplayName("预约服务 - bookService")
    class BookServiceTests {

        @Test
        @DisplayName("应当成功创建预约")
        void shouldBookServiceSuccessfully() {
            // Given
            List<Long> serviceIds = List.of(SERVICE_ID);
            ServiceItem availableService = buildAvailableService(SERVICE_ID);
            UserInfoDTO userInfo = buildUserInfo();

            when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(availableService));
            when(bookingRepository.decrementStock(SERVICE_ID)).thenReturn(1);
            when(bookingRepository.insertServices(eq(USER_ID), anyList())).thenReturn(List.of(ORDER_ID));
            when(userInfoApi.getUserById(USER_ID)).thenReturn(userInfo);

            // When
            BookService.BookingSubmitResult result = bookService.bookService(USER_ID, serviceIds);

            // Then
            assertNotNull(result);
            assertNotNull(result.userInfo());
            assertEquals("测试用户", result.userInfo().getName());
            // 3.3.1：返回真实回填的订单号
            assertEquals(List.of(ORDER_ID), result.orderIds());
            verify(bookingRepository).insertServices(eq(USER_ID), anyList());
            verify(userInfoApi).getUserById(USER_ID);
        }

        @Test
        @DisplayName("服务ID列表为空时应当抛出 SERVICE_ID_EMPTY 异常")
        void shouldThrowExceptionWhenServiceIdsEmpty() {
            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> bookService.bookService(USER_ID, Collections.emptyList()));
            assertEquals(ServiceErrorCode.SERVICE_ID_EMPTY.getCode(), exception.getCode());
            verify(bookingRepository, never()).insertServices(anyLong(), anyList());
        }

        @Test
        @DisplayName("服务ID列表为 null 时应当抛出 SERVICE_ID_EMPTY 异常")
        void shouldThrowExceptionWhenServiceIdsNull() {
            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> bookService.bookService(USER_ID, null));
            assertEquals(ServiceErrorCode.SERVICE_ID_EMPTY.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("服务不存在时应当抛出 SERVICE_NOT_EXIST 异常")
        void shouldThrowExceptionWhenServiceNotExist() {
            // Given
            when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.empty());

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> bookService.bookService(USER_ID, List.of(SERVICE_ID)));
            assertEquals(ServiceErrorCode.SERVICE_NOT_EXIST.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("服务已禁用时应当抛出 SERVICE_DISABLED 异常")
        void shouldThrowExceptionWhenServiceDisabled() {
            // Given
            ServiceItem disabledService = buildDisabledService(SERVICE_ID);
            when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(disabledService));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> bookService.bookService(USER_ID, List.of(SERVICE_ID)));
            assertEquals(ServiceErrorCode.SERVICE_DISABLED.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("容量已满时应当抛出 BOOKING_CAPACITY_FULL 且不插入预约")
        void shouldThrowWhenCapacityFull() {
            // Given：decrementStock 返回 0（乐观锁扣减失败 = 容量满）
            when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(buildAvailableService(SERVICE_ID)));
            when(bookingRepository.decrementStock(SERVICE_ID)).thenReturn(0);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> bookService.bookService(USER_ID, List.of(SERVICE_ID)));
            assertEquals(BookErrorCode.BOOKING_CAPACITY_FULL.getCode(), exception.getCode());
            // 容量满不应继续插入预约
            verify(bookingRepository, never()).insertServices(anyLong(), anyList());
        }

        @Test
        @DisplayName("预约成功时应先乐观锁扣减库存")
        void shouldDecrementStockBeforeInsert() {
            // Given
            when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(buildAvailableService(SERVICE_ID)));
            when(bookingRepository.decrementStock(SERVICE_ID)).thenReturn(1);
            when(bookingRepository.insertServices(eq(USER_ID), anyList())).thenReturn(List.of(ORDER_ID));
            when(userInfoApi.getUserById(USER_ID)).thenReturn(buildUserInfo());

            // When
            bookService.bookService(USER_ID, List.of(SERVICE_ID));

            // Then：先扣减后插入
            verify(bookingRepository).decrementStock(SERVICE_ID);
            verify(bookingRepository).insertServices(eq(USER_ID), anyList());
        }

        @Test
        @DisplayName("并发预约同一服务时超出容量上限的请求被拒绝")
        void shouldRejectOverCapacityUnderConcurrentBooking() throws Exception {
            // Given：模拟 DB 原子条件 UPDATE（decrementStock）——容量充足才 +1，
            // 并发下只有前 capacity 个请求成功，其余触发行=0 抛 BOOKING_CAPACITY_FULL。
            int capacity = 3;
            int threads = 10;
            AtomicInteger bookedAttempts = new AtomicInteger();
            AtomicLong orderSeq = new AtomicLong(ORDER_ID);
            when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(buildAvailableService(SERVICE_ID)));
            when(bookingRepository.decrementStock(SERVICE_ID)).thenAnswer(inv ->
                    bookedAttempts.incrementAndGet() <= capacity ? 1 : 0);
            when(bookingRepository.insertServices(eq(USER_ID), anyList())).thenAnswer(inv -> {
                @SuppressWarnings("unchecked")
                List<Integer> sids = (List<Integer>) inv.getArgument(1);
                return sids.stream().map(s -> orderSeq.getAndIncrement()).toList();
            });
            when(userInfoApi.getUserById(USER_ID)).thenReturn(buildUserInfo());

            // When：10 个线程同时预约同一服务
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch ready = new CountDownLatch(1);
            AtomicInteger success = new AtomicInteger();
            AtomicInteger capacityFull = new AtomicInteger();
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    ready.await();
                    try {
                        bookService.bookService(USER_ID, List.of(SERVICE_ID));
                        success.incrementAndGet();
                    } catch (BusinessException e) {
                        assertEquals(BookErrorCode.BOOKING_CAPACITY_FULL.getCode(), e.getCode());
                        capacityFull.incrementAndGet();
                    }
                    return null;
                }));
            }
            ready.countDown();
            for (Future<?> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }
            pool.shutdown();

            // Then：恰好 capacity 个成功，其余全部容量满，且 10 次都发起了库存扣减尝试
            assertEquals(capacity, success.get());
            assertEquals(threads - capacity, capacityFull.get());
            assertEquals(threads, bookedAttempts.get());
        }
    }

    @Nested
    @DisplayName("取消预约 - cancelBookings")
    class CancelBookingsTests {

        @Test
        @DisplayName("应当成功取消预约")
        void shouldCancelBookingsSuccessfully() {
            // Given
            List<Long> bookingIds = List.of(ORDER_ID);
            when(bookingRepository.cancelBookings(USER_ID, bookingIds)).thenReturn(1);

            // When
            boolean result = bookService.cancelBookings(USER_ID, bookingIds);

            // Then
            assertTrue(result);
            verify(bookingRepository).cancelBookings(USER_ID, bookingIds);
        }

        @Test
        @DisplayName("取消成功时发布变更事件，库存与时段释放已由原子 SQL 完成")
        void shouldPublishChangedEventOnCancel() {
            // Given
            List<Long> bookingIds = List.of(ORDER_ID);
            when(bookingRepository.cancelBookings(USER_ID, bookingIds)).thenReturn(1);

            // When
            boolean result = bookService.cancelBookings(USER_ID, bookingIds);

            // Then：service 层不再单独回补（已在 cancelBookings 的多表 UPDATE 内原子完成，7.3.6）
            assertTrue(result);
            verify(bookingEventPublisher).publishChanged(USER_ID, ORDER_ID, "CANCELLED");
            verify(bookingRepository, never()).releaseStock(anyLong());
        }

        @Test
        @DisplayName("取消影响 0 行时不发布变更事件")
        void shouldNotPublishEventWhenCancelFailed() {
            // Given：cancelBookings 返回 0（无待审核单/活动单可取消）
            List<Long> bookingIds = List.of(ORDER_ID);
            when(bookingRepository.cancelBookings(USER_ID, bookingIds)).thenReturn(0);

            // When
            boolean result = bookService.cancelBookings(USER_ID, bookingIds);

            // Then
            assertFalse(result);
            verify(bookingEventPublisher, never()).publishChanged(anyLong(), anyLong(), anyString());
        }

        @Test
        @DisplayName("预约ID列表为空时应当返回 false")
        void shouldReturnFalseWhenBookingIdsEmpty() {
            // When
            boolean result = bookService.cancelBookings(USER_ID, Collections.emptyList());

            // Then
            assertFalse(result);
            verify(bookingRepository, never()).cancelBookings(anyLong(), anyList());
        }

        @Test
        @DisplayName("预约ID列表为 null 时应当返回 false")
        void shouldReturnFalseWhenBookingIdsNull() {
            // When
            boolean result = bookService.cancelBookings(USER_ID, null);

            // Then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("查询预约 - getAllBookings / getBookingById")
    class QueryBookingsTests {

        @Test
        @DisplayName("应当返回用户的预约列表")
        void shouldReturnUserBookings() {
            // Given
            ServiceStatusResponse status = buildStatusResponse();
            when(bookingRepository.getServiceStatusByUserId(USER_ID))
                    .thenReturn(List.of(status));

            // When
            var bookings = bookService.getAllBookings(USER_ID);

            // Then
            assertNotNull(bookings);
            assertEquals(1, bookings.size());
            assertEquals(ORDER_ID, bookings.get(0).getOrderId());
        }

        @Test
        @DisplayName("应当返回单个预约详情")
        void shouldReturnBookingDetail() {
            // Given
            ServiceStatusResponse status = buildStatusResponse();
            when(bookingRepository.getServiceStatusByOrderIdAndUserId(USER_ID, ORDER_ID)).thenReturn(status);

            // When
            var booking = bookService.getBookingById(USER_ID, ORDER_ID);

            // Then
            assertNotNull(booking);
            assertEquals(ORDER_ID, booking.getOrderId());
        }

        @Test
        @DisplayName("预约不存在时应当返回 null")
        void shouldReturnNullWhenBookingNotFound() {
            // Given
            when(bookingRepository.getServiceStatusByOrderIdAndUserId(USER_ID, ORDER_ID)).thenReturn(null);

            // When
            var booking = bookService.getBookingById(USER_ID, ORDER_ID);

            // Then
            assertNull(booking);
        }
    }

    // ======================== 辅助方法 ========================

    private ServiceItem buildAvailableService(Long id) {
        return ServiceItem.builder()
                .serviceId(id)
                .serviceName("自习室预约")
                .serviceDescribe("图书馆自习室")
                .serviceState(1)
                .build();
    }

    @Test
    @DisplayName("批量预约中某项被幂等去重时，必须回补该项已扣的库存")
    void shouldReleaseStockWhenItemDeduplicated() {
        Long otherId = 2L;
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(buildAvailableService(SERVICE_ID)));
        when(serviceRepository.findById(otherId)).thenReturn(Optional.of(buildAvailableService(otherId)));
        when(bookingRepository.decrementStock(anyLong())).thenReturn(1);
        // 1L 被 SQL 幂等去重（返回空），2L 正常建成
        when(bookingRepository.insertServices(anyLong(), anyList())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.List<Integer> ids = invocation.getArgument(1);
            return ids.contains(SERVICE_ID.intValue()) ? java.util.List.of() : java.util.List.of(500L);
        });

        BookService.BookingSubmitResult result =
                bookService.bookService(USER_ID, java.util.List.of(SERVICE_ID, otherId));

        // 被去重的 1L 必须回补，否则 booked_count 凭空 +1 且无对应订单
        verify(bookingRepository).releaseStock(SERVICE_ID);
        verify(bookingRepository, never()).releaseStock(otherId);
        assertNotNull(result);
        assertEquals(1, result.orderIds().size());
    }

    private ServiceItem buildDisabledService(Long id) {
        return ServiceItem.builder()
                .serviceId(id)
                .serviceName("已禁用服务")
                .serviceDescribe("已禁用")
                .serviceState(0)
                .build();
    }

    private UserInfoDTO buildUserInfo() {
        UserInfoDTO dto = new UserInfoDTO();
        dto.setId(USER_ID);
        dto.setName("测试用户");
        dto.setEmail("test@example.com");
        dto.setGrade("大三");
        dto.setRole(0);
        return dto;
    }

    private ServiceStatusResponse buildStatusResponse() {
        ServiceStatusResponse response = new ServiceStatusResponse();
        response.setOrderId(ORDER_ID);
        response.setUserId(USER_ID);
        response.setUsername("测试用户");
        response.setServiceName("自习室预约");
        response.setServiceDescribe("图书馆自习室");
        response.setManageStatus(0);
        response.setCreateTime(LocalDateTime.now());
        return response;
    }
}
