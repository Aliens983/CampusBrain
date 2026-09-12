package com.kb.infrastructure.rag.rewrite;

import com.kb.domain.chat.BookingSlots;
import lombok.extern.slf4j.Slf4j;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 预约槽位抽取器（规则驱动）
 * <p>
 * 从中文提问中抽取校区、业务分类、日期、时段等条件。
 * 之所以用规则而不是完全交给 LLM：这些条件有强约定（校区只有两个、时段是 HH:mm），
 * 规则抽取<b>确定性强、零额外 LLM 开销</b>，且在未配置 LLM Key 时依然可用。
 * LLM 负责它更擅长的部分——自然语言的指代消解与意图判断。
 *
 * @author forever-king
 */
@Slf4j
public final class BookingSlotExtractor {

    /** 时段区间：9-10点 / 9:00-10:00 / 09:00~10:00 / 9点到10点 */
    private static final Pattern TIME_RANGE = Pattern.compile(
            "(\\d{1,2})\\s*[:：点时]?\\s*(\\d{2})?\\s*[-~—到至]\\s*(\\d{1,2})\\s*[:：点时]?\\s*(\\d{2})?\\s*点?");

    /** 单个时点：9点 / 09:30 / 9时30 */
    private static final Pattern TIME_POINT = Pattern.compile(
            "(\\d{1,2})\\s*[:：点时]\\s*(\\d{2})?");

    private static final Pattern DATE_FULL = Pattern.compile(
            "(\\d{4})\\s*[-/年]\\s*(\\d{1,2})\\s*[-/月]\\s*(\\d{1,2})");

    private static final Pattern DATE_SHORT = Pattern.compile(
            "(?<!\\d)(\\d{1,2})\\s*[月/]\\s*(\\d{1,2})\\s*[日号]?");

    private static final Pattern WEEKDAY = Pattern.compile(
            "(下{0,1})\\s*(?:周|星期|礼拜)\\s*([一二三四五六日天1-7])");

    private BookingSlotExtractor() {
    }

    /**
     * 从用户提问中抽取预约条件
     */
    public static BookingSlots extract(String text) {
        BookingSlots slots = new BookingSlots();
        if (text == null || text.isBlank()) {
            return slots;
        }
        String q = text.toLowerCase(Locale.ROOT);

        slots.setCampus(extractCampus(q));
        slots.setCategory(extractCategory(q));

        // 先抽日期并把命中片段从文本中剔除：
        // 否则"2026-09-12"里的数字会被时段正则误当成"20:26-09"这样的伪时段
        String[] dateAndRest = extractDateAndRest(q);
        slots.setDate(dateAndRest[0]);
        String rest = dateAndRest[1];

        String[] window = extractTimeWindow(rest);
        slots.setStartTime(window[0]);
        slots.setEndTime(window[1]);

        return slots;
    }

    // ==================== 校区 ====================

    private static String extractCampus(String q) {
        if (q.contains("下沙") || q.contains("xs校区")) {
            return "xs";
        }
        if (q.contains("仓前") || q.contains("cq校区")) {
            return "cq";
        }
        return null;
    }

    // ==================== 业务分类 ====================

    private static String extractCategory(String q) {
        // 注意顺序：先判更具体的词，避免"咨询师"被"咨询"抢先
        if (containsAny(q, "教师咨询", "咨询师", "老师", "教师", "辅导", "心理咨询")) {
            return "teacher";
        }
        if (containsAny(q, "设备", "借用", "器材", "投影", "借用")) {
            return "equipment";
        }
        if (containsAny(q, "教室", "自习室", "会议室", "场地", "空间")) {
            return "space";
        }
        if (containsAny(q, "活动", "报名", "讲座", "比赛")) {
            return "activity";
        }
        // 兜底：单独的"咨询"也算教师咨询
        if (q.contains("咨询")) {
            return "teacher";
        }
        return null;
    }

    // ==================== 日期 ====================

