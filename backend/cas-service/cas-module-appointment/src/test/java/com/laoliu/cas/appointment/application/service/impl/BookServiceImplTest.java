package com.laoliu.cas.appointment.application.service.impl;

import com.laoliu.cas.appointment.application.service.BookService;
import com.laoliu.cas.appointment.domain.entity.Service;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.domain.repository.ServiceRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BookServiceImpl 单元测试。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("预约服务单元测试")
class BookServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ServiceRepository serviceRepository;

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
        bookService = new BookServiceImpl(bookingRepository, serviceRepository, userInfoApi, bookingEventPublisher);
    }

    @Nested
    @DisplayName("预约服务 - bookService")
    class BookServiceTests {

        @Test
        @DisplayName("应当成功创建预约")
        void shouldBookServiceSuccessfully() {
            // Given
            List<Long> serviceIds = List.of(SERVICE_ID);
            Service availableService = buildAvailableService(SERVICE_ID);
            UserInfoDTO userInfo = buildUserInfo();

            when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(availableService));
            when(bookingRepository.decrementStock(SERVICE_ID)).thenReturn(1);
            when(bookingRepository.insertServices(eq(USER_ID), anyList())).thenReturn(1);
            when(userInfoApi.getUserById(USER_ID)).thenReturn(userInfo);

            // When
            UserInfoDTO result = bookService.bookService(USER_ID, serviceIds);

            // Then
            assertNotNull(result);
            assertEquals("测试用户", result.getName());
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
            Service disabledService = buildDisabledService(SERVICE_ID);
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
            when(bookingRepository.insertServices(eq(USER_ID), anyList())).thenReturn(1);
            when(userInfoApi.getUserById(USER_ID)).thenReturn(buildUserInfo());

            // When
            bookService.bookService(USER_ID, List.of(SERVICE_ID));

            // Then：先扣减后插入
            verify(bookingRepository).decrementStock(SERVICE_ID);
            verify(bookingRepository).insertServices(eq(USER_ID), anyList());
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
        @DisplayName("取消成功时应释放对应服务库存")
        void shouldReleaseStockOnCancel() {
            // Given
            List<Long> bookingIds = List.of(ORDER_ID);
            when(bookingRepository.cancelBookings(USER_ID, bookingIds)).thenReturn(1);
            when(bookingRepository.selectServiceIdsByBookingIds(USER_ID, bookingIds))
                    .thenReturn(List.of(SERVICE_ID));

            // When
            boolean result = bookService.cancelBookings(USER_ID, bookingIds);

            // Then：查待审核单的 serviceId 并回退库存
            assertTrue(result);
            verify(bookingRepository).selectServiceIdsByBookingIds(USER_ID, bookingIds);
            verify(bookingRepository).releaseStock(SERVICE_ID);
        }

        @Test
        @DisplayName("取消失败时不应释放库存")
        void shouldNotReleaseStockWhenCancelFailed() {
            // Given：cancelBookings 返回 0（无待审核单可取消）
            List<Long> bookingIds = List.of(ORDER_ID);
            when(bookingRepository.cancelBookings(USER_ID, bookingIds)).thenReturn(0);

            // When
            boolean result = bookService.cancelBookings(USER_ID, bookingIds);

            // Then：不查 serviceId、不释放库存
            assertFalse(result);
            verify(bookingRepository, never()).selectServiceIdsByBookingIds(anyLong(), anyList());
            verify(bookingRepository, never()).releaseStock(anyLong());
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

    private Service buildAvailableService(Long id) {
        return Service.builder()
                .serviceId(id)
                .serviceName("自习室预约")
                .serviceDescribe("图书馆自习室")
                .serviceState(1)
                .build();
    }

    private Service buildDisabledService(Long id) {
        return Service.builder()
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
