package com.laoliu.cas.appointment.application.service.impl;

import com.laoliu.cas.appointment.application.dto.request.EquipmentBookRequest;
import com.laoliu.cas.appointment.domain.entity.Equipment;
import com.laoliu.cas.appointment.domain.entity.ServiceItem;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.domain.repository.EquipmentRepository;
import com.laoliu.cas.appointment.domain.repository.ServiceItemRepository;
import com.laoliu.cas.appointment.domain.service.BookingLimits;
import com.laoliu.cas.appointment.infrastructure.mq.BookingEventPublisher;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.BookErrorCode;
import com.laoliu.cas.common.time.BusinessTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 设备借用数量边界：上限校验、long 相加防溢出、库存不足拒绝.
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("设备借用数量边界测试")
class EquipmentServiceQuantityTest {

    @Mock private ServiceItemRepository serviceRepository;
    @Mock private EquipmentRepository equipmentRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private BookingEventPublisher bookingEventPublisher;

    private EquipmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EquipmentServiceImpl(serviceRepository, equipmentRepository,
                bookingRepository, bookingEventPublisher);
    }

    private EquipmentBookRequest request(int quantity) {
        EquipmentBookRequest req = new EquipmentBookRequest();
        req.setQuantity(quantity);
        req.setDate(BusinessTime.today().plusDays(1).toString());
        req.setStartTime("09:00");
        req.setEndTime("10:00");
        return req;
    }

    private void stubAvailableEquipment(int stock, int occupied) {
        when(equipmentRepository.findByIdForUpdate(7L))
                .thenReturn(Optional.of(Equipment.builder()
                        .id(7L).name("投影仪").serviceId(1L).availableStock(stock).build()));
        when(serviceRepository.findById(1L))
                .thenReturn(Optional.of(ServiceItem.builder().serviceId(1L).serviceState(1).build()));
        when(bookingRepository.sumEquipmentOverlap(eq(7L), any(), eq("09:00"), eq("10:00")))
                .thenReturn(occupied);
    }

    @Test
    @DisplayName("超过上限直接拒绝，不查询库存、不落单")
    void shouldRejectQuantityAboveLimit() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.bookEquipment(1L, 7L, request(BookingLimits.MAX_EQUIPMENT_QUANTITY + 1)));

        assertEquals(BookErrorCode.BORROW_QUANTITY_INVALID.getCode(), ex.getCode());
        verify(bookingRepository, never()).sumEquipmentOverlap(anyLong(), any(), any(), any());
        verify(bookingRepository, never()).insertEquipmentBooking(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("占用 + 本次数量超过库存时拒绝，不落单")
    void shouldRejectWhenStockNotEnough() {
        stubAvailableEquipment(20, 15);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.bookEquipment(1L, 7L, request(BookingLimits.MAX_EQUIPMENT_QUANTITY)));

        assertEquals(BookErrorCode.EQUIPMENT_STOCK_NOT_ENOUGH.getCode(), ex.getCode());
        verify(bookingRepository, never()).insertEquipmentBooking(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("库存充足时正常落单")
    void shouldBookWhenStockEnough() {
        stubAvailableEquipment(20, 5);
        when(bookingRepository.insertEquipmentBooking(eq(1L), eq(1L), eq(7L), eq(3),
                any(), eq("09:00"), eq("10:00"))).thenReturn(100L);

        Long orderId = service.bookEquipment(1L, 7L, request(3));

        assertEquals(100L, orderId);
        verify(bookingEventPublisher).publishChanged(1L, 1L, "BOOKED");
    }
}
