package com.laoliu.cas.appointment.application.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laoliu.cas.appointment.application.service.BookService;
import com.laoliu.cas.appointment.application.service.ServiceItemService;
import com.laoliu.cas.appointment.application.service.ConsultationService;
import com.laoliu.cas.appointment.application.service.EquipmentService;
import com.laoliu.cas.appointment.application.service.RoomService;
import com.laoliu.cas.appointment.interfaces.dto.request.AssistantBookingDraftRequest;
import com.laoliu.cas.appointment.interfaces.dto.response.AssistantBookingDraft;
import com.laoliu.cas.appointment.interfaces.dto.response.AssistantBookingResult;
import com.laoliu.cas.appointment.interfaces.dto.response.AssistantConsultantResponse;
import com.laoliu.cas.appointment.interfaces.dto.response.AssistantEquipmentResponse;
import com.laoliu.cas.appointment.interfaces.dto.response.AssistantRoomResponse;
import com.laoliu.cas.appointment.domain.entity.Equipment;
import com.laoliu.cas.appointment.domain.entity.Room;
import com.laoliu.cas.appointment.domain.entity.ServiceItem;
import com.laoliu.cas.appointment.domain.entity.ServiceCategory;
import com.laoliu.cas.appointment.domain.entity.Consultant;
import com.laoliu.cas.appointment.domain.entity.TimeSlot;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.domain.repository.ConsultantRepository;
import com.laoliu.cas.appointment.domain.repository.EquipmentRepository;
import com.laoliu.cas.appointment.domain.repository.RoomRepository;
import com.laoliu.cas.appointment.domain.repository.ServiceCategoryRepository;
import com.laoliu.cas.appointment.domain.repository.ServiceItemRepository;
import com.laoliu.cas.appointment.domain.repository.TimeSlotRepository;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.redis.util.RedisUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 预约助手服务测试
 * <p>
 * 重点覆盖两类高风险逻辑：
 * <ol>
 *   <li>草稿校验分支——校验漏一项就会造成超卖 / 双占；</li>
 *   <li>确认下单的派发与二次校验——这是"两段式预约"真正落库的地方。</li>
 * </ol>
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("预约助手服务")
class AppointmentAssistantServiceImplTest {

    @Mock private ServiceItemRepository serviceRepository;
    @Mock private ServiceCategoryRepository serviceCategoryRepository;
    @Mock private ServiceItemService serviceService;
    @Mock private ConsultantRepository consultantRepository;
    @Mock private TimeSlotRepository timeSlotRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private EquipmentRepository equipmentRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private BookService bookService;
    @Mock private ConsultationService consultationService;
    @Mock private RoomService roomService;
    @Mock private EquipmentService equipmentService;
    @Mock private RedisUtil redisUtil;
    @Mock private com.laoliu.cas.appointment.infrastructure.metrics.BookingMetrics bookingMetrics;

    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private AppointmentAssistantServiceImpl service;

    private ServiceCategory category(String code) {
        return ServiceCategory.builder().id(1L).code(code).name(code).sort(1).build();
    }

    private ServiceItem service(Long id, Integer capacity, Integer booked) {
        return ServiceItem.builder()
                .serviceId(id).serviceName("测试服务").serviceState(1)
                .categoryId(1L).campus("cq")
                .capacity(capacity).bookedCount(booked)
                .build();
    }

    private void stubCategory(String code) {
        when(serviceCategoryRepository.findAll()).thenReturn(List.of(category(code)));
    }

    @Nested
    @DisplayName("草稿校验：不通过时返回 valid=false 而非抛异常")
    class Validation {

        @Test
        @DisplayName("容量已满 → valid=false 并给出原因")
        void shouldRejectWhenCapacityFull() {
            stubCategory("activity");
            when(serviceRepository.findById(1L)).thenReturn(Optional.of(service(1L, 10, 10)));

            AssistantBookingDraft draft = service.createDraft(7L,
                    AssistantBookingDraftRequest.builder().serviceId(1L).build());

            assertFalse(draft.getValid());
            assertTrue(draft.getInvalidReason().contains("名额已满"));
            assertNull(draft.getDraftId(), "校验不通过不应分配草稿ID");
        }

