package com.kb.infrastructure.rag.intent;

import com.kb.domain.rag.IntentClassifier;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 关键词粗筛的预约意图分类器（2.8）。
 * <p>
 * 路由词表收窄（3.3.4）：仅保留"单独出现也强烈指向预约动作/资源"的词；
 * 校区、人物身份等须与预约词共现，宁可不进也不要乱进——混入"老师/教师/咨询/
 * 设备/仓前/下沙"等歧义词会把"教师招聘政策""设备处报修电话""仓前食堂在哪"
 * 这类纯知识库问题也误路由进预约工具链路。
 * </p>
 *
 * @author forever-king
 */
@Component
public class KeywordIntentClassifier implements IntentClassifier {

    private static final List<String> APPOINTMENT_KEYWORDS = List.of(
            "可预约", "预约", "余量", "名额", "会议室", "设备借用", "借用", "自习室",
            "场地", "空闲", "可约", "档期", "怎么预约", "怎么约", "能不能约",
            "有哪些服务", "还有哪些", "心理咨询", "教室", "取消预约", "我的预约");

    @Override
    public boolean isAppointmentQuery(String q) {
        if (q == null || q.isEmpty()) {
            return false;
        }
        return APPOINTMENT_KEYWORDS.stream().anyMatch(q::contains);
    }
}