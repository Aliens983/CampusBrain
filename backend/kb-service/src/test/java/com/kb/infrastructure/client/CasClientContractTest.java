package com.kb.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.infrastructure.client.dto.CasServiceOption;
import com.kb.infrastructure.client.dto.CasTimeSlot;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import feign.Feign;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.beans.factory.ObjectFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KB → CAS Feign 契约测试。
 * <p>
 * 背景：CAS 的响应 DTO 与 KB 的 {@code Cas*} DTO 是两份手工维护的拷贝，
 * 编译期完全不共享类型；只要 CAS 侧把 JSON 字段改名（如 serviceDescribe、
 * availableSlotCount），KB 运行期静默收到 null，单测全绿、CI 无法发现。
 * <p>
 * 本测试用 JDK 内置 HttpServer 扮演 CAS，返回按「CAS 当前真实出参格式」
 * 手写的 JSON，再用真实 Feign（SpringMvcContract + Jackson 解码）发起调用：
 * 字段名一旦漂移，解码结果即为 null，测试立即失败。不引入 WireMock 等新
 * 依赖（离线构建环境可用）。
 *
 * @author forever-king
 */
class CasClientContractTest {

    private static HttpServer server;
    private static CasClient casClient;

    /** 记录最后一次请求，用于钉死请求路径/查询串契约 */
    private static final AtomicReference<String> LAST_REQUEST_URI = new AtomicReference<>();

    @BeforeAll
    static void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", CasClientContractTest::handle);
        server.start();

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectFactory<HttpMessageConverters> converters =
                () -> new HttpMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper));
        casClient = Feign.builder()
                .contract(new SpringMvcContract())
                .encoder(new SpringEncoder(converters))
                .decoder(new ResponseEntityDecoder(new SpringDecoder(converters)))
                .target(CasClient.class, "http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterAll
    static void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    /** 按 CAS CommonResult 包装的固定响应 */
    private static void handle(HttpExchange exchange) throws IOException {
        LAST_REQUEST_URI.set(exchange.getRequestURI().toASCIIString());
        String body;
        String path = exchange.getRequestURI().getPath();
        if (path.endsWith("/assistant/services")) {
            body = """
                    {"code":200,"message":"success","data":[{
                      "serviceId":7,"serviceName":"心理咨询（学业辅导）",
                      "serviceDescribe":"面向在校生的一对一心理咨询",
                      "campus":"cq","campusName":"仓前校区",
                      "categoryCode":"teacher","categoryName":"教师咨询",
                      "capacity":50,"bookedCount":10,"remaining":40,
                      "bookable":true,"bookableReason":null
                    }]}
                    """;
        } else if (path.endsWith("/slots")) {
            body = """
                    {"code":200,"message":"success","data":[
                      {"slotId":901,"startTime":"09:00","endTime":"09:30","available":"1"},
                      {"slotId":902,"startTime":"10:00","endTime":"10:30","available":"0"}
                    ]}
                    """;
        } else if (path.endsWith("/assistant/my-bookings")) {
            body = """
                    {"code":200,"message":"success","data":[]}
                    """;
        } else if (path.endsWith("/availability")) {
            // 业务失败契约：非 200 code 必须被识别为失败，而不是抛异常或误判成功
            body = """
                    {"code":500,"message":"服务暂不可用","data":null}
                    """;
        } else {
            body = "{\"code\":404,\"message\":\"not found\",\"data\":null}";
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    @Test
    @DisplayName("服务列表：code/message/data 包装与 12 个字段名契约")
    void assistantServices_jsonFieldContract() {
        CasResult<List<CasServiceOption>> result =
                casClient.getAssistantServices("cq", "teacher", "xinli");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        CasServiceOption s = result.getData().get(0);
        // 以下任一字段在 CAS 侧改名，这里都会变成 null/0 —— 正是本测试要拦截的漂移
        assertThat(s.getServiceId()).isEqualTo(7L);
        assertThat(s.getServiceName()).isEqualTo("心理咨询（学业辅导）");
        assertThat(s.getServiceDescribe()).isEqualTo("面向在校生的一对一心理咨询");
        assertThat(s.getCampus()).isEqualTo("cq");
        assertThat(s.getCampusName()).isEqualTo("仓前校区");
        assertThat(s.getCategoryCode()).isEqualTo("teacher");
        assertThat(s.getCategoryName()).isEqualTo("教师咨询");
        assertThat(s.getCapacity()).isEqualTo(50);
        assertThat(s.getBookedCount()).isEqualTo(10);
        assertThat(s.getRemaining()).isEqualTo(40);
        assertThat(s.getBookable()).isTrue();
        assertThat(s.getBookableReason()).isNull();

        // 请求契约：@RequestParam 名称必须与 CAS Controller 一致
        String uri = LAST_REQUEST_URI.get();
        assertThat(uri).contains("campus=cq").contains("category=teacher").contains("keyword=xinli");
    }

    @Test
    @DisplayName("咨询时段：slotId/startTime/endTime/available 字段名契约 + PathVariable 透传")
    void consultantSlots_jsonFieldContract() {
        CasResult<List<CasTimeSlot>> result = casClient.getConsultantSlots(55L, "2026-09-20");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(2);
        CasTimeSlot first = result.getData().get(0);
        assertThat(first.getSlotId()).isEqualTo(901L);
        assertThat(first.getStartTime()).isEqualTo("09:00");
        assertThat(first.getEndTime()).isEqualTo("09:30");
        assertThat(first.getAvailable()).isEqualTo("1");
        assertThat(result.getData().get(1).getAvailable()).isEqualTo("0");

        assertThat(LAST_REQUEST_URI.get())
                .contains("/consultants/55/slots")
                .contains("date=2026-09-20");
    }

    @Test
    @DisplayName("空列表契约：data 为空数组时得到空集合而非 null")
    void emptyData_decodesToEmptyList() {
        CasResult<List<CasBooking>> result = casClient.getMyBookingsByStatus(1);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
        assertThat(LAST_REQUEST_URI.get()).contains("manageStatus=1");
    }

    @Test
    @DisplayName("业务失败契约：code=500 时 isSuccess() 为 false 且 message 透传")
    void businessError_notSuccess() {
        CasResult<List<CasAvailability>> result = casClient.getAvailability();

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("服务暂不可用");
        assertThat(result.getData()).isNull();
    }
}