        @Test
        @DisplayName("教室时段已被占用 → valid=false")
        void shouldRejectWhenRoomOccupied() {
            stubCategory("space");
            when(serviceRepository.findById(1L)).thenReturn(Optional.of(service(1L, -1, 0)));
            Room room = Room.builder().id(5L).name("A101").serviceId(1L).seats(40).build();
            when(roomRepository.findById(5L)).thenReturn(Optional.of(room));
            when(bookingRepository.countRoomOverlap(eq(5L), any(LocalDate.class), eq("09:00"), eq("10:00")))
                    .thenReturn(1);
            String date = LocalDate.now().plusDays(1).toString();

            AssistantBookingDraft draft = service.createDraft(7L,
                    AssistantBookingDraftRequest.builder()
                            .serviceId(1L).roomId(5L).date(date)
                            .startTime("09:00").endTime("10:00").build());

            assertFalse(draft.getValid());
            assertTrue(draft.getInvalidReason().contains("已被预约"));
        }

        @Test
        @DisplayName("资源不属于所选服务 → valid=false（防跨服务预约）")
        void shouldRejectWhenRoomBelongsToOtherService() {
            stubCategory("space");
            when(serviceRepository.findById(1L)).thenReturn(Optional.of(service(1L, -1, 0)));
            Room room = Room.builder().id(5L).name("A101").serviceId(99L).build();
            when(roomRepository.findById(5L)).thenReturn(Optional.of(room));
            String date = LocalDate.now().plusDays(1).toString();

            AssistantBookingDraft draft = service.createDraft(7L,
                    AssistantBookingDraftRequest.builder()
                            .serviceId(1L).roomId(5L).date(date)
                            .startTime("09:00").endTime("10:00").build());

            assertFalse(draft.getValid());
            assertTrue(draft.getInvalidReason().contains("不属于所选服务"));
        }

        @Test
        @DisplayName("过去的日期 → valid=false")
        void shouldRejectPastDate() {
            stubCategory("space");
            when(serviceRepository.findById(1L)).thenReturn(Optional.of(service(1L, -1, 0)));

            AssistantBookingDraft draft = service.createDraft(7L,
                    AssistantBookingDraftRequest.builder()
                            .serviceId(1L).roomId(5L).date("2020-01-01")
                            .startTime("09:00").endTime("10:00").build());

            assertFalse(draft.getValid());
            assertTrue(draft.getInvalidReason().contains("不能预约过去的日期"));
        }
    }

    @Nested
    @DisplayName("两段式预约：确认后才真正下单")
    class ConfirmFlow {

        @Test
        @DisplayName("生成草稿只校验不落库")
        void shouldNotBookWhenCreatingDraft() {
            stubCategory("activity");
            when(serviceRepository.findById(1L)).thenReturn(Optional.of(service(1L, -1, 0)));

            AssistantBookingDraft draft = service.createDraft(7L,
                    AssistantBookingDraftRequest.builder().serviceId(1L).build());

            assertTrue(draft.getValid());
            assertNotNull(draft.getDraftId());
            verify(bookService, never()).bookService(anyLong(), any());
        }

        @Test
        @DisplayName("确认后派发到既有下单服务，并清除草稿")
        void shouldDispatchBookingOnConfirm() throws Exception {
            stubCategory("activity");
            when(serviceRepository.findById(1L)).thenReturn(Optional.of(service(1L, -1, 0)));

            AssistantBookingDraft draft = service.createDraft(7L,
                    AssistantBookingDraftRequest.builder().serviceId(1L).build());
            // 捕获写入 Redis 的草稿 JSON，模拟确认时回读
            ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
            verify(redisUtil).set(anyString(), jsonCaptor.capture(), anyLong(), any());
            when(redisUtil.get(anyString())).thenReturn(jsonCaptor.getValue());
            // 3.3.1：下单服务返回真实回填 orderId 的提交结果（不再由助手事后猜测最新单）
            when(bookService.bookService(eq(7L), any()))
                    .thenReturn(new BookService.BookingSubmitResult(null, List.of(1001L)));

            AssistantBookingResult result = service.confirmDraft(7L, draft.getDraftId());

            verify(bookService).bookService(eq(7L), any());
            verify(redisUtil).delete(anyString());
            assertNotNull(result);
            assertEquals(1001L, result.getOrderId());
        }

        @Test
        @DisplayName("草稿过期（Redis 无值）→ 抛异常，绝不静默下单")
        void shouldRejectExpiredDraft() {
            when(redisUtil.get(anyString())).thenReturn(null);

            assertThrows(BusinessException.class, () -> service.confirmDraft(7L, "expired"));
            verify(bookService, never()).bookService(anyLong(), any());
        }

