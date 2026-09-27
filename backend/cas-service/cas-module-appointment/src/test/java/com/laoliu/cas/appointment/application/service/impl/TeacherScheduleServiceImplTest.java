package com.laoliu.cas.appointment.application.service.impl;

import com.laoliu.cas.appointment.domain.entity.Consultant;
import com.laoliu.cas.appointment.domain.entity.ScheduleCancelRequest;
import com.laoliu.cas.appointment.domain.entity.TimeSlot;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.domain.repository.ConsultantRepository;
import com.laoliu.cas.appointment.domain.repository.ScheduleCancelRepository;
import com.laoliu.cas.appointment.domain.repository.TimeSlotRepository;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TeacherScheduleServiceImplTest {

    @Mock private ConsultantRepository consultantRepository;
    @Mock private TimeSlotRepository timeSlotRepository;
    @Mock private ScheduleCancelRepository scheduleCancelRepository;
    @Mock private BookingRepository bookingRepository;

    private TeacherScheduleServiceImpl service;
    private final Consultant consultant = Consultant.builder().id(21L).userId(7L).name("教师").build();

    @BeforeEach
    void setUp() {
        service = new TeacherScheduleServiceImpl(consultantRepository, timeSlotRepository,
                scheduleCancelRepository, bookingRepository);
        lenient().when(consultantRepository.findTeacherConsultantsByUserId(7L)).thenReturn(List.of(consultant));
    }

    @Test
    void teacherCanApplyForOwnUnbookedFutureSlot() {
        TimeSlot slot = slot(21L, 1);
        when(timeSlotRepository.findById(301L)).thenReturn(Optional.of(slot));
        when(timeSlotRepository.findAvailability(301L)).thenReturn(Optional.of(1));
        when(bookingRepository.countActiveConsultationBookingsBySlot(301L)).thenReturn(0);
        when(scheduleCancelRepository.countPendingForSlot(301L)).thenReturn(0);

        service.applyCancellation(7L, 301L, "临时外出");

        verify(scheduleCancelRepository).insert(any(ScheduleCancelRequest.class));
        verify(timeSlotRepository, never()).markCancelledIfAvailable(301L);
    }

    @Test
    void teacherCannotApplyForAnotherTeachersSlot() {
        when(timeSlotRepository.findById(301L)).thenReturn(Optional.of(slot(22L, 1)));

        assertThrows(ForbiddenException.class, () -> service.applyCancellation(7L, 301L, "临时外出"));
        verifyNoInteractions(scheduleCancelRepository);
    }

    @Test
    void occupiedSlotCannotBeSubmittedForCancellation() {
        when(timeSlotRepository.findById(301L)).thenReturn(Optional.of(slot(21L, 0)));
        assertThrows(BusinessException.class, () -> service.applyCancellation(7L, 301L, "临时外出"));
        verify(scheduleCancelRepository, never()).insert(any());
    }

    @Test
    void approvalRechecksBookingAndDoesNotMarkOccupiedSlotCancelled() {
        ScheduleCancelRequest pending = ScheduleCancelRequest.builder().id(401L).slotId(301L).status(0).build();
        when(scheduleCancelRepository.findByIdForUpdate(401L)).thenReturn(Optional.of(pending));
        when(scheduleCancelRepository.findPendingBySlotForUpdate(301L)).thenReturn(Optional.of(pending));
        when(timeSlotRepository.findByIdForUpdate(301L)).thenReturn(Optional.of(slot(21L, 0)));
        when(timeSlotRepository.findAvailabilityForUpdate(301L)).thenReturn(Optional.of(0));

        assertThrows(BusinessException.class, () -> service.approve(401L, 1L, null));
        verify(timeSlotRepository, never()).markCancelledIfAvailable(301L);
        verify(scheduleCancelRepository, never()).approve(anyLong(), anyLong(), any());
    }

    @Test
    void duplicateSubmitOnUniqueKeyReturnsBusiness400() {
        TimeSlot slot = slot(21L, 1);
        when(timeSlotRepository.findById(301L)).thenReturn(Optional.of(slot));
        when(timeSlotRepository.findAvailability(301L)).thenReturn(Optional.of(1));
        when(bookingRepository.countActiveConsultationBookingsBySlot(301L)).thenReturn(0);
        when(scheduleCancelRepository.countPendingForSlot(301L)).thenReturn(0);
        doThrow(new org.springframework.dao.DuplicateKeyException("uk_scr_pending_slot"))
                .when(scheduleCancelRepository).insert(any(ScheduleCancelRequest.class));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.applyCancellation(7L, 301L, "临时外出"));
        org.junit.jupiter.api.Assertions.assertEquals(400, ex.getCode());
    }

    @Test
    void pastDateSlotCannotBeSubmittedForCancellation() {
        TimeSlot past = TimeSlot.builder().id(302L).consultantId(21L)
                .slotDate(LocalDate.now()).startTime("09:00").endTime("10:00")
                .available(true).rawAvailability(1).build();
        when(timeSlotRepository.findById(302L)).thenReturn(Optional.of(past));
        assertThrows(BusinessException.class, () -> service.applyCancellation(7L, 302L, "临时外出"));
        verify(scheduleCancelRepository, never()).insert(any());
    }

    private TimeSlot slot(Long consultantId, int available) {
        return TimeSlot.builder().id(301L).consultantId(consultantId)
                .slotDate(LocalDate.now().plusDays(1)).startTime("09:00").endTime("10:00")
                .available(available == 1).rawAvailability(available).build();
    }
}
