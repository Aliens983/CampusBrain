# AGENTS.md — cas-module-appointment

预约核心业务模块：服务目录（分类/校区）、四类预约（咨询/教室/设备/活动）、审核、教师自审、咨询沟通、时段与库存防冲突、自动完成、轮播图。权威说明见上层 `../CLAUDE.md`。

## 包结构（com.laoliu.cas.appointment）

```
interfaces/
├── controller/
│   ├── admin/    ServiceAdminController(/admin/services) · ServiceStatusAdminController(/admin/bookings)
│   │             CarouselAdminController(/admin/carousel)
│   ├── app/      BookAppController(/app/bookings) · ServiceStatusController(/app/bookings/mine)
│   │             ServiceController(/app/services) · ServiceCategoryController(/app/service-categories)
│   │             ConsultationAppController(/app/consultations) · RoomAppController(/app/rooms)
│   │             EquipmentAppController(/app/equipment) · AvailabilityController(/appointments/*)
│   │             CarouselAppController(/app/carousel) · ConsultChatAppController(/app/chat/consult/**)
│   ├── teacher/  TeacherAuditController(/teacher/bookings)
│   └── assistant/ AppointmentAssistantController(/appointments/assistant/**，内网 HMAC 签名)
└── convert/      ServiceConvert · BookingViewConverter · CarouselConvert（interfaces 不再持有 DTO，2.2）
application/
├── dto/          request/（BookServiceRequest、RoomBookRequest、EquipmentBookRequest、
│   │             ConsultationBookRequest、AuditRequest、TeacherAuditRequest、Service*ReqVO、
│   │             AssistantBookingDraftRequest、CarouselImage record …）
│                 response/（ServiceRespVO、ServiceCategoryRespVO、BookingResponse、ServiceStatusResponse、
│                     ConsultantResponse、RoomResponse、TimeSlotRespVO、BookResultResponse、
│                     ServiceAvailabilityResponse、Assistant*Response/Draft/Result …）
└── service/      Book · ServiceStatus · ServiceItem · ServiceCategory · Consultation · Room ·
                      Equipment · TeacherAudit · ConsultChat · AppointmentAssistant · Carousel（接口 + impl）
domain/          entity：ServiceItem · ServiceCategory · Consultant · TimeSlot ·
                        Room · Equipment · Carousel · ConsultChatConversation · ConsultChatMessage
                 repository：对应 *Repository 接口；view：BookingQueryView 等读模型（2.1）
infrastructure/
├── persistence/ dataobject（ServicesDO/ServiceCategoryDO/ItemDO/ConsultantDO/
│                 TimeSlotDO/RoomDO/EquipmentDO/CarouselDO/ConsultChat*DO）+ mapper + repository/*Impl
├── task/        BookingAutoCompleteTask（@Scheduled 60s）+ AppointmentScheduleConfig
├── mq/          BookingEventPublisher + RabbitMqConfig + AppointmentChangedEvent
│                （发 appointment.changed；显式 DirectExchange + Binding）
└── config/      AppointmentScheduleConfig
```

> 历史上曾存在的「FAKE 硬编码咨询师/设备数据」「重复 ServiceController/ServiceStatusController」
> 「ServiceController 直注 Repository」等问题均已重构修复；本模块无残留假数据。

## 关键接口（网关前缀 /api/v1）

