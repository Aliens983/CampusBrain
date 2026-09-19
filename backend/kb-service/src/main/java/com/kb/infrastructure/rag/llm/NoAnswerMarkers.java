package com.kb.infrastructure.rag.llm;

import java.util.List;

/**
 * RAG 回答"未基于本地资料作答"的标记词判定。
 * <p>
 * 流式链路在缓冲前缀阶段（前 {@code NO_ANSWER_GATE_CHARS} 字）用它决定是否中止 RAG 推流、
 * 改用模型直接兜底；同步链路生成完整回答后做同样判定。两处共用同一份词表，避免口径漂移。
 * </p>
 * <p>
 * 3.11（深度审查 P2）：
 * <ul>
 *   <li><b>假阴性修正</b>：补入"无法给出 / 暂无相关信息 / 资料中没有提及 / 未能找到 / 无法作答"
 *       等常见否定措辞，原词表缺这些导致前缀满 16 字即开闸推"无法回答"文案却不再切兜底；</li>
 *   <li><b>假阳性窗口</b>：流式判定只在<b>前 16 字缓冲期</b>内执行，正文引用块（如"文档中未找到…的原句"）
 *       出现在缓冲期之后，不会被本条规则误判；词表在同步完整回答判定时沿用同一语义。</li>
 * </ul>
 * 词表仍为兜底信号；长期方案（检索分数阈值 + 模型结构化信号）见 3.11 建议。
 * </p>
 *
 * @author forever-king
 */
public final class NoAnswerMarkers {

    private static final List<String> MARKERS = List.of(
            "无法回答", "没有相关", "未包含", "暂无法", "没有找到", "未找到",
            "无法根据", "无法为您", "没有足够的", "文档中未",
            "无法给出", "无法作答", "暂无相关信息", "资料中没有提及", "未能找到");

    private NoAnswerMarkers() {
    }

    public static boolean looksLikeNoAnswer(String answer) {
        if (answer == null || answer.isBlank()) {
            return true;
        }
        for (String marker : MARKERS) {
            if (answer.contains(marker)) {
                return true;
            }
        }
        return false;
    }
}
