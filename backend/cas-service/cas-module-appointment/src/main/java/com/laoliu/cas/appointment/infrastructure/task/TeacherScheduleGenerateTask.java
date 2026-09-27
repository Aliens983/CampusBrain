package com.laoliu.cas.appointment.infrastructure.task;

import com.laoliu.cas.appointment.domain.entity.Consultant;
import com.laoliu.cas.appointment.domain.repository.ConsultantRepository;
import com.laoliu.cas.appointment.domain.repository.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TeacherScheduleGenerateTask {

    private static final List<SlotTime> FIXED_SLOTS = List.of(
            new SlotTime("09:00", "10:00"),
            new SlotTime("11:00", "12:00"),
            new SlotTime("14:00", "15:00"),
            new SlotTime("16:00", "17:00"));

    private final ConsultantRepository consultantRepository;
    private final TimeSlotRepository timeSlotRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void generateAtStartup() {
        generate();
    }

    @Scheduled(cron = "0 15 0 * * *", zone = "Asia/Shanghai")
    public void generateDaily() {
        generate();
    }

    public void generate() {
        List<Consultant> consultants = consultantRepository.findActiveTeacherConsultants();
        if (consultants.isEmpty()) {
            log.warn("No active teacher consultants found; fixed schedule generation skipped");
            return;
        }
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(13);
        int inserted = 0;
        for (Consultant consultant : consultants) {
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                if (!isWorkingDay(date)) continue;
                for (SlotTime slot : FIXED_SLOTS) {
                    if (timeSlotRepository.insertIfMissing(consultant.getId(), date, slot.start(), slot.end())) inserted++;
                }
            }
        }
        log.info("Fixed teacher schedule generation completed: consultants={}, inserted={}, dateRange={}..{}",
                consultants.size(), inserted, start, end);
    }

    private boolean isWorkingDay(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    private record SlotTime(String start, String end) {}
}