| 分组 | 路径 |
|---|---|
| 服务目录 | `GET /app/services`、`/{id}`、`/mine`；`GET /app/service-categories`；`GET/POST /admin/services`、`PUT /admin/services/{id}` |
| 预约 | `POST /app/bookings`、`GET /app/bookings`、`GET /app/bookings/{id}`、`GET /app/bookings/mine`、`PATCH /app/bookings/{id}/cancel`（原 `/{room,equipment,consultation}` 三端点已于 2026-09-12 删除，见下方核心规则） |
| 资源 | `/app/consultations`（+`/{consultantId}/slots`、`/{consultantId}/book`）、`/app/rooms`（+`/{roomId}/book`）、`/app/equipment`（+`/categories`、`/{id}`、`/{equipmentId}/book`） |
| 审核（管理员） | `GET /admin/bookings`、`PATCH /admin/bookings/{id}/{approve,reject}`（reject 必填原因） |
| 审核（教师） | `GET /teacher/bookings`、`PATCH /teacher/bookings/{id}/{approve,reject}`（仅本人名下咨询） |
| 咨询沟通 | `/app/chat/consult/conversations/**`（列表/unread-count/open-*/messages/已读，参与者鉴权） |
| 余量（给 KB） | `GET /appointments/availability`（内网签名）、`GET /appointments/mine` |
| 轮播图 | `GET /app/carousel`；`GET/POST/DELETE /admin/carousel/{id}`、`POST /admin/carousel/reorder` |

## 核心规则

- **状态机** `manage_status`：0 待审 / 1 通过 / 2 拒绝 / 3 取消 / 4 完成；拒绝必有 reason。
- **防冲突**：咨询/教室 = 时段重叠查询 + 行锁（一间教室同时段唯一）；活动 = capacity/booked_count 原子扣减（-1 不限、容量够直通）。取消/拒绝回补。
- **设备 = 时段重叠动态统计（易误解）**：`equipment.available_stock` 是**静态总台数、不扣减**；占用量由 `sumEquipmentOverlap(设备, 日期, 起止时段)` 统计，借用时校验 `已占用 + 本次数量 ≤ available_stock` 并对设备行加锁。
- **设备借用必须走专用端点**：`POST /app/bookings`（通用下单）拒绝 `equipment` 类服务，返回 `EQUIPMENT_REQUIRE_DEDICATED_API(40020)`。通用下单不携带 equipmentId/时段/数量，会造成时段占用统计不到而超借。
- **幂等**：下单 60s 窗口同用户同服务去重，重复返回 BOOKING_REPEATED。
- **自动完成**：BookingAutoCompleteTask 60s 轮询，窗口过期置 COMPLETED。
- **事件**：预约创建/取消后 BookingEventPublisher 发 RabbitMQ `appointment.changed`。**拓扑（2026-09-12 起）**：显式 `DirectExchange cas.appointment.exchange` + Binding，取代默认 exchange 的隐式绑定；队列名仍为 `appointment.changed`；消息体为 `AppointmentChangedEvent` 经 ObjectMapper 序列化的 JSON，**不再手工拼接字符串**。KB 侧 `AppointmentEventConfig` 的 EXCHANGE/QUEUE/ROUTING_KEY 三个常量须与此处一致。

## 数据表

Flyway：`services`（category_id/campus/image_url/capacity/booked_count）、`service_category`（固定 4 类）、
`consultant`、`time_slot`、`room`、`equipment`、`item`、`carousel`、
`consult_chat_conversation`、`consult_chat_message`（V5）。外键均为代码级，不建 DB FK。

## 测试

11 个测试类 / 83 个 `@Test`：BookServiceImplTest 18、AppointmentAssistantServiceImplTest 16、
ServiceStatusServiceImplTest 16、CarouselServiceImplTest 7（1.5/2.10/2.11）、ServiceItemServiceImplTest 8、
TeacherAuditServiceImplTest 5、AvailabilityControllerTest 3、ConsultChatServiceImplTest 3、
BookingEventPublisherTest 4、BookingAutoCompleteTaskTest 2、DomainLayerBoundaryTest 1（domain 反向依赖字节码守卫，2.1/2.2）。

## 依赖

编译期仅依赖 `cas-module-system-api`（UserInfoApi/NotificationSettingsApi）与
`cas-module-infra-api`（FileService/EmailService）契约 artifact + `cas-framework`（2.3）；
system/infra 的实现只在 cas-server 运行时装配，本模块不依赖其他业务模块实现。