        @Test
        @DisplayName("确认时二次校验失败 → 不落库")
        void shouldRevalidateOnConfirm() throws Exception {
            stubCategory("activity");
            ServiceItem svc = service(1L, -1, 0);
            when(serviceRepository.findById(1L)).thenReturn(Optional.of(svc));

            AssistantBookingDraft draft = service.createDraft(7L,
                    AssistantBookingDraftRequest.builder().serviceId(1L).build());
            ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
            verify(redisUtil).set(anyString(), jsonCaptor.capture(), anyLong(), any());
            when(redisUtil.get(anyString())).thenReturn(jsonCaptor.getValue());

            // 生成草稿后到确认之间，名额被别人占满
            when(serviceRepository.findById(1L)).thenReturn(Optional.of(service(1L, 10, 10)));

            assertThrows(BusinessException.class, () -> service.confirmDraft(7L, draft.getDraftId()));
            verify(bookService, never()).bookService(anyLong(), any());
        }

        @Test
        @DisplayName("咨询预约：草稿保存 slotId，确认时二次校验通过并派发到咨询下单")
        void shouldConfirmConsultationDraft() throws Exception {
            stubCategory("teacher");
            when(serviceRepository.findById(1L)).thenReturn(Optional.of(service(1L, -1, 0)));
            Consultant consultant = Consultant.builder().id(3L).name("张老师").serviceId(1L).build();
            when(consultantRepository.findById(3L)).thenReturn(Optional.of(consultant));
            TimeSlot slot = TimeSlot.builder().id(9L).consultantId(3L)
                    .slotDate(LocalDate.now().plusDays(1))
                    .startTime("09:00").endTime("10:00").available(true).build();
            when(timeSlotRepository.findById(9L)).thenReturn(Optional.of(slot));

            AssistantBookingDraft draft = service.createDraft(7L,
                    AssistantBookingDraftRequest.builder()
                            .serviceId(1L).consultantId(3L).slotId(9L).build());

            assertTrue(draft.getValid());
            assertEquals(9L, draft.getSlotId(),
                    "草稿必须保存时段ID，否则确认时二次校验拿不到 slotId，咨询预约确认必然失败");

            ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
            verify(redisUtil).set(anyString(), jsonCaptor.capture(), anyLong(), any());
            when(redisUtil.get(anyString())).thenReturn(jsonCaptor.getValue());
            when(consultationService.bookConsultation(eq(7L), eq(3L), eq(9L))).thenReturn(2001L);

            AssistantBookingResult result = service.confirmDraft(7L, draft.getDraftId());

            // 必须派发到咨询下单，且第二个参数是 consultantId 而非 serviceId
            verify(consultationService).bookConsultation(7L, 3L, 9L);
            assertEquals(2001L, result.getOrderId());
        }
    }

    @Nested
    @DisplayName("查询性能与行为")
    class Query {

        @Test
        @DisplayName("教室列表只全量查一次，避免按服务逐个查的 N+1")
        void shouldLoadRoomsInOneQuery() {
            when(serviceService.getAvailableServices()).thenReturn(List.of(service(1L, -1, 0), service(2L, -1, 0)));
            when(roomRepository.findAll()).thenReturn(List.of(
                    Room.builder().id(1L).name("A101").serviceId(1L).build(),
                    Room.builder().id(2L).name("A102").serviceId(1L).build(),
                    Room.builder().id(3L).name("B101").serviceId(2L).build()));

            List<AssistantRoomResponse> rooms = service.findRooms(null, null, null, null);

            assertEquals(3, rooms.size());
            verify(roomRepository, times(1)).findAll();
            verify(roomRepository, never()).findByServiceId(anyLong());
        }

        @Test
        @DisplayName("按校区过滤教室")
        void shouldFilterRoomsByCampus() {
            when(serviceService.getAvailableServices()).thenReturn(List.of(
                    ServiceItem.builder().serviceId(1L).serviceName("仓前").serviceState(1).campus("cq").build(),
                    ServiceItem.builder().serviceId(2L).serviceName("下沙").serviceState(1).campus("xs").build()));
            when(roomRepository.findAll()).thenReturn(List.of(
                    Room.builder().id(1L).name("A101").serviceId(1L).build(),
                    Room.builder().id(3L).name("B101").serviceId(2L).build()));

            List<AssistantRoomResponse> rooms = service.findRooms("xs", null, null, null);

            assertEquals(1, rooms.size());
            assertEquals("B101", rooms.get(0).getName());
            assertEquals("xs", rooms.get(0).getCampus());
        }

