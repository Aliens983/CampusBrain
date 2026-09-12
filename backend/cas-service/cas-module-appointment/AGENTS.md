# AGENTS.md — cas-module-appointment

预约核心业务模块：服务目录（分类/校区）、四类预约（咨询/教室/设备/活动）、审核、教师自审、咨询沟通、时段与库存防冲突、自动完成、轮播图。权威说明见上层 `../CLAUDE.md`。

## 包结构（com.laoliu.cas.appointment）

```
interfaces/
├── controller/
│   ├── admin/    ServiceAdminController(/admin/services) · ServiceStatusAdminController(/admin/bookings)
│   ├── app/      BookAppController(/app/bookings) · ServiceStatusController(/app/bookings/mine)
│   │             ServiceController(/app/services) · ServiceCategoryController(/app/service-categories)
│   │             ConsultationAppController(/app/consultations) · RoomAppController(/app/rooms)
│   │             EquipmentAppController(/app/equipment) · AvailabilityController(/appointments/*)
│   │             ConsultChatAppController(/app/chat/consult/**)
│   └── teacher/  TeacherAuditController(/teacher/bookings)
├── dto/          request/（BookServiceRequest、RoomBookRequest、EquipmentBookRequest、
│                     ConsultationBookRequest、AuditRequest、TeacherAuditRequest、Service*ReqVO …）
│                 response/（ServiceRespVO、ServiceCategoryRespVO、BookingDTO、ServiceStatusResponse、
│                     ConsultantResponse、RoomResponse、TimeSlotRespVO、BookResultResponse、
│                     ServiceAvailabilityVO、UserServicesRespVO …）
└── convert/      ServiceConvert
application/service/  Book · ServiceStatus · Service · ServiceCategory · Consultation · Room ·
                      Equipment · TeacherAudit · ConsultChat（接口 + impl）
domain/          entity：Service · ServiceCategory · AppointmentRecord · Consultant · TimeSlot ·
                        Room · Equipment · ConsultChatConversation · ConsultChatMessage
                 repository：对应 *Repository 接口
infrastructure/
├── persistence/ dataobject（ServicesDO/ServiceCategoryDO/ItemDO/AppointmentRecordDO/ConsultantDO/
│                 TimeSlotDO/RoomDO/EquipmentDO/ConsultChat*DO）+ mapper + repository/*Impl
├── task/        BookingAutoCompleteTask（@Scheduled 60s）+ AppointmentScheduleConfig
├── mq/          BookingEventPublisher + RabbitMqConfig（发 appointment.changed）
└── config/      AppointmentScheduleConfig
carousel/        独立子域：controller(CarouselAdminController/CarouselAppController) · service ·
                 mapper · dataobject（轮播图，未严格四层）
```

> 历史上曾存在的「FAKE 硬编码咨询师/设备数据」「重复 ServiceController/ServiceStatusController」
> 「ServiceController 直注 Repository」等问题均已重构修复；本模块无残留假数据。

## 关键接口（网关前缀 /api/v1）

| 分组 | 路径 |
|---|---|
| 服务目录 | `GET /app/services`、`/{id}`、`/mine`；`GET /app/service-categories`；`GET/POST /admin/services`、`PUT /admin/services/{id}` |
| 预约 | `POST /app/bookings`、`POST /app/bookings/{room,equipment,consultation}`、`GET /app/bookings`、`GET /app/bookings/{id}`、`GET /app/bookings/mine` |
| 资源 | `/app/consultations`（+`/{consultantId}/slots`、`/{consultantId}/book`）、`/app/rooms`（+`/{roomId}/book`）、`/app/equipment`（+`/categories`、`/{id}`、`/{equipmentId}/book`） |
| 审核（管理员） | `GET /admin/bookings`、`PATCH /admin/bookings/{id}/{approve,reject}`（reject 必填原因） |
| 审核（教师） | `GET /teacher/bookings`、`PATCH /teacher/bookings/{id}/{approve,reject}`（仅本人名下咨询） |
| 咨询沟通 | `/app/chat/consult/conversations/**`（列表/unread-count/open-*/messages/已读，参与者鉴权） |
| 余量（给 KB） | `GET /appointments/availability`（内网签名）、`GET /appointments/mine` |
| 轮播图 | `GET /app/carousel`；`GET/POST/DELETE /admin/carousel/{id}`、`POST /admin/carousel/reorder` |

## 核心规则

- **状态机** `manage_status`：0 待审 / 1 通过 / 2 拒绝 / 3 取消 / 4 完成；拒绝必有 reason。
- **防冲突**：咨询/教室 = 时段重叠查询 + 行锁（一间教室同时段唯一）；设备 = available_stock 原子扣减；活动 = capacity/booked_count 原子扣减（-1 不限、容量够直通）。取消/拒绝回补。
- **幂等**：下单 60s 窗口同用户同服务去重，重复返回 BOOKING_REPEATED。
- **自动完成**：BookingAutoCompleteTask 60s 轮询，窗口过期置 COMPLETED。
- **事件**：预约创建/取消后 BookingEventPublisher 发 RabbitMQ `appointment.changed`。

## 数据表

Flyway：`services`（category_id/campus/image_url/capacity/booked_count）、`service_category`（固定 4 类）、
`consultant`、`time_slot`、`room`、`equipment`、`item`、`carousel`、
`consult_chat_conversation`、`consult_chat_message`（V5）。外键均为代码级，不建 DB FK。

## 测试

5 个测试类 / 41 个 `@Test`：BookServiceImplTest 15、ServiceStatusServiceImplTest 10、
ServiceServiceImplTest 8、TeacherAuditServiceImplTest 5、AvailabilityControllerTest 3。

## 依赖

依赖 `cas-module-system`（UserInfoApi）、`cas-module-infra`（Email/File）、`cas-framework`；不依赖其他业务模块。
