package com.kb.infrastructure.rag.tool;

import com.kb.domain.chat.BookingSlots;
import com.kb.domain.chat.ChatContextHolder;
import com.kb.domain.chat.ChatSession;
import com.kb.infrastructure.client.CasClient;
import com.kb.infrastructure.client.CasResult;
import com.kb.infrastructure.client.dto.CasBookingDraft;
import com.kb.infrastructure.client.dto.CasBookingDraftRequest;
import com.kb.infrastructure.client.dto.CasConsultantOption;
import com.kb.infrastructure.client.dto.CasTimeSlot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DeterministicBookingFallback} 单元测试：
 * 模型两轮零工具后由服务端确定性接管教师咨询预约链路的各分支。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("教师咨询预约确定性兜底")
class DeterministicBookingFallbackTest {

    @Mock
    private CasClient casClient;
    @Mock
    private com.kb.domain.chat.ChatSessionRepository chatSessionRepository;

    private AppointmentTool appointmentTool;
    private DeterministicBookingFallback fallback;
    private ChatSession session;

    @BeforeEach
    void setUp() {
        appointmentTool = new AppointmentTool(casClient, chatSessionRepository);
        fallback = new DeterministicBookingFallback(casClient, appointmentTool, chatSessionRepository);
        session = new ChatSession();
    }

    @AfterEach
    void clearContext() {
        ChatContextHolder.clear();
    }

    private void bindSlots(BookingSlots slots) {
        ChatContextHolder.set(new ChatContextHolder.ChatContext("sid", 1L, slots));
    }

    private BookingSlots completeSlots() {
        return BookingSlots.builder()
                .campus("xs").category("teacher").date("2026-10-05")
                .startTime("09:00").endTime("10:00")
                .build();
    }

    private CasConsultantOption consultant(long id, String name, long serviceId) {
        CasConsultantOption c = new CasConsultantOption();
        c.setConsultantId(id);
        c.setName(name);
        c.setDepartment("心理咨询中心");
        c.setServiceId(serviceId);
        c.setServiceName("教师心理咨询");
        return c;
    }

    private CasResult<List<CasConsultantOption>> consultantsResult(CasConsultantOption... cs) {
        CasResult<List<CasConsultantOption>> r = new CasResult<>();
        r.setCode(200);
        r.setData(List.of(cs));
        return r;
    }

    private CasResult<List<CasTimeSlot>> slotsResult(CasTimeSlot... ts) {
        CasResult<List<CasTimeSlot>> r = new CasResult<>();
        r.setCode(200);
        r.setData(List.of(ts));
        return r;
    }

    private CasTimeSlot timeSlot(long id, String start, String end) {
        CasTimeSlot t = new CasTimeSlot();
        t.setSlotId(id);
        t.setStartTime(start);
        t.setEndTime(end);
        return t;
    }

    private void mockDraftSuccess() {
        CasBookingDraft draft = new CasBookingDraft();
        draft.setValid(true);
        draft.setDraftId("draft-1");
        draft.setSummary("下沙校区 · 姚老师 · 2026-10-05 09:00-10:00 心理咨询");
        CasResult<CasBookingDraft> r = new CasResult<>();
        r.setCode(200);
        r.setData(draft);
        when(casClient.createBookingDraft(any(CasBookingDraftRequest.class))).thenReturn(r);
    }

    @Test
    @DisplayName("槽位不齐（缺时段）时不接管，返回 null")
    void missingSlots_returnsNull() {
        BookingSlots slots = BookingSlots.builder()
                .campus("xs").category("teacher").date("2026-10-05").build();
        bindSlots(slots);

        assertThat(fallback.tryPrepareTeacherBooking("帮我预约姚老师")).isNull();
        verify(casClient, never()).getAssistantConsultants(any(), any(), any());
    }

    @Test
    @DisplayName("非教师咨询分类不接管")
    void nonTeacherCategory_returnsNull() {
        bindSlots(BookingSlots.builder()
                .campus("xs").category("space").date("2026-10-05")
                .startTime("09:00").endTime("10:00").build());

        assertThat(fallback.tryPrepareTeacherBooking("帮我预约自习室")).isNull();
    }

    @Test
    @DisplayName("唯一老师 + 时段匹配：生成草稿并请用户确认，姓名关键词从原话抽取")
    void uniqueConsultantAndMatchingSlot_preparesDraft() {
        bindSlots(completeSlots());
        when(casClient.getAssistantConsultants("xs", "姚", "2026-10-05"))
                .thenReturn(consultantsResult(consultant(7L, "姚静", 100L)));
        when(casClient.getConsultantSlots(7L, "2026-10-05"))
                .thenReturn(slotsResult(timeSlot(901L, "09:00", "10:00"),
                        timeSlot(902L, "11:00", "12:00")));
        mockDraftSuccess();
        when(chatSessionRepository.loadForUser("sid", 1L)).thenReturn(session);

        String reply = fallback.tryPrepareTeacherBooking(
                "帮我预约下周一上午9点到10点下沙校区姚老师的心理咨询");

        assertThat(reply).contains("姚老师").contains("确认");
        ArgumentCaptor<CasBookingDraftRequest> captor =
                ArgumentCaptor.forClass(CasBookingDraftRequest.class);
        verify(casClient).createBookingDraft(captor.capture());
        CasBookingDraftRequest req = captor.getValue();
        assertThat(req.getConsultantId()).isEqualTo(7L);
        assertThat(req.getServiceId()).isEqualTo(100L);
        assertThat(req.getSlotId()).isEqualTo(901L);
        assertThat(req.getDate()).isEqualTo("2026-10-05");
        // 草稿已挂到会话，上层 persistAndNotify 会据此下发 confirm 事件
        assertThat(session.getPendingBooking()).isNotNull();
        assertThat(session.getPendingBooking().getSummary()).contains("姚老师");
    }

