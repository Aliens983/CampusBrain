# cas-module-appointment — 预约核心业务模块

DDD 业务模块，承载预约全流程：服务目录、四类预约（咨询/教室/设备/活动）、审核、时段与库存防冲突、自动完成，以及首页轮播图。

## 核心功能
- **服务目录**：按分类（`teacher/equipment/space/activity`）与校区（`cq/xs`）提供服务列表/详情，含封面图 `image_url` 与容量。
- **四类预约**：
  - 咨询：选咨询师 + 可约时段（`time_slot` 落库），时段冲突防重；
  - 教室：选教室 + 时段窗口，**一间教室同一时间段仅一人可约**；
  - 设备：窗口借用 + 数量，`available_stock` 库存原子扣减，到点自动归还；
  - 活动：`capacity` 容量扣减（-1 不限），超额拒绝。
- **审核 / 取消**：通过/拒绝（拒绝必填原因），自动释放时段与库存；`BookingAutoCompleteTask` 定时把已过窗口的单置为「完成」。
- **轮播图**：`carousel` 独立子包（controller/admin + app、service、mapper、dataobject），管理端上传/删除/拖拽排序（≤6 张）+ 用户端启用列表。

## 目录结构
```
com.laoliu.cas.appointment
├── carousel/                    # 轮播图子域
├── interfaces/controller/       # admin（/admin/services /admin/bookings）· app（/app/services /app/bookings …）
├── interfaces/dto/              # request / response
├── application/service/         # 应用编排（impl），含 @EnableScheduling 自动完成任务
├── domain/                      # 纯实体 + repository 接口（零框架注解）
└── infrastructure/              # persistence：dataobject / mapper / repositoryImpl
```

## 主要 REST 分组（经网关前缀 `/api/v1`）
| 路径 | 说明 |
|---|---|
| `GET /app/service-categories` | 固定 4 类分类字典（只读） |
| `GET /app/services`、`/{id}`、`/mine` | 服务目录浏览 |
| `POST /app/bookings`、`GET /app/bookings/{id}`、`GET /app/bookings/mine` | 统一下单 / 详情 / 我的预约（原 `/app/bookings/{room,equipment,consultation}` 三端点已于 2026-09-12 删除，改走各资源专用端点） |
| `GET /app/consultations`、`/{consultantId}/slots`、`GET /app/rooms`、`GET /app/equipment(/categories)` | 资源与可约数据 |
| `/app/chat/consult/conversations/**` | 学生⇄教师 1:1 咨询沟通（列表/未读/打开/消息/已读，参与者鉴权） |
| `GET /teacher/bookings`、`PATCH /teacher/bookings/{id}/approve\|reject` | 教师自审名下咨询档期 |
| `GET /appointments/availability` | 实时余量（供 KB 只读查询，内网签名） |
| `GET /app/carousel` | 用户端轮播列表 |
| `GET/POST/PUT /admin/services`、`GET /admin/bookings` + `PATCH /admin/bookings/{id}/approve\|reject` | 服务治理 / 预约审核 |
| `GET/POST/DELETE /admin/carousel`、`POST /admin/carousel/reorder` | 轮播图管理 |

## 核心数据表（Flyway V1/V4/V5）
`services`（目录：category_id/campus/image_url/capacity/booked_count）+ `service_category`（固定 4 类）→ `item`（预约单：service_id + 资源列其一；`manage_status` 0待审/1通过/2拒绝/3取消/4完成）→ 资源 `consultant`+`time_slot`、`room`、`equipment`；独立 `carousel`；`consult_chat_conversation`+`consult_chat_message`（V5 咨询沟通）。

## 依赖
依赖 `cas-module-system`（用户/角色）、`cas-module-infra`（邮件/文件）；测试 5 个测试类、41 个 `@Test`（预约/审核/教师自审/余量，Mockito）。