        @Test
        @DisplayName("4.7 咨询师列表带日期：时段数一条 GROUP BY 批量取，不逐个 findAvailable")
        void shouldBatchLoadConsultantSlotCounts() {
            when(serviceRepository.findAll()).thenReturn(List.of(service(1L, -1, 0)));
            when(consultantRepository.findAll()).thenReturn(List.of(
                    Consultant.builder().id(1L).name("王老师").serviceId(1L).build(),
                    Consultant.builder().id(2L).name("李老师").serviceId(1L).build()));
            when(timeSlotRepository.countAvailableByConsultants(any(), any(LocalDate.class)))
                    .thenReturn(Map.of(1L, 3));

            List<AssistantConsultantResponse> list =
                    service.findConsultants(null, null, LocalDate.now().plusDays(1).toString());

            assertEquals(2, list.size());
            assertEquals(3, list.get(0).getAvailableSlotCount());
            assertEquals(0, list.get(1).getAvailableSlotCount(), "当天无可用时段的咨询师按 0 兜底");
            verify(timeSlotRepository, times(1)).countAvailableByConsultants(any(), any(LocalDate.class));
            verify(timeSlotRepository, never()).findAvailable(anyLong(), any(LocalDate.class));
        }

        @Test
        @DisplayName("4.7 咨询师列表不带日期：不触发任何时段查询")
        void shouldNotQuerySlotsWithoutDate() {
            when(serviceRepository.findAll()).thenReturn(List.of(service(1L, -1, 0)));
            when(consultantRepository.findAll()).thenReturn(List.of(
                    Consultant.builder().id(1L).name("王老师").serviceId(1L).build()));

            List<AssistantConsultantResponse> list = service.findConsultants(null, null, null);

            assertEquals(1, list.size());
            assertNull(list.get(0).getAvailableSlotCount());
            verify(timeSlotRepository, never()).countAvailableByConsultants(any(), any(LocalDate.class));
            verify(timeSlotRepository, never()).findAvailable(anyLong(), any(LocalDate.class));
        }

        @Test
        @DisplayName("4.7 教室列表带时间窗：占用数批量聚合，free 按命中数判定，不逐条 countRoomOverlap")
        void shouldBatchLoadRoomOverlap() {
            when(serviceService.getAvailableServices()).thenReturn(List.of(service(1L, -1, 0)));
            when(roomRepository.findAll()).thenReturn(List.of(
                    Room.builder().id(1L).name("A101").serviceId(1L).build(),
                    Room.builder().id(2L).name("A102").serviceId(1L).build()));
            when(bookingRepository.countRoomOverlapBatch(any(), any(LocalDate.class), anyString(), anyString()))
                    .thenReturn(Map.of(1L, 2));

            List<AssistantRoomResponse> rooms = service.findRooms(
                    null, LocalDate.now().plusDays(1).toString(), "09:00", "10:00");

            assertEquals(2, rooms.size());
            assertFalse(rooms.get(0).getFree(), "窗口内有占用 → 非空闲");
            assertTrue(rooms.get(1).getFree(), "结果集中缺省（0 条占用）→ 空闲");
            verify(bookingRepository, times(1))
                    .countRoomOverlapBatch(any(), any(LocalDate.class), anyString(), anyString());
            verify(bookingRepository, never())
                    .countRoomOverlap(anyLong(), any(LocalDate.class), anyString(), anyString());
        }

        @Test
        @DisplayName("4.7 设备列表带时间窗：占用台数批量聚合，余量按批量结果计算")
        void shouldBatchLoadEquipmentOverlap() {
            when(serviceRepository.findAll()).thenReturn(List.of(service(1L, -1, 0)));
            when(equipmentRepository.findAll()).thenReturn(List.of(
                    Equipment.builder().id(1L).name("投影仪").serviceId(1L).availableStock(5).build(),
                    Equipment.builder().id(2L).name("麦克风").serviceId(1L).availableStock(3).build()));
            when(bookingRepository.sumEquipmentOverlapBatch(any(), any(LocalDate.class), anyString(), anyString()))
                    .thenReturn(Map.of(1L, 5));

            List<AssistantEquipmentResponse> list = service.findEquipment(
                    null, null, LocalDate.now().plusDays(1).toString(), "09:00", "10:00");

            assertEquals(2, list.size());
            assertEquals(0, list.get(0).getRemainingForWindow(), "库存 5 占用 5 → 余量 0");
            assertEquals(3, list.get(1).getRemainingForWindow(), "结果集中缺省按占用 0 → 余量即库存");
            verify(bookingRepository, times(1))
                    .sumEquipmentOverlapBatch(any(), any(LocalDate.class), anyString(), anyString());
            verify(bookingRepository, never())
                    .sumEquipmentOverlap(anyLong(), any(LocalDate.class), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("取消预约")
    class Cancel {

        @Test
        @DisplayName("预约单不存在 → 抛异常")
        void shouldRejectUnknownOrder() {
            when(bookingRepository.getServiceStatusByOrderIdAndUserId(7L, 999L)).thenReturn(null);

            assertThrows(BusinessException.class, () -> service.cancelBooking(7L, 999L));
        }
    }
}
