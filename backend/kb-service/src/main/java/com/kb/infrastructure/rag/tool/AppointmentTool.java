package com.kb.infrastructure.rag.tool;

import com.kb.infrastructure.client.CasAvailability;
import com.kb.infrastructure.client.CasClient;
import com.kb.infrastructure.client.CasResult;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 预约实时查询工具（LangChain4j Function Calling）。
 * <p>
 * 供 LLM 在用户询问"有哪些服务可预约 / 余量"时调用，返回 CAS 的实时预约数据，
 * 实现"静态 RAG + 实时数据"相结合的闭环。
 * </p>
 *
 * @author forever-king
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentTool {

    private final CasClient casClient;

    /**
     * 查询当前可预约的校园服务及其实时预约余量。
     */
    @Tool("查询当前可预约的校园服务列表及其实时预约余量，例如自习室、心理咨询、会议室、设备借用等。返回服务名称、描述与当前预约单数。")
    public String queryAppointmentAvailability() {
        try {
            CasResult<List<CasAvailability>> result = casClient.getAvailability();
            if (!result.isSuccess() || result.getData() == null || result.getData().isEmpty()) {
                return "当前没有可预约的校园服务。";
            }
            StringBuilder sb = new StringBuilder("当前可预约的校园服务及余量如下：\n");
            for (CasAvailability a : result.getData()) {
                sb.append("- ").append(a.getServiceName())
                        .append("（当前预约 ").append(a.getBookingCount() == null ? 0 : a.getBookingCount())
                        .append(" 单）：").append(a.getServiceDescribe()).append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("查询预约余量失败", e);
            return "预约数据服务暂时不可用，请稍后再试。";
        }
    }
}
