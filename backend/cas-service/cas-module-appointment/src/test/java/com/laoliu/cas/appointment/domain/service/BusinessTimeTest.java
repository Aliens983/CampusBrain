package com.laoliu.cas.appointment.domain.service;

import com.laoliu.cas.common.time.BusinessTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 业务时区边界测试：UTC 16:30 已经是北京时间次日 00:30.
 *
 * @author forever-king
 */
@DisplayName("业务时区测试")
class BusinessTimeTest {

    @Test
    @DisplayName("UTC 跨天时业务日期按东八区前进一天")
    void shouldUseShanghaiDateAcrossUtcMidnight() {
        Clock utcClock = Clock.fixed(Instant.parse("2026-10-31T16:30:00Z"), ZoneOffset.UTC);

        assertEquals(LocalDate.of(2026, 11, 1), BusinessTime.today(utcClock));
        assertEquals(LocalDateTime.of(2026, 11, 1, 0, 30), BusinessTime.now(utcClock));
    }
}