    @Test
    @DisplayName("多位老师重名：列出候选请用户选择，不生成草稿")
    void multipleConsultants_asksWhichOne() {
        bindSlots(completeSlots());
        when(casClient.getAssistantConsultants("xs", "管", "2026-10-05"))
                .thenReturn(consultantsResult(
                        consultant(5L, "管明", 100L), consultant(9L, "管芳", 101L)));

        String reply = fallback.tryPrepareTeacherBooking("帮我预约下周一上午9点下沙校区管老师");

        assertThat(reply).contains("管明").contains("管芳").contains("哪一位");
        verify(casClient, never()).getConsultantSlots(any(), any());
        verify(casClient, never()).createBookingDraft(any());
    }

    @Test
    @DisplayName("目标时段不存在：列出当天可约时段追问")
    void requestedSlotMissing_listsAlternatives() {
        bindSlots(completeSlots());
        when(casClient.getAssistantConsultants("xs", "姚", "2026-10-05"))
                .thenReturn(consultantsResult(consultant(7L, "姚静", 100L)));
        when(casClient.getConsultantSlots(7L, "2026-10-05"))
                .thenReturn(slotsResult(timeSlot(902L, "11:00", "12:00"),
                        timeSlot(903L, "14:00", "15:00")));

        String reply = fallback.tryPrepareTeacherBooking("帮我预约下周一上午9点到10点下沙校区姚老师");

        assertThat(reply).contains("11:00-12:00").contains("14:00-15:00").contains("哪个时段");
        verify(casClient, never()).createBookingDraft(any());
    }

    @Test
    @DisplayName("周末当天无时段：明确说明周末不排班而非约满")
    void weekendNoSlots_explainsNoScheduling() {
        BookingSlots slots = BookingSlots.builder()
                .campus("xs").category("teacher").date("2026-10-03")
                .startTime("09:00").endTime("10:00").build();
        bindSlots(slots);
        when(casClient.getAssistantConsultants("xs", "姚", "2026-10-03"))
                .thenReturn(consultantsResult(consultant(7L, "姚静", 100L)));
        when(casClient.getConsultantSlots(7L, "2026-10-03"))
                .thenReturn(new CasResult<>() {{
                    setCode(200);
                    setData(List.of());
                }});

        String reply = fallback.tryPrepareTeacherBooking("帮我预约这周六上午9点下沙校区姚老师");

        assertThat(reply).contains("周末").contains("工作日");
        verify(casClient, never()).createBookingDraft(any());
    }

    @Test
    @DisplayName("查无此老师：提示换姓名/校区")
    void noConsultant_returnsNotFoundHint() {
        bindSlots(completeSlots());
        when(casClient.getAssistantConsultants("xs", "王", "2026-10-05"))
                .thenReturn(new CasResult<>() {{
                    setCode(200);
                    setData(List.of());
                }});

        String reply = fallback.tryPrepareTeacherBooking("帮我预约下周一上午9点下沙校区王老师");

        assertThat(reply).contains("没有查到").contains("王");
    }

    @Test
    @DisplayName("姓名抽取：校区/时间词不会混进姓名关键词")
    void teacherNameExtraction() {
        assertThat(DeterministicBookingFallback.extractTeacherName(
                "帮我预约下周一上午9点到10点下沙校区姚老师的心理咨询")).isEqualTo("姚");
        assertThat(DeterministicBookingFallback.extractTeacherName(
                "帮我预约这周六上午9点下沙校区欧阳老师")).isEqualTo("欧阳");
        assertThat(DeterministicBookingFallback.extractTeacherName("今天天气不错")).isNull();
    }

    @Test
    @DisplayName("CAS 不可用时透传用户可读错误，不生成草稿")
    void casUnavailable_returnsCasMessage() {
        bindSlots(completeSlots());
        CasResult<List<CasConsultantOption>> err = new CasResult<>();
        err.setCode(503);
        err.setMessage("预约服务暂不可用，请稍后再试");
        when(casClient.getAssistantConsultants("xs", "姚", "2026-10-05")).thenReturn(err);

        String reply = fallback.tryPrepareTeacherBooking("帮我预约下周一上午9点到10点下沙校区姚老师");

        assertThat(reply).contains("暂不可用");
        verify(casClient, never()).createBookingDraft(any());
    }
}