    /**
     * 抽取日期，并把命中的日期片段从文本中剔除
     *
     * @return [日期 yyyy-MM-dd（可为 null）, 剔除日期后的剩余文本]
     */
    private static String[] extractDateAndRest(String q) {
        LocalDate today = LocalDate.now();
        String rest = q;

        // 相对日期（顺序敏感："大后天"必须先于"后天"匹配）
        String[][] relatives = {
                {"大后天", today.plusDays(3).toString()},
                {"后天", today.plusDays(2).toString()},
                {"明天", today.plusDays(1).toString()},
                {"明日", today.plusDays(1).toString()},
                {"今天", today.toString()},
                {"今日", today.toString()},
                {"当天", today.toString()},
        };
        for (String[] kv : relatives) {
            if (rest.contains(kv[0])) {
                return new String[]{kv[1], rest.replace(kv[0], " ")};
            }
        }

        Matcher weekday = WEEKDAY.matcher(rest);
        if (weekday.find()) {
            boolean nextWeek = "下".equals(weekday.group(1));
            DayOfWeek target = toDayOfWeek(weekday.group(2));
            if (target != null) {
                LocalDate candidate = today.with(DayOfWeek.MONDAY).plusDays(target.getValue() - 1);
                if (nextWeek || candidate.isBefore(today)) {
                    candidate = candidate.plusWeeks(1);
                }
                return new String[]{candidate.toString(), rest.replace(weekday.group(), " ")};
            }
        }

        Matcher full = DATE_FULL.matcher(rest);
        if (full.find()) {
            String date = String.format("%s-%02d-%02d",
                    full.group(1), Integer.parseInt(full.group(2)), Integer.parseInt(full.group(3)));
            return new String[]{date, rest.replace(full.group(), " ")};
        }

        Matcher shortDate = DATE_SHORT.matcher(rest);
        if (shortDate.find()) {
            int month = Integer.parseInt(shortDate.group(1));
            int day = Integer.parseInt(shortDate.group(2));
            if (month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                LocalDate candidate = LocalDate.of(today.getYear(), month, day);
                if (candidate.isBefore(today)) {
                    candidate = candidate.plusYears(1);
                }
                return new String[]{candidate.toString(), rest.replace(shortDate.group(), " ")};
            }
        }
        return new String[]{null, rest};
    }

    // ==================== 时段 ====================

    /**
     * 返回 [startTime, endTime]，无法识别时元素为 null
     */
    private static String[] extractTimeWindow(String q) {
        Matcher range = TIME_RANGE.matcher(q);
        if (range.find()) {
            String start = normalizeTime(range.group(1), range.group(2), false);
            String end = normalizeTime(range.group(3), range.group(4), false);
            // "9-10点" 这类省略分钟 + 无上午/下午修饰时，按上午/下午推断
            start = applyPeriod(start, q);
            end = applyPeriod(end, q);
            return new String[]{start, end};
        }

        Matcher point = TIME_POINT.matcher(q);
        if (point.find()) {
            String start = normalizeTime(point.group(1), point.group(2), false);
            start = applyPeriod(start, q);
            return new String[]{start, plusOneHour(start)};
        }

        // 只有时段词（上午/下午/晚上）
        String[] period = periodWindow(q);
        if (period != null) {
            return period;
        }
        return new String[]{null, null};
    }

    /** 把 12 小时制的小时数按"上午/下午/晚上"修正为 24 小时制 */
    private static String applyPeriod(String time, String q) {
        if (time == null) {
            return null;
        }
        int hour = Integer.parseInt(time.substring(0, 2));
        if (hour >= 12) {
            return time;
        }
        if (q.contains("下午") || q.contains("中午")) {
            return String.format("%02d:%s", hour + 12, time.substring(3));
        }
        if (q.contains("晚上") || q.contains("傍晚")) {
            return String.format("%02d:%s", hour + 12, time.substring(3));
        }
        return time;
    }

    private static String[] periodWindow(String q) {
        if (q.contains("上午") || q.contains("早上") || q.contains("早晨")) {
            return new String[]{"08:00", "12:00"};
        }
        if (q.contains("中午")) {
            return new String[]{"11:00", "13:00"};
        }
        if (q.contains("下午")) {
            return new String[]{"12:00", "18:00"};
        }
        if (q.contains("晚上") || q.contains("傍晚")) {
            return new String[]{"18:00", "22:00"};
        }
        return null;
    }

    private static String normalizeTime(String hour, String minute, boolean pad) {
        int h = Integer.parseInt(hour);
        if (h < 0 || h > 23) {
            return null;
        }
        int m = (minute == null || minute.isBlank()) ? 0 : Integer.parseInt(minute);
        return String.format("%02d:%02d", h, m);
    }

    private static String plusOneHour(String time) {
        if (time == null) {
            return null;
        }
        int h = Integer.parseInt(time.substring(0, 2));
        int m = Integer.parseInt(time.substring(3));
        h = (h + 1) % 24;
        return String.format("%02d:%02d", h, m);
    }

    private static DayOfWeek toDayOfWeek(String token) {
        return switch (token) {
            case "一", "1" -> DayOfWeek.MONDAY;
            case "二", "2" -> DayOfWeek.TUESDAY;
            case "三", "3" -> DayOfWeek.WEDNESDAY;
            case "四", "4" -> DayOfWeek.THURSDAY;
            case "五", "5" -> DayOfWeek.FRIDAY;
            case "六", "6" -> DayOfWeek.SATURDAY;
            case "日", "天", "7" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }

    private static boolean containsAny(String q, String... keywords) {
        for (String k : keywords) {
            if (q.contains(k)) {
                return true;
            }
        }
        return false;
    }
}
