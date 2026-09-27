package com.laoliu.cas.appointment.infrastructure.task;

import com.laoliu.cas.appointment.domain.entity.Consultant;
import com.laoliu.cas.appointment.domain.repository.ConsultantRepository;
import com.laoliu.cas.appointment.domain.repository.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherScheduleGenerateTaskTest {

    @Mock private ConsultantRepository consultantRepository;
    @Mock private TimeSlotRepository timeSlotRepository;

    private TeacherScheduleGenerateTask task;

    @BeforeEach
    void setUp() {
        task = new TeacherScheduleGenerateTask(consultantRepository, timeSlotRepository);
    }

    @Test
    void createsOnlyFixedSlotsOnWeekdaysForTeacherConsultants() {
        when(consultantRepository.findActiveTeacherConsultants()).thenReturn(List.of(
                Consultant.builder().id(11L).build(), Consultant.builder().id(12L).build()));
        when(timeSlotRepository.insertIfMissing(anyLong(), any(LocalDate.class), anyString(), anyString()))
                .thenReturn(true);

        task.generate();

        long workingDays = java.util.stream.IntStream.range(0, 14)
                .mapToObj(offset -> LocalDate.now().plusDays(offset))
                .filter(date -> date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY)
                .count();
        verify(timeSlotRepository, times((int) workingDays * 2 * 4))
                .insertIfMissing(anyLong(), any(LocalDate.class), anyString(), anyString());
        verify(timeSlotRepository, atLeastOnce()).insertIfMissing(eq(11L), any(LocalDate.class), eq("09:00"), eq("10:00"));
        verify(timeSlotRepository, atLeastOnce()).insertIfMissing(eq(11L), any(LocalDate.class), eq("11:00"), eq("12:00"));
        verify(timeSlotRepository, atLeastOnce()).insertIfMissing(eq(11L), any(LocalDate.class), eq("14:00"), eq("15:00"));
        verify(timeSlotRepository, atLeastOnce()).insertIfMissing(eq(11L), any(LocalDate.class), eq("16:00"), eq("17:00"));
    }

    @Test
    void skipsGenerationWhenNoTeacherConsultantsAreBound() {
        when(consultantRepository.findActiveTeacherConsultants()).thenReturn(List.of());

        task.generate();

        verifyNoInteractions(timeSlotRepository);
    }
}
