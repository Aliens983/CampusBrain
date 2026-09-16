package com.kb.infrastructure.client;

import com.kb.infrastructure.client.dto.CasBookingDraft;
import com.kb.infrastructure.client.dto.CasBookingDraftRequest;
import com.kb.infrastructure.client.dto.CasBookingResult;
import com.kb.infrastructure.client.dto.CasConsultantOption;
import com.kb.infrastructure.client.dto.CasEquipmentOption;
import com.kb.infrastructure.client.dto.CasRoomOption;
import com.kb.infrastructure.client.dto.CasServiceOption;
import com.kb.infrastructure.client.dto.CasTimeSlot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CAS Feign 客户端统一降级工厂。
 * <p>
 * CAS 慢、FullGC、线程池耗尽或 Nacos 下线瞬间，Feign 调用会在超时/连接失败时
 * 进入这里，而不是把异常抛给问答链路：此前 7 个 Function Calling 工具各自
 * try-catch，且发生在 SSE 链路中，少量挂起即可占满问答线程；且漏网的异常
 * 会打断整轮流式输出。
 * <p>
 * 降级策略（结构化、可区分）：
 * <ul>
 *   <li>所有方法返回 {@code code=503} 的 {@link CasResult}，data 为 null，
 *       message 为统一的用户可读提示，调用方按普通业务失败分支处理；</li>
 *   <li>真实异常原因在工厂入口记录 warn 日志，保留排障线索，不在用户回答中
 *       暴露内部主机/堆栈信息。</li>
 * </ul>
 * 注意：本类生效依赖 {@code spring.cloud.openfeign.circuitbreaker.enabled=true}。
 *
 * @author forever-king
 */
@Slf4j
@Component
public class CasClientFallbackFactory implements FallbackFactory<CasClient> {

    /** CAS 不可用时统一返回给用户的提示（工具/应用层直接透传 result.message） */
    public static final String SERVICE_UNAVAILABLE_MESSAGE = "预约数据服务暂时不可用，请稍后再试。";

    /** 降级响应业务码：与 HTTP 503 语义对齐 */
    public static final int FALLBACK_CODE = 503;

    @Override
    public CasClient create(Throwable cause) {
        log.warn("CAS 服务调用失败，进入统一降级: {}", cause.toString());

        return new CasClient() {
            @Override
            public CasResult<List<CasAvailability>> getAvailability() {
                return unavailable();
            }

            @Override
            public CasResult<List<CasBooking>> getMyBookings() {
                return unavailable();
            }

            @Override
            public CasResult<List<CasServiceOption>> getAssistantServices(
                    String campus, String category, String keyword) {
                return unavailable();
            }

            @Override
            public CasResult<List<CasConsultantOption>> getAssistantConsultants(
                    String campus, String keyword, String date) {
                return unavailable();
            }

            @Override
            public CasResult<List<CasTimeSlot>> getConsultantSlots(Long consultantId, String date) {
                return unavailable();
            }

            @Override
            public CasResult<List<CasRoomOption>> getAssistantRooms(
                    String campus, String date, String startTime, String endTime) {
                return unavailable();
            }

            @Override
            public CasResult<List<CasEquipmentOption>> getAssistantEquipment(
                    String campus, String keyword, String date, String startTime, String endTime) {
                return unavailable();
            }

            @Override
            public CasResult<List<CasBooking>> getMyBookingsByStatus(Integer manageStatus) {
                return unavailable();
            }

            @Override
            public CasResult<CasBookingDraft> createBookingDraft(CasBookingDraftRequest request) {
                return unavailable();
            }

            @Override
            public CasResult<CasBookingDraft> getBookingDraft(String draftId) {
                return unavailable();
            }

            @Override
            public CasResult<Void> discardBookingDraft(String draftId) {
                return unavailable();
            }

            @Override
            public CasResult<CasBookingResult> confirmBookingDraft(String draftId) {
                return unavailable();
            }

            @Override
            public CasResult<CasBookingResult> cancelBooking(Long orderId) {
                return unavailable();
            }
        };
    }

    private static <T> CasResult<T> unavailable() {
        CasResult<T> result = new CasResult<>();
        result.setCode(FALLBACK_CODE);
        result.setMessage(SERVICE_UNAVAILABLE_MESSAGE);
        result.setData(null);
        return result;
    }
}
