package com.laoliu.cas.appointment.domain.service;

import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.ErrorCode;
import com.laoliu.cas.common.exception.code.BookErrorCode;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 资源预约的时间窗准入策略（P1-06）。
 * <p>
 * 此前三类资源（咨询/教室/设备）下单只校验"日期是否早于今天"，不校验
 * "今天的时段是否已经过去"—— 于是可以对昨天、以及今天上午已经结束的时段
 * 成功下单，产生大量"过去的预约"垃圾数据：它们会永久占用时段/库存，
 * 并污染基于时间字符串比较的冲突判定与统计。
 * <p>
 * 校验规则：
 * <ol>
 *   <li>日期早于今天 → 拒绝；</li>
 *   <li>日期等于今天且开始时刻 &lt;= 当前时刻 → 拒绝（已开始/已结束的时段不可约）。</li>
 * </ol>
 *
 * @author forever-king
 */
public final class BookingWindowPolicy {

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private BookingWindowPolicy() {
    }

    /**
     * 校验教室时段是否可预约。
     */
    public static void assertRoomBookable(LocalDate date, String startTime) {
        assertFutureWindow(date, startTime, BookErrorCode.BOOK_TIME_INVALID);
    }

    /**
     * 校验设备借用时段是否可预约。
     */
    public static void assertEquipmentBorrowable(LocalDate date, String startTime) {
        assertFutureWindow(date, startTime, BookErrorCode.BORROW_TIME_INVALID);
    }

    /**
     * 校验咨询时段是否可预约。
     */
    public static void assertConsultationBookable(LocalDate date, String startTime) {
        assertFutureWindow(date, startTime, BookErrorCode.BOOK_TIME_INVALID);
    }

    private static void assertFutureWindow(LocalDate date, String startTime, ErrorCode errorCode) {
        if (date == null || startTime == null || startTime.isBlank()) {
            throw new BusinessException(errorCode);
        }
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            throw new BusinessException(errorCode, "不能预约过去的日期");
        }
        if (!date.equals(today)) {
            return; // 未来日期一律允许
        }
        LocalTime start;
        try {
            start = LocalTime.parse(startTime, HM);
        } catch (DateTimeParseException e) {
            throw new BusinessException(errorCode, "开始时间格式应为 HH:mm");
        }
        // 今天：开始时刻必须晚于当前时刻（否则该时段已经开始或已结束）
        if (!start.isAfter(LocalTime.now().withSecond(0).withNano(0))) {
            throw new BusinessException(errorCode, "该时段已开始或已结束，请选择更晚的时段");
        }
    }
}
