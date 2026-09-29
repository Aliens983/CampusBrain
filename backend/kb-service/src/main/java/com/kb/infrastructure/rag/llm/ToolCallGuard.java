package com.kb.infrastructure.rag.llm;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 预约工具链路的"空承诺"判定（Function Calling 守卫）。
 * <p>
 * 真实故障（2026-09-26）：用户在多轮对话中催促查询教师余量时，模型连续 6 轮只输出
 * "我这就为您查询…""I'll query the teacher consultation availability…"之类的承诺
 * 就结束了本轮输出，<b>始终没有发起工具调用</b>，用户反复追问才在某一轮碰巧查到。
 * </p>
 * <p>
 * 本类用于流式工具链路结束后识别这类回答：整轮零工具执行且输出是"承诺/过程性空话"
 * （或为空），编排层据此丢弃该输出并强制带"必须调工具"的指令重试一次。
 * 带真实数据的长回答（咨询师列表、余量数字）与正常寒暄不会被误判。
 * </p>
 *
 * @author forever-king
 */
final class ToolCallGuard {

    /** 承诺类回答的长度上限：真实查询结果（列表/余量）远超此长度，承诺句通常很短 */
    private static final int MAX_PROMISE_LEN = 100;

    /** 工具已执行但回答仍停在"计划下一步"时的长度上限（真实结论通常更长） */
    private static final int MAX_NARRATIVE_LEN = 140;

    /** 工具链中途的"计划式旁白"标记：说了要继续查却结束了输出，没有给出最终结论 */
    private static final List<String> NARRATIVE_MARKERS = List.of(
            "让我看看", "让我查", "让我确认", "我看看", "我再查", "我再看", "确认一下",
            "接下来我", "这就看看", "这就去看", "继续查", "我进一步", "再帮您确认",
            "letmecheck", "letmesee", "iwillcheck");

    /** 去空白/标点后匹配的承诺标记（含历史故障中出现的英文搪塞句）。
     *  注意：不放"帮您查/帮你查"这类裸词——能力介绍"我可以帮你查询可预约服务"也会命中，
     *  只保留带明确"即将去做但还没做"时态的词。 */
    private static final List<String> PROMISE_MARKERS = List.of(
            "我这就", "我来帮", "我来查", "我来给", "我去查", "马上为", "马上帮", "马上查",
            "马上就", "稍等", "这就为", "这就帮", "这就去", "正在查", "正在为", "正在帮",
            "为您查询", "为你查询", "去查一下",
            "illquery", "letme", "willquery", "haveyouquery", "querydown", "queryforyou");

    /**
     * 英文"即将去查"句式（2026-09-29 复发：用户夹杂英文催促时模型整轮回英文承诺，
     * 如 "I'll look up the available consultants for you."，旧标记只覆盖 query 漏掉了 look up）。
     * 在保留单词边界的规范化原文（小写、弯引号转正、压空白）上匹配，
     * 避免 "ill" 命中 still/will 等普通词。
     */
    private static final Pattern ENGLISH_INTENT = Pattern.compile(
            "(?is)\\b(i'll|i will|let me|i'm going to|i am going to|going to|gonna)\\b"
                    + "[\\w', ]{0,40}?\\b(look up|look|check|query|search|find|retrieve)\\b");

    private ToolCallGuard() {
    }

    /**
     * @return true 表示该输出是"没有工具结果支撑的空承诺"（空白也算），应丢弃并重试
     */
    static boolean isEmptyPromise(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }
        String compact = text.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[\\s\\p{Punct}，。！？、：；…—·“”‘’（）()\\-]+", "");
        if (compact.isEmpty()) {
            return true;
        }
        if (compact.length() > MAX_PROMISE_LEN) {
            return false;
        }
        for (String marker : PROMISE_MARKERS) {
            if (compact.contains(marker)) {
                return true;
            }
        }
        // 英文承诺句："I'll look up ... for you."、"Let me check that."（零工具、短句）
        if (ENGLISH_INTENT.matcher(normalizeRaw(text)).find()) {
            return true;
        }
        // 历史故障中模型输出过整句英文搪塞（无任何中文、无数据）
        if (!containsChinese(compact) && compact.contains("query")) {
            return true;
        }
        return false;
    }

    /**
     * 工具已经执行过、但最终回答仍停在"我再去看看/让我确认一下"的计划式旁白，
     * 没有给出基于工具数据的最终结论（2026-09-26 端到端实测出现：
     * searchConsultants 返回后模型说"让我看看两位老师的详细时间"就结束）。
     *
     * @return true 表示需要带"立即给出最终结论"的指令补救一次
     */
    static boolean isIncompleteNarrative(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String compact = text.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[\\s\\p{Punct}，。！？、：；…—·“”‘’（）()\\-]+", "");
        if (compact.length() > MAX_NARRATIVE_LEN) {
            return false;
        }
        for (String marker : NARRATIVE_MARKERS) {
            if (compact.contains(marker)) {
                return true;
            }
        }
        // 英文计划旁白："Let me check the time slots first."（工具已返回但没给结论）
        return ENGLISH_INTENT.matcher(normalizeRaw(text)).find();
    }

    /** 模型零工具空承诺后，追加到重试请求末尾的强制指令 */
    static String forcedRetryInstruction() {
        return """

                【系统纠正·最高优先级】你上一轮只回复了"要去查询"，却没有真正调用任何工具。
                现在必须立即调用合适的查询工具（searchConsultants / searchRooms /
                searchEquipment / searchAvailableServices / searchMyBookings），
                参数从上文【当前已知的预约条件】和对话历史继承，拿到工具返回的真实数据后
                再用简体中文回答。严禁再次只输出"我这就查询"之类的承诺而不调用工具；
                若确实缺少必填参数，只用一句话说明缺什么，不要承诺会去查。""";
    }

    /** 工具已返回但模型停在"计划式旁白"时，要求其立即补全最终结论的指令 */
    static String narrativeRetryInstruction() {
        return """

                【系统纠正·最高优先级】你上一轮已经拿到了工具返回的数据，却只说了下一步打算
                （"让我看看/我再确认"）就结束了，没有给出最终答复。现在请：
                若已有工具数据足以回答，直接基于这些数据给出完整的简体中文结论；
                若必须再调用工具才能回答（例如需要具体时段），立即继续调用工具后给出结论。
                严禁再次只描述计划而不给结论。""";
    }

    /** 两次都失败时给用户的诚实兜底（不假装查到、不再空头承诺） */
    static String retryExhaustedMessage() {
        return "抱歉，这次预约信息没有查询成功。请您稍后再问一次，或换个说法重新描述"
                + "校区、日期和时段（例如“仓前校区明天上午9点到10点的教师咨询”）。";
    }

    private static boolean containsChinese(String s) {
        return s.codePoints().anyMatch(c -> c >= 0x4E00 && c <= 0x9FFF);
    }

    /** 保留单词边界的规范化：小写、弯引号转正、空白压缩，供英文意图正则使用 */
    private static String normalizeRaw(String text) {
        return text.toLowerCase(java.util.Locale.ROOT)
                .replace('’', '\'').replace('‘', '\'')
                .replaceAll("\\s+", " ").trim();
    }
}
