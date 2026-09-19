package com.laoliu.cas.appointment.application.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laoliu.cas.appointment.application.service.BookService;
import com.laoliu.cas.appointment.application.service.ServiceItemService;
import com.laoliu.cas.appointment.application.service.ConsultationService;
import com.laoliu.cas.appointment.application.service.EquipmentService;
import com.laoliu.cas.appointment.application.service.RoomService;
import com.laoliu.cas.appointment.infrastructure.metrics.BookingMetrics;
import com.laoliu.cas.appointment.interfaces.dto.request.AssistantBookingDraftRequest;
import com.laoliu.cas.appointment.interfaces.dto.response.AssistantBookingDraft;
import com.laoliu.cas.appointment.interfaces.dto.response.AssistantBookingResult;
import com.laoliu.cas.appointment.interfaces.dto.response.AssistantConsultantResponse;
import com.laoliu.cas.appointment.interfaces.dto.response.AssistantEquipmentResponse;
import com.laoliu.cas.appointment.interfaces.dto.response.AssistantRoomResponse;
import com.laoliu.cas.appointment.interfaces.dto.response.AssistantServiceResponse;
import com.laoliu.cas.appointment.application.service.AppointmentAssistantService;
import com.laoliu.cas.appointment.domain.entity.Consultant;
import com.laoliu.cas.appointment.domain.entity.Equipment;
import com.laoliu.cas.appointment.domain.entity.Room;
import com.laoliu.cas.appointment.domain.entity.ServiceItem;
import org.springframework.stereotype.Service;
import com.laoliu.cas.appointment.domain.entity.ServiceCategory;
import com.laoliu.cas.appointment.domain.entity.TimeSlot;
import com.laoliu.cas.appointment.domain.enums.CategoryCode;
import com.laoliu.cas.appointment.domain.repository.BookingRepository;
import com.laoliu.cas.appointment.domain.repository.ConsultantRepository;
import com.laoliu.cas.appointment.domain.repository.EquipmentRepository;
import com.laoliu.cas.appointment.domain.repository.RoomRepository;
import com.laoliu.cas.appointment.domain.repository.ServiceCategoryRepository;
import com.laoliu.cas.appointment.domain.repository.ServiceItemRepository;
import com.laoliu.cas.appointment.domain.repository.TimeSlotRepository;
import com.laoliu.cas.appointment.interfaces.dto.request.ConsultationBookRequest;
import com.laoliu.cas.appointment.interfaces.dto.request.EquipmentBookRequest;
import com.laoliu.cas.appointment.interfaces.dto.request.RoomBookRequest;
import com.laoliu.cas.appointment.domain.view.BookingQueryView;
import com.laoliu.cas.appointment.domain.view.TimeSlotView;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.BookErrorCode;
import com.laoliu.cas.redis.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 预约助手应用服务实现
 * <p>
 * 只做「读」与「两段式预约」：草稿阶段纯校验预览，确认阶段复用既有下单服务，
 * 保证与用户端走完全相同的防冲突 / 防超卖 / 幂等逻辑，不另起一套。
 *
 * @author forever-king
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentAssistantServiceImpl implements AppointmentAssistantService {

    /** 草稿在 Redis 中的存活时长（分钟） */
    private static final long DRAFT_TTL_MINUTES = 10L;

    /** 草稿 Redis key 前缀（按用户隔离，防越权读取他人草稿） */
    private static final String DRAFT_KEY_PREFIX = "assistant:booking:draft:";

    private final ServiceItemRepository serviceRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    /** 走应用服务（带 @Cacheable）而非直接查库，避免每次助手问答全表扫服务 */
    private final ServiceItemService serviceItemService;
    private final ConsultantRepository consultantRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final RoomRepository roomRepository;
    private final EquipmentRepository equipmentRepository;
    private final BookingRepository bookingRepository;
    private final BookService bookService;
    private final ConsultationService consultationService;
    private final RoomService roomService;
    private final EquipmentService equipmentService;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;
    private final BookingMetrics bookingMetrics;

    // ==================== 查询 ====================

    @Override
    public List<AssistantServiceResponse> findServices(String campus, String category, String keyword) {
        Map<Long, ServiceCategory> categories = categoryIndex();
        List<AssistantServiceResponse> result = new ArrayList<>();
        // 走 ServiceItemService 而非 repository：前者带 @Cacheable("services")，
        // 避免每次助手问答都全表扫 services
        for (ServiceItem s : serviceItemService.getAvailableServices()) {
            ServiceCategory cat = categories.get(s.getCategoryId());
            String categoryCode = cat == null ? null : cat.getCode();
            if (!matchesCampus(s.getCampus(), campus) || !matchesCategory(categoryCode, category)) {
                continue;
            }
            if (keyword != null && !keyword.isBlank() && !containsKeyword(s.getServiceName(), s.getServiceDescribe(), keyword)) {
                continue;
            }
            result.add(toServiceResponse(s, cat));
        }
        return result;
    }

    @Override
    public List<AssistantConsultantResponse> findConsultants(String campus, String keyword, String date) {
        Map<Long, ServiceItem> serviceIndex = serviceIndex();
        LocalDate parsedDate = parseDateOrNull(date);

        // 第一遍：校区/关键词过滤。先收集命中咨询师，再一条 GROUP BY 批量取当日可预约时段数
        // （4.7：此前每命中一位咨询师就发一次 findAvailable，N 位咨询师 N 次 SQL）
        List<Consultant> matched = new ArrayList<>();
        Map<Long, ServiceItem> matchedService = new LinkedHashMap<>();
        for (Consultant c : consultantRepository.findAll()) {
            ServiceItem s = c.getServiceId() == null ? null : serviceIndex.get(c.getServiceId());
            if (s != null && !matchesCampus(s.getCampus(), campus)) {
                continue;
            }
            if (campus != null && !campus.isBlank() && s == null) {
                // 指定了校区但咨询师没有归属服务，无法判定校区，跳过
                continue;
            }
            if (keyword != null && !keyword.isBlank()
                    && !containsKeyword(c.getName(), c.getDepartment(), c.getTitle(), c.getDescription(), keyword)) {
                continue;
            }
            matched.add(c);
            matchedService.put(c.getId(), s);
        }
        Map<Long, Integer> slotCounts = parsedDate == null || matched.isEmpty()
                ? Map.of()
                : timeSlotRepository.countAvailableByConsultants(
                        matched.stream().map(Consultant::getId).toList(), parsedDate);

        List<AssistantConsultantResponse> result = new ArrayList<>();
        for (Consultant c : matched) {
            ServiceItem s = matchedService.get(c.getId());
            Integer slotCount = parsedDate == null ? null : slotCounts.getOrDefault(c.getId(), 0);
            result.add(AssistantConsultantResponse.builder()
                    .consultantId(c.getId())
                    .name(c.getName())
                    .title(c.getTitle())
                    .department(c.getDepartment())
                    .description(c.getDescription())
                    .serviceId(c.getServiceId())
                    .serviceName(s == null ? null : s.getServiceName())
                    .campus(s == null ? null : s.getCampus())
                    .campusName(campusName(s == null ? null : s.getCampus()))
                    .availableSlotCount(slotCount)
                    .build());
        }
        return result;
    }

    @Override
    public List<TimeSlotView> findConsultantSlots(Long consultantId, String date) {
        if (consultantId == null || date == null || date.isBlank()) {
            return Collections.emptyList();
        }
        return consultantRepository.findTimeSlots(consultantId, date);
    }

    @Override
    public List<AssistantRoomResponse> findRooms(String campus, String date, String startTime, String endTime) {
        Map<Long, ServiceItem> serviceIndex = serviceIndex();
        LocalDate parsedDate = parseDateOrNull(date);
        boolean withWindow = parsedDate != null && startTime != null && endTime != null
                && !startTime.isBlank() && !endTime.isBlank() && startTime.compareTo(endTime) < 0;

        // 一次性取全部教室再按服务分组：此前是"遍历服务 → 每个服务查一次教室"的 N+1
        Map<Long, List<Room>> roomsByService = roomRepository.findAll().stream()
                .collect(Collectors.groupingBy(Room::getServiceId, LinkedHashMap::new, Collectors.toList()));

        // 先收集本校区全部待展示（教室, 所属服务），再一条 GROUP BY 批量取窗口占用
        // （4.7：此前每间教室各发一次 countRoomOverlap，N 间教室 N 次 SQL）
        List<Room> candidateRooms = new ArrayList<>();
        Map<Long, ServiceItem> roomService = new LinkedHashMap<>();
        for (ServiceItem s : serviceItemService.getAvailableServices()) {
            if (!matchesCampus(s.getCampus(), campus)) {
                continue;
            }
            for (Room r : roomsByService.getOrDefault(s.getServiceId(), List.of())) {
                candidateRooms.add(r);
                roomService.put(r.getId(), s);
            }
        }
        Map<Long, Integer> roomOverlap = withWindow && !candidateRooms.isEmpty()
                ? bookingRepository.countRoomOverlapBatch(
                        candidateRooms.stream().map(Room::getId).toList(),
                        parsedDate, startTime, endTime)
                : Map.of();

        List<AssistantRoomResponse> result = new ArrayList<>();
        for (Room r : candidateRooms) {
            ServiceItem s = roomService.get(r.getId());
            Boolean free = withWindow ? roomOverlap.getOrDefault(r.getId(), 0) == 0 : null;
            result.add(AssistantRoomResponse.builder()
                    .roomId(r.getId())
                    .name(r.getName())
                    .location(r.getLocation())
                    .seats(r.getSeats())
                    .serviceId(s.getServiceId())
                    .serviceName(s.getServiceName())
                    .campus(s.getCampus())
                    .campusName(campusName(s.getCampus()))
                    .free(free)
                    .build());
        }
        return result;
    }

    @Override
    public List<AssistantEquipmentResponse> findEquipment(String campus, String keyword, String date,
                                                    String startTime, String endTime) {
        Map<Long, ServiceItem> serviceIndex = serviceIndex();
        LocalDate parsedDate = parseDateOrNull(date);
        boolean withWindow = parsedDate != null && startTime != null && endTime != null
                && !startTime.isBlank() && !endTime.isBlank() && startTime.compareTo(endTime) < 0;

        // 第一遍：校区/关键词过滤，收集命中设备，再一条 GROUP BY 批量取窗口占用
        // （4.7：此前每台设备各发一次 sumEquipmentOverlap，N 台设备 N 次 SQL）
        List<Equipment> matched = new ArrayList<>();
        Map<Long, ServiceItem> matchedService = new LinkedHashMap<>();
        for (Equipment e : equipmentRepository.findAll()) {
            ServiceItem s = e.getServiceId() == null ? null : serviceIndex.get(e.getServiceId());
            if (s != null && !matchesCampus(s.getCampus(), campus)) {
                continue;
            }
            if (campus != null && !campus.isBlank() && s == null) {
                continue;
            }
            if (keyword != null && !keyword.isBlank()
                    && !containsKeyword(e.getName(), e.getCategory(), e.getDescription(), keyword)) {
                continue;
            }
            matched.add(e);
            matchedService.put(e.getId(), s);
        }
        Map<Long, Integer> equipmentOccupied = withWindow && !matched.isEmpty()
                ? bookingRepository.sumEquipmentOverlapBatch(
                        matched.stream().map(Equipment::getId).toList(),
                        parsedDate, startTime, endTime)
                : Map.of();

        List<AssistantEquipmentResponse> result = new ArrayList<>();
        for (Equipment e : matched) {
            ServiceItem s = matchedService.get(e.getId());
            Integer remaining = null;
            if (withWindow) {
                int occupied = equipmentOccupied.getOrDefault(e.getId(), 0);
                int stock = e.getAvailableStock() == null ? 0 : e.getAvailableStock();
                remaining = Math.max(stock - occupied, 0);
            }
            result.add(AssistantEquipmentResponse.builder()
                    .equipmentId(e.getId())
                    .name(e.getName())
                    .category(e.getCategory())
                    .description(e.getDescription())
                    .totalStock(e.getTotalStock())
                    .availableStock(e.getAvailableStock())
                    .unit(e.getUnit())
                    .location(e.getLocation())
                    .serviceId(e.getServiceId())
                    .serviceName(s == null ? null : s.getServiceName())
                    .campus(s == null ? null : s.getCampus())
                    .campusName(campusName(s == null ? null : s.getCampus()))
                    .remainingForWindow(remaining)
                    .build());
        }
        return result;
    }

    @Override
    public List<BookingQueryView> findMyBookings(Long userId, Integer manageStatus) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<BookingQueryView> all = bookingRepository.getServiceStatusByUserId(userId);
        if (manageStatus == null) {
            return all;
        }
        return all.stream()
                .filter(b -> manageStatus.equals(b.getManageStatus()))
                .toList();
    }

    // ==================== 两段式预约 ====================

    @Override
    public AssistantBookingDraft createDraft(Long userId, AssistantBookingDraftRequest request) {
        AssistantBookingDraft draft = resolveAndValidate(request);
        // 校验不通过：不分配草稿ID、不写 Redis。
        // 无效草稿本就无法被确认，给它一个 ID 只会误导调用方，并留下永不清理的键
        if (Boolean.FALSE.equals(draft.getValid())) {
            log.info("预约草稿校验不通过: userId={}, reason={}", userId, draft.getInvalidReason());
            return draft;
        }
        String draftId = UUID.randomUUID().toString();
        draft.setDraftId(draftId);
        draft.setUserId(userId);
        draft.setExpiresAt(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(DRAFT_TTL_MINUTES));
        draft.setConfirmPrompt(buildConfirmPrompt(draft));
        saveDraft(draft);
        log.info("预约草稿已生成: userId={}, draftId={}, type={}", userId, draftId, draft.getResourceType());
        return draft;
    }

    @Override
    public AssistantBookingDraft getDraft(Long userId, String draftId) {
        return loadDraft(userId, draftId);
    }

    @Override
    public void discardDraft(Long userId, String draftId) {
        redisUtil.delete(draftKey(userId, draftId));
    }

    @Override
    public AssistantBookingResult confirmDraft(Long userId, String draftId) {
        AssistantBookingDraft stored = loadDraft(userId, draftId);
        // 二次校验：草稿生成后到确认之间，容量 / 时段 / 库存可能已经变化
        AssistantBookingDraft revalidated = resolveAndValidate(toRequest(stored));
        if (Boolean.FALSE.equals(revalidated.getValid())) {
            throw new BusinessException(BookErrorCode.BOOKING_FAILED.getCode(),
                    revalidated.getInvalidReason());
        }

        Long orderId = executeBooking(userId, stored);
        discardDraft(userId, draftId);

        return AssistantBookingResult.builder()
                .orderId(orderId)
                .status(Boolean.TRUE.equals(stored.getNeedAudit()) ? "PENDING" : "APPROVED")
                .statusText(Boolean.TRUE.equals(stored.getNeedAudit()) ? "待审核" : "已通过")
                .message(Boolean.TRUE.equals(stored.getNeedAudit())
                        ? "预约已提交，等待管理员审核：" + stored.getSummary()
                        : "预约成功：" + stored.getSummary())
                .resourceType(stored.getResourceType())
                .resourceName(stored.getResourceName())
                .serviceName(stored.getServiceName())
                .campusName(stored.getCampusName())
                .date(stored.getDate())
                .startTime(stored.getStartTime())
                .endTime(stored.getEndTime())
                .quantity(stored.getQuantity())
                .build();
    }

    @Override
    public AssistantBookingResult cancelBooking(Long userId, Long orderId) {
        BookingQueryView target = bookingRepository.getServiceStatusByOrderIdAndUserId(userId, orderId);
        if (target == null) {
            throw new BusinessException(BookErrorCode.BOOKING_NOT_FOUND);
        }
        boolean ok = bookService.cancelBookings(userId, Collections.singletonList(orderId));
        if (!ok) {
            throw new BusinessException(BookErrorCode.BOOKING_CANCEL_FAILED);
        }
        return AssistantBookingResult.builder()
                .orderId(orderId)
                .status("CANCELLED")
                .statusText("已取消")
                .message("已取消预约：" + target.getServiceName())
                .serviceName(target.getServiceName())
                .date(target.getSlotDate() == null ? null : String.valueOf(target.getSlotDate()))
                .startTime(target.getStartTime())
                .endTime(target.getEndTime())
                .build();
    }

    // ==================== 草稿解析与校验 ====================

    /**
     * 解析请求 → 推断资源类型 → 逐项校验，返回草稿（未分配 draftId）。
     * 校验不通过时 {@code valid=false} 并给出 {@code invalidReason}，不抛异常，
     * 便于 AI 把原因直接转述给用户。
     */
    private AssistantBookingDraft resolveAndValidate(AssistantBookingDraftRequest request) {
        if (request == null || request.getServiceId() == null) {
            return invalid(null, null, "缺少服务ID，无法预约");
        }
        ServiceItem service = serviceRepository.findById(request.getServiceId()).orElse(null);
        if (service == null) {
            return invalid(null, null, "服务不存在");
        }
        if (!service.isAvailable()) {
            return invalid(service, null, "该服务已下架，暂不可预约");
        }
        ServiceCategory category = service.getCategoryId() == null
                ? null : categoryIndex().get(service.getCategoryId());
        String categoryCode = category == null ? null : category.getCode();

        String type = inferResourceType(request);
        return switch (type) {
            case AssistantBookingDraft.TYPE_CONSULTATION -> validateConsultation(service, category, request);
            case AssistantBookingDraft.TYPE_ROOM -> validateRoom(service, category, request);
            case AssistantBookingDraft.TYPE_EQUIPMENT -> validateEquipment(service, category, request);
            default -> validatePlainService(service, category, request);
        };
    }

    private String inferResourceType(AssistantBookingDraftRequest request) {
        if (request.getConsultantId() != null || request.getSlotId() != null) {
            return AssistantBookingDraft.TYPE_CONSULTATION;
        }
        if (request.getRoomId() != null) {
            return AssistantBookingDraft.TYPE_ROOM;
        }
        if (request.getEquipmentId() != null) {
            return AssistantBookingDraft.TYPE_EQUIPMENT;
        }
        return AssistantBookingDraft.TYPE_SERVICE;
    }

    private AssistantBookingDraft validateConsultation(ServiceItem service, ServiceCategory category,
                                                       AssistantBookingDraftRequest request) {
        if (request.getConsultantId() == null || request.getSlotId() == null) {
            return invalid(service, category, "咨询预约需要同时提供咨询师ID与时段ID");
        }
        Consultant consultant = consultantRepository.findById(request.getConsultantId()).orElse(null);
        if (consultant == null) {
            return invalid(service, category, "咨询师不存在");
        }
        if (!service.getServiceId().equals(consultant.getServiceId())) {
            // 先校验"资源归属"再校验"时段占用"：归属错误属于参数串台，必须最早拦下，
            // 否则后续按错误咨询师去判时段会给出误导性的"时段可用/已约"结论
            return invalid(service, category, "该咨询师不属于所选服务，请确认后重试");
        }
        TimeSlot slot = timeSlotRepository.findById(request.getSlotId())
                .filter(s -> s.getConsultantId() != null && s.getConsultantId().equals(consultant.getId()))
                .orElse(null);
        if (slot == null) {
            return invalid(service, category, "时段与咨询师不匹配");
        }
        if (!slot.isAvailable()) {
            return invalid(service, category, "该时段不可用或刚被他人预约，请重新选择");
        }
        AssistantBookingDraft draft = valid(AssistantBookingDraft.TYPE_CONSULTATION, "教师咨询", service, category,
                consultant.getId(), consultant.getName(),
                String.valueOf(slot.getSlotDate()), slot.getStartTime(), slot.getEndTime(),
                null, request.getPurpose(), List.of(), Boolean.TRUE);
        // 时段ID 必须随草稿一起持久化：确认时 toRequest 靠它回填，
        // 否则二次校验拿不到 slotId，咨询预约确认会 100% 失败
        draft.setSlotId(slot.getId());
        return draft;
    }

    private AssistantBookingDraft validateRoom(ServiceItem service, ServiceCategory category,
                                               AssistantBookingDraftRequest request) {
        TimeWindow window = TimeWindow.of(request.getDate(), request.getStartTime(), request.getEndTime());
        if (window.error != null) {
            return invalid(service, category, window.error);
        }
        Room room = roomRepository.findById(request.getRoomId()).orElse(null);
        if (room == null) {
            return invalid(service, category, "教室不存在");
        }
        if (!service.getServiceId().equals(room.getServiceId())) {
            // 先校验归属再查占用：跨服务的教室根本不在本服务资源域内，不应参与时段冲突判定
            return invalid(service, category, "该教室不属于所选服务，请确认后重试");
        }
        if (bookingRepository.countRoomOverlap(room.getId(), window.date, window.start, window.end) > 0) {
            bookingMetrics.recordConflictBlocked(BookingMetrics.REASON_SLOT_UNAVAILABLE);
            return invalid(service, category, "该教室此时间段已被预约，请换教室或时段");
        }
        return valid(AssistantBookingDraft.TYPE_ROOM, "教室空间", service, category,
                room.getId(), room.getName(), request.getDate(), window.start, window.end,
                null, request.getPurpose(), List.of(), Boolean.TRUE);
    }

    private AssistantBookingDraft validateEquipment(ServiceItem service, ServiceCategory category,
                                                    AssistantBookingDraftRequest request) {
        int quantity = request.getQuantity() == null || request.getQuantity() < 1 ? 1 : request.getQuantity();
        TimeWindow window = TimeWindow.of(request.getDate(), request.getStartTime(), request.getEndTime());
        if (window.error != null) {
            return invalid(service, category, window.error);
        }
        Equipment equipment = equipmentRepository.findById(request.getEquipmentId()).orElse(null);
        if (equipment == null) {
            return invalid(service, category, "设备不存在");
        }
        if (!service.getServiceId().equals(equipment.getServiceId())) {
            // 先校验归属再算库存：跨服务设备不在本服务资源域内，其库存与本服务无关
            return invalid(service, category, "该设备不属于所选服务，请确认后重试");
        }
        int stock = equipment.getAvailableStock() == null ? 0 : equipment.getAvailableStock();
        int occupied = bookingRepository.sumEquipmentOverlap(equipment.getId(), window.date, window.start, window.end);
        if (occupied + quantity > stock) {
            return invalid(service, category, String.format(
                    "该时段设备库存不足（可借 %d，已被占用 %d），请减少数量或换时段", stock, occupied));
        }
        List<String> warnings = new ArrayList<>();
        if (stock - occupied - quantity <= 1) {
            warnings.add("该时段设备余量紧张，请尽快确认");
        }
        return valid(AssistantBookingDraft.TYPE_EQUIPMENT, "设备借用", service, category,
                equipment.getId(), equipment.getName(), request.getDate(), window.start, window.end,
                quantity, request.getPurpose(), warnings, Boolean.TRUE);
    }

    private AssistantBookingDraft validatePlainService(ServiceItem service, ServiceCategory category,
                                                       AssistantBookingDraftRequest request) {
        if (!service.hasCapacity()) {
            return invalid(service, category, "该服务预约名额已满，请选择其他服务");
        }
        List<String> warnings = new ArrayList<>();
        if (service.getCapacity() != null && service.getCapacity() > 0 && service.getBookedCount() != null) {
            int remaining = service.getCapacity() - service.getBookedCount();
            if (remaining <= 1) {
                warnings.add("该服务仅剩 " + remaining + " 个名额，请尽快确认");
            }
        }
        // 活动报名免审直通（category.code = activity），其余需人工审核
        boolean needAudit = !CategoryCode.ACTIVITY.is(category == null ? null : category.getCode());
        return valid(AssistantBookingDraft.TYPE_SERVICE,
                category == null ? "校园服务" : category.getName(), service, category,
                service.getServiceId(), service.getServiceName(),
                request.getDate(), request.getStartTime(), request.getEndTime(),
                null, request.getPurpose(), warnings, needAudit);
    }

    // ==================== 下单执行 ====================

    /**
     * 按资源类型派发到既有下单服务，与用户端共用同一套防冲突 / 防超卖逻辑。
     * 返回各下单服务回填的真实 orderId（3.3.1：不再用"查用户最新一单"猜测，
     * 避免并发下拿到他人/他单订单号）。
     */
    private Long executeBooking(Long userId, AssistantBookingDraft draft) {
        switch (draft.getResourceType()) {
            case AssistantBookingDraft.TYPE_CONSULTATION -> {
                ConsultationBookRequest req = new ConsultationBookRequest();
                req.setSlotId(resolveSlotId(draft));
                // 第二个参数是 consultantId 而非 serviceId：草稿的 resourceId 对咨询类型就是咨询师ID
                // （见 validateConsultation 的 valid(...) 入参）。此前误传 serviceId，
                // 会导致查出错误咨询师或直接 CONSULTANT_NOT_FOUND。
                return consultationService.bookConsultation(userId, draft.getResourceId(), req.getSlotId());
            }
            case AssistantBookingDraft.TYPE_ROOM -> {
                RoomBookRequest req = new RoomBookRequest();
                req.setDate(draft.getDate());
                req.setStartTime(draft.getStartTime());
                req.setEndTime(draft.getEndTime());
                return roomService.bookRoom(userId, draft.getResourceId(), req);
            }
            case AssistantBookingDraft.TYPE_EQUIPMENT -> {
                EquipmentBookRequest req = new EquipmentBookRequest();
                req.setQuantity(draft.getQuantity() == null ? 1 : draft.getQuantity());
                req.setDate(draft.getDate());
                req.setStartTime(draft.getStartTime());
                req.setEndTime(draft.getEndTime());
                return equipmentService.bookEquipment(userId, draft.getResourceId(), req);
            }
            default -> {
                // 通用/活动单：助手场景恒为单个 serviceId，取该单回填的 orderId
                return bookService.bookService(userId, Collections.singletonList(draft.getServiceId()))
                        .orderIds().stream().findFirst().orElse(null);
            }
        }
    }

    /** 咨询预约下单只需要 slotId；草稿里存的是咨询师ID，这里反查其可用时段中的同一时段 */
    private Long resolveSlotId(AssistantBookingDraft draft) {
        // 草稿里保存的就是生成草稿时校验通过的时段ID，直接用，无需再反查。
        // 反查仅用于兼容历史草稿（slotId 为空），它依赖日期解析与起止时间字符串精确匹配，
        // 一旦时段被占用或时间格式有出入就会误判为"时段不可用"，因此只作兜底。
        if (draft.getSlotId() != null) {
            return draft.getSlotId();
        }
        LocalDate date = parseDateOrNull(draft.getDate());
        if (date != null) {
            Optional<TimeSlot> matched = timeSlotRepository
                    .findAvailable(draft.getResourceId(), date).stream()
                    .filter(s -> draft.getStartTime() != null && draft.getStartTime().equals(s.getStartTime()))
                    .filter(s -> draft.getEndTime() == null || draft.getEndTime().equals(s.getEndTime()))
                    .findFirst();
            if (matched.isPresent()) {
                return matched.get().getId();
            }
        }
        bookingMetrics.recordConflictBlocked(BookingMetrics.REASON_SLOT_UNAVAILABLE);
        throw new BusinessException(BookErrorCode.SLOT_UNAVAILABLE);
    }

    // ==================== 草稿持久化 ====================

    private void saveDraft(AssistantBookingDraft draft) {
        try {
            redisUtil.set(draftKey(draft.getUserId(), draft.getDraftId()),
                    objectMapper.writeValueAsString(draft), DRAFT_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            throw new BusinessException(BookErrorCode.BOOKING_FAILED.getCode(), "预约草稿生成失败");
        }
    }

    private AssistantBookingDraft loadDraft(Long userId, String draftId) {
        if (userId == null || draftId == null || draftId.isBlank()) {
            throw new BusinessException(BookErrorCode.BOOKING_NOT_FOUND);
        }
        String json = redisUtil.get(draftKey(userId, draftId));
        if (json == null) {
            throw new BusinessException(BookErrorCode.BOOKING_NOT_FOUND.getCode(),
                    "预约草稿已过期，请重新发起预约");
        }
        try {
            return objectMapper.readValue(json, AssistantBookingDraft.class);
        } catch (JsonProcessingException e) {
            log.warn("预约草稿反序列化失败: userId={}, draftId={}", userId, draftId);
            throw new BusinessException(BookErrorCode.BOOKING_NOT_FOUND.getCode(),
                    "预约草稿已失效，请重新发起预约");
        }
    }

    private String draftKey(Long userId, String draftId) {
        return DRAFT_KEY_PREFIX + userId + ":" + draftId;
    }

    private AssistantBookingDraftRequest toRequest(AssistantBookingDraft draft) {
        AssistantBookingDraftRequest req = new AssistantBookingDraftRequest();
        req.setServiceId(draft.getServiceId());
        req.setDate(draft.getDate());
        req.setStartTime(draft.getStartTime());
        req.setEndTime(draft.getEndTime());
        req.setQuantity(draft.getQuantity());
        req.setPurpose(draft.getPurpose());
        switch (draft.getResourceType()) {
            case AssistantBookingDraft.TYPE_CONSULTATION -> {
                req.setConsultantId(draft.getResourceId());
                // 时段ID 必须回填：确认时的二次校验走 validateConsultation，
                // 它要求 consultantId 与 slotId 同时非空，缺一则确认必然失败
                req.setSlotId(draft.getSlotId());
            }
            case AssistantBookingDraft.TYPE_ROOM -> req.setRoomId(draft.getResourceId());
            case AssistantBookingDraft.TYPE_EQUIPMENT -> req.setEquipmentId(draft.getResourceId());
            default -> { /* 通用服务无需资源ID */ }
        }
        return req;
    }

    // ==================== 草稿构建辅助 ====================

    private AssistantBookingDraft valid(String type, String typeName, ServiceItem service, ServiceCategory category,
                                        Long resourceId, String resourceName, String date, String start, String end,
                                        Integer quantity, String purpose, List<String> warnings, Boolean needAudit) {
        String campus = service.getCampus();
        AssistantBookingDraft draft = AssistantBookingDraft.builder()
                .resourceType(type)
                .resourceTypeName(typeName)
                .serviceId(service.getServiceId())
                .serviceName(service.getServiceName())
                .campus(campus)
                .campusName(campusName(campus))
                .categoryCode(category == null ? null : category.getCode())
                .categoryName(category == null ? null : category.getName())
                .resourceId(resourceId)
                .resourceName(resourceName)
                .date(date)
                .startTime(start)
                .endTime(end)
                .quantity(quantity)
                .purpose(purpose)
                .needAudit(needAudit)
                .valid(Boolean.TRUE)
                .warnings(warnings)
                .build();
        draft.setSummary(buildSummary(draft));
        draft.setConfirmPrompt(buildConfirmPrompt(draft));
        return draft;
    }

    private AssistantBookingDraft invalid(ServiceItem service, ServiceCategory category, String reason) {
        return AssistantBookingDraft.builder()
                .valid(Boolean.FALSE)
                .invalidReason(reason)
                .serviceId(service == null ? null : service.getServiceId())
                .serviceName(service == null ? null : service.getServiceName())
                .campus(service == null ? null : service.getCampus())
                .campusName(campusName(service == null ? null : service.getCampus()))
                .categoryCode(category == null ? null : category.getCode())
                .categoryName(category == null ? null : category.getName())
                .warnings(List.of())
                .build();
    }

    /** 人类可读的一行摘要，例如「仓前校区 · 教师咨询 · 张老师 · 2026-09-12 09:00-10:00」 */
    private String buildSummary(AssistantBookingDraft d) {
        StringBuilder sb = new StringBuilder();
        if (d.getCampusName() != null) {
            sb.append(d.getCampusName());
        }
        if (d.getServiceName() != null) {
            sb.append(sb.length() > 0 ? " · " : "").append(d.getServiceName());
        }
        if (d.getResourceName() != null && !d.getResourceName().equals(d.getServiceName())) {
            sb.append(" · ").append(d.getResourceName());
        }
        if (d.getDate() != null) {
            sb.append(" · ").append(d.getDate());
        }
        if (d.getStartTime() != null && d.getEndTime() != null) {
            sb.append(" ").append(d.getStartTime()).append("-").append(d.getEndTime());
        }
        if (d.getQuantity() != null && d.getQuantity() > 1) {
            sb.append(" · ").append(d.getQuantity()).append(" 件");
        }
        return sb.toString();
    }

    private String buildConfirmPrompt(AssistantBookingDraft d) {
        if (Boolean.FALSE.equals(d.getValid())) {
            return d.getInvalidReason();
        }
        StringBuilder sb = new StringBuilder("请确认是否提交以下预约：").append(d.getSummary());
        if (Boolean.TRUE.equals(d.getNeedAudit())) {
            sb.append("（提交后需管理员/教师审核）");
        } else {
            sb.append("（活动类预约提交后即时通过）");
        }
        if (d.getWarnings() != null && !d.getWarnings().isEmpty()) {
            sb.append("。注意：").append(String.join("；", d.getWarnings()));
        }
        return sb.toString();
    }

    // ==================== 通用工具 ====================

    private AssistantServiceResponse toServiceResponse(ServiceItem s, ServiceCategory cat) {
        int booked = s.getBookedCount() == null ? 0 : s.getBookedCount();
        Integer capacity = s.getCapacity();
        Integer remaining = capacity == null || capacity == -1 ? -1 : Math.max(capacity - booked, 0);
        boolean bookable = s.hasCapacity();
        return AssistantServiceResponse.builder()
                .serviceId(s.getServiceId())
                .serviceName(s.getServiceName())
                .serviceDescribe(s.getServiceDescribe())
                .campus(s.getCampus())
                .campusName(campusName(s.getCampus()))
                .categoryCode(cat == null ? null : cat.getCode())
                .categoryName(cat == null ? null : cat.getName())
                .capacity(capacity)
                .bookedCount(booked)
                .remaining(remaining)
                .bookable(bookable)
                .bookableReason(bookable ? null : "名额已满")
                .build();
    }

    private Map<Long, ServiceCategory> categoryIndex() {
        Map<Long, ServiceCategory> index = new LinkedHashMap<>();
        for (ServiceCategory c : serviceCategoryRepository.findAll()) {
            index.put(c.getId(), c);
        }
        return index;
    }

    private Map<Long, ServiceItem> serviceIndex() {
        Map<Long, ServiceItem> index = new LinkedHashMap<>();
        for (ServiceItem s : serviceRepository.findAll()) {
            index.put(s.getServiceId(), s);
        }
        return index;
    }

    private boolean matchesCampus(String campus, String expected) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        return expected.equalsIgnoreCase(campus);
    }

    private boolean matchesCategory(String code, String expected) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        return expected.equalsIgnoreCase(code);
    }

    private boolean containsKeyword(String... texts) {
        if (texts == null || texts.length < 2) {
            return true;
        }
        String keyword = texts[texts.length - 1].toLowerCase(Locale.ROOT);
        for (int i = 0; i < texts.length - 1; i++) {
            if (texts[i] != null && texts[i].toLowerCase(Locale.ROOT).contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String campusName(String campus) {
        if (campus == null) {
            return null;
        }
        return switch (campus.toLowerCase(Locale.ROOT)) {
            case "cq" -> "仓前校区";
            case "xs" -> "下沙校区";
            default -> campus;
        };
    }

    private LocalDate parseDateOrNull(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date);
        } catch (Exception e) {
            return null;
        }
    }

    /** 时间窗口解析结果：error 非空即不合法 */
    private record TimeWindow(LocalDate date, String start, String end, String error) {
        static TimeWindow of(String date, String start, String end) {
            if (date == null || start == null || end == null
                    || date.isBlank() || start.isBlank() || end.isBlank()) {
                return new TimeWindow(null, null, null, "缺少日期或起止时间");
            }
            if (start.compareTo(end) >= 0) {
                return new TimeWindow(null, null, null, "开始时间必须早于结束时间");
            }
            LocalDate parsed;
            try {
                parsed = LocalDate.parse(date);
            } catch (Exception e) {
                return new TimeWindow(null, null, null, "日期格式不正确，应为 yyyy-MM-dd");
            }
            if (parsed.isBefore(LocalDate.now())) {
                return new TimeWindow(null, null, null, "不能预约过去的日期");
            }
            return new TimeWindow(parsed, start, end, null);
        }
    }
}
