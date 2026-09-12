package com.kb.infrastructure.rag.rewrite;

import com.kb.domain.chat.BookingSlots;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 预约槽位抽取器测试
 * <p>
 * 重点验证：校区、业务分类、日期、时段四类条件的识别，
 * 以及"日期片段不会干扰时段识别"这一边界。
 *
 * @author forever-king
 */
@DisplayName("预约槽位抽取器")
class BookingSlotExtractorTest {

    @Test
    @DisplayName("识别仓前校区")
    void shouldExtractCqCampus() {
        assertEquals("cq", BookingSlotExtractor.extract("仓前校区有哪些可预约服务").getCampus());
    }

    @Test
    @DisplayName("识别下沙校区")
    void shouldExtractXsCampus() {
        assertEquals("xs", BookingSlotExtractor.extract("换成下沙校区呢").getCampus());
    }

    @Test
    @DisplayName("识别业务分类")
    void shouldExtractCategory() {
        assertEquals("teacher", BookingSlotExtractor.extract("想预约教师咨询").getCategory());
        assertEquals("teacher", BookingSlotExtractor.extract("心理咨询还有名额吗").getCategory());
        assertEquals("equipment", BookingSlotExtractor.extract("怎么借用设备").getCategory());
        assertEquals("space", BookingSlotExtractor.extract("自习室还有空的吗").getCategory());
        assertEquals("activity", BookingSlotExtractor.extract("这个讲座怎么报名").getCategory());
    }

    @Test
    @DisplayName("识别上午时段：9-10点应解析为 09:00-10:00 而不是夜间")
    void shouldExtractMorningWindow() {
        BookingSlots slots = BookingSlotExtractor.extract("仓前校区上午9-10点是否有容量预约教师");
        assertEquals("cq", slots.getCampus());
        assertEquals("teacher", slots.getCategory());
        assertEquals("09:00", slots.getStartTime());
        assertEquals("10:00", slots.getEndTime());
    }

    @Test
    @DisplayName("下午时段应换算为 24 小时制")
    void shouldConvertAfternoonTo24Hour() {
        BookingSlots slots = BookingSlotExtractor.extract("下沙校区下午2点到3点有教室吗");
        assertEquals("14:00", slots.getStartTime());
        assertEquals("15:00", slots.getEndTime());
    }

    @Test
    @DisplayName("识别相对日期")
    void shouldExtractRelativeDate() {
        LocalDate today = LocalDate.now();
        assertEquals(today.toString(), BookingSlotExtractor.extract("今天有空的教室吗").getDate());
        assertEquals(today.plusDays(1).toString(), BookingSlotExtractor.extract("明天呢").getDate());
        assertEquals(today.plusDays(2).toString(), BookingSlotExtractor.extract("后天可以吗").getDate());
    }

    @Test
    @DisplayName("完整日期不应被误判为时段")
    void shouldNotMisreadFullDateAsTimeWindow() {
        BookingSlots slots = BookingSlotExtractor.extract("2026-09-12 有可预约的教室吗");
        assertEquals("2026-09-12", slots.getDate());
        assertNull(slots.getStartTime(), "纯日期文本不应解析出时段");
        assertNull(slots.getEndTime());
    }

    @Test
    @DisplayName("日期与时段同时出现时两者都要识别")
    void shouldExtractBothDateAndWindow() {
        BookingSlots slots = BookingSlotExtractor.extract("2026-09-12 09:00-10:00 想借设备");
        assertEquals("2026-09-12", slots.getDate());
        assertEquals("09:00", slots.getStartTime());
        assertEquals("10:00", slots.getEndTime());
        assertEquals("equipment", slots.getCategory());
    }

    @Test
    @DisplayName("无预约条件时返回空槽位")
    void shouldReturnEmptyWhenNothingMatched() {
        BookingSlots slots = BookingSlotExtractor.extract("介绍一下知识库的用法");
        assertEquals(new BookingSlots(), slots);
    }
}
