package com.kb.domain.rag;

/**
 * 预约意图粗粒度分类器（2.8 深度审查 P2）。
 * <p>
 * 判断用户问题是否可能涉及"实时预约数据"（余量/档期/我的预约/资源可用性等），
 * 供缓存守卫、回答管线与问答编排<b>各自独立注入</b>，避免 CacheGuard 依赖 AnswerPipeline
 * 形成"缓存守卫→回答管线"的越界依赖与三角互知。
 * </p>
 * <p>
 * 判据：命中预约关键词粗筛即返回 true；进入工具链路后是否真正调用工具由 LLM 自主决定，
 * 本接口只做路由层的粗筛。
 * </p>
 *
 * @author forever-king
 */
public interface IntentClassifier {

    /**
     * 是否为可能涉及实时预约数据的提问。
     *
     * @param q 用户问题原文/改写后文本
     * @return true 表示应走预约工具链路（不读不写知识问答缓存）
     */
    boolean isAppointmentQuery(String q);
}