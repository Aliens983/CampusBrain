package com.kb.infrastructure.rag.llm;

import java.util.List;

/**
 * RAG 回答"未基于本地资料作答"的标记词判定。
 * <p>
 * 流式链路在缓冲前缀阶段用它决定是否中止 RAG 推流、改用模型直接兜底；
 * 同步链路生成完整回答后做同样判定。两处共用同一份词表，避免口径漂移。
 * </p>
 *
 * @author forever-king
 */
public final class NoAnswerMarkers {

    private static final List<String> MARKERS = List.of(
            "无法回答", "没有相关", "未包含", "暂无法", "没有找到", "未找到",
            "无法根据", "无法为您", "没有足够的", "文档中未");

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
