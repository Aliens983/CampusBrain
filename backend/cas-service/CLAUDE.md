# cas-service — AI 助手工作指南（CLAUDE.md）

> 本文件是 AI 编码助手在 cas-service（校园预约系统）内工作的权威指南。最后核对：**2026-09-11**，与当前代码一致。
> 整体微服务架构（gateway / cas-service / kb-service）见 `../README.md`；面向人的模块说明见 `README.md`。

## Quick Reference

```
端口:        18080，server.servlet.context-path=/api/v1
服务名:      cas-service（Nacos 注册；配置中心 data-id: cas-service.yaml）
Java:        17       Spring Boot: 3.3.5       Spring Cloud Alibaba: 2023.0.1.2
ORM:         MyBatis-Plus 3.5.5
DB:          MySQL 8，库 cas_db（本地开发用宿主机 3306；compose 全栈用 cas-mysql）
Cache:       Redis（本地 6379 db0）：验证码 / 邮箱限频
MQ:          RabbitMQ：发布 appointment.changed
迁移:        Flyway（classpath:db/migration，V1~V5）
API 文档:    Knife4j → http://localhost:18080/api/v1/doc.html（经网关放行）
Group/包:    com.laoliu / com.laoliu.cas
构建运行:    mvn -pl cas-service/cas-server -am package -DskipTests
             java -jar cas-server/target/cas-server-1.0.0.jar（无 mvnw，用系统 mvn）
测试:        mvn -B -pl cas-service -am test  → 82 个 @Test（13 个测试类）
密钥:        全部 ${ENV_VAR} 注入（application.yml 仅内置本地示例默认值，生产必须覆盖）
```

## 微服务中的位置

```
frontend → gateway:8888（JWT 验签 + 身份头透传）
             ├─ /api/v1/**（除 /kb）→ cas-service:18080（context-path 也是 /api/v1，不剥前缀）
             └─ /api/v1/kb/**       → kb-service:8081（StripPrefix=2）
kb-service ──Feign + Nacos + X-Internal-Sign──> GET /appointments/availability（cas 只读接口）
cas-service ──RabbitMQ appointment.changed──> kb-service（KB 当前仅记日志）
```

- JWT 由 gateway 统一验签；cas 信任网关注入的身份头。`InternalAuthFilter`（cas-spring-boot-starter-security）校验服务间请求的 `X-Internal-Sign`（HMAC + 时间戳新鲜度），通过后写入 SecurityContext。
- 放行路径在 `SecurityAutoConfiguration` 配置（auth/captcha/swagger/error/uploads 等）。

## Maven 模块依赖图（自底向上）

```
cas-dependencies（BOM，统一第三方版本）
cas-framework（聚合）
  ├─ cas-common                     共享内核：Result/异常/错误码/枚举/JWT 工具/安全工具/@RequireRole
  ├─ cas-spring-boot-starter-web    GlobalExceptionHandler、Web 配置
  ├─ cas-spring-boot-starter-security  JWTFilter + InternalAuthFilter + SecurityAutoConfiguration
  ├─ cas-spring-boot-starter-mybatis  MyBatis-Plus 配置
  ├─ cas-spring-boot-starter-redis    RedisTemplate + RedisUtil
  ├─ cas-spring-boot-starter-mq       MqAutoConfiguration（RabbitMQ 基础配置，非空 stub）
  └─ cas-spring-boot-starter-test     BaseApplicationTest
cas-module-infra        文件 / 邮件 / 二维码（依赖 framework + thirdparty 的 OSSService）
cas-thirdparty          天气、阿里云 OSS、短信（仅依赖 common；AI 对话链已删除，见下）
cas-module-system       用户 / 认证 / 角色 / 通知策略（依赖 infra + thirdparty）
cas-module-appointment  预约核心（依赖 system + infra）
cas-server              启动入口：CampusAppointmentApplication + application.yml + Flyway + Demo 控制器
```

硬约束：`server` 零业务代码；业务模块互不直接依赖（跨模块走 `api/`）；thirdparty 不依赖业务模块。

## DDD 分层（每个业务模块一致）

```
interfaces/   controller/{admin,app,teacher} + dto/{request,response} + convert|assembler
application/  service + impl（编排，不直接碰 MyBatis）
domain/       entity（纯 POJO，无 Spring 注解）+ repository（接口）
infrastructure/ persistence/{dataobject,mapper,repository/*Impl} + task/mq/config/aspect
api/          跨模块对外接口 XxxApi + XxxApiImpl + dto（仅 system 模块提供）
carousel/     appointment 内的独立子域包（controller/service/mapper/dataobject，未严格四层）
```

预约模块另有扁平化子域包：`carousel/`（轮播图）；咨询沟通类放在标准四层（ConsultChat* 系列）。

## 当前各模块真实内容

### cas-module-appointment（最大模块）
- **服务目录**：ServiceController `GET /app/services`、`/{id}`、`/mine`；ServiceAdminController `/admin/services`（GET 分页 / POST 新增 / PUT `/{id}` / GET `/by-user`）；ServiceCategoryController `GET /app/service-categories`（固定 4 类字典，配套 ServiceCategory* 全套）。
- **预约**：BookAppController `/app/bookings`（POST 统一下单、GET 列表、GET `/{id}`、POST `/room|/equipment|/consultation`）；ServiceStatusController `GET /app/bookings/mine`；ServiceStatusAdminController `/admin/bookings`（GET 分页 + `PATCH /{id}/approve|reject`，拒绝必填原因）。
- **资源**：ConsultationAppController `/app/consultations`（列表 / `{id}` / `{consultantId}/slots` / `{consultantId}/book`）；RoomAppController `/app/rooms`（GET、POST `/{roomId}/book`）；EquipmentAppController `/app/equipment`（GET、`/categories`、`/{id}`、`/{equipmentId}/book`）。
- **教师端**：TeacherAuditController `/teacher/bookings`（GET 我名下申请、`PATCH /{id}/approve|reject`），Service/Impl + TeacherAuditRequest。
- **咨询沟通**：ConsultChatAppController `/app/chat/consult/conversations/**`（列表、unread-count、open-with-consultant/open-with-student/open-by-booking、消息按 afterId 增量拉取、发消息、已读），参与者本人鉴权。
- **余量（给 KB）**：AvailabilityController `GET /appointments/availability`（内网签名）+ `/appointments/mine`。
- **轮播图**：`carousel/` 子包 CarouselAdminController（/admin/carousel，GET/POST/DELETE/{id}/reorder）、CarouselAppController（GET /app/carousel）。
- **领域实体**：Service、ServiceCategory、AppointmentRecord、Consultant、TimeSlot、Room、Equipment、ConsultChatConversation、ConsultChatMessage。
- **定时/MQ**：infrastructure/task/BookingAutoCompleteTask（60s 扫描过期置 COMPLETED）+ AppointmentScheduleConfig；infrastructure/mq/BookingEventPublisher + RabbitMqConfig（发 appointment.changed）。

### cas-module-system
- controller/app：LoginController（`POST /auth/login`、`/auth/reset`）、RegisterController（`POST /auth/register`）、EmailController（`POST /auth/verification-code`）、GraphicController（`GET /captcha`）。
- controller/admin：UserController（`@RequestMapping("/users")`：`/`、`/me`、`/list`、POST、PUT `/me`、`/me/notify` GET/PUT、PUT `/password`、GET `/me/bookings`）、RoleAdminController（`/admin/users/role` GET/PUT）、NotifyPolicyAdminController（`/admin/settings/notify` GET/PUT）、EmailAdminController（`POST /admin/email`）。
- aspect/RoleAspect：**已修复**——权限不足抛 `ForbiddenException`/`UnauthorizedException`，由 GlobalExceptionHandler 统一返回；超管放行全部，TEACHER 可访问开放给 USER 的接口，教师专属接口须显式列 TEACHER。
- api：UserInfoApi（供 appointment 取用户信息）、GetUserIdViaTokenApi。
- 另有 NotificationPolicy*（全局策略 + 用户偏好）、BookingRecord*（我的预约视图）。

### cas-module-infra
- FileAdminController `POST /admin/files`（本地上传，绝对路径 transferTo，支持子目录 uuid 命名）、OSSAdminController `POST /admin/files/oss`；QRCodeAppController `GET /app/qr-code`。
- FileService / EmailService（@Async，JavaMail 465 SSL）/ QRCodeService（Hutool → OSSService）。无 domain 层。

### cas-thirdparty（注意：AI 对话链已整体删除）
- 现存：WeatherController（`GET /weather`、`/weather/local`）、WeatherApi(Impl)、OSSService(Impl)、SmsService(Impl)、AliyunConfig、OSSConfig，infrastructure/config 下 **QwenConfig / DeepSeekConfig 为无注入的孤儿配置类**。
- 已于 **2026-09-07 下线删除**：CallTheModelController、CallModelService(Impl)、ChatReqVO/RespVO、AiChatHistory 实体/DO/Mapper/Repository、`ai_chat_history` 表（Flyway V1 已同步裁剪）。不要再恢复或引用 `/ai/chat`、`/callTheLargeModel`。AI 对话唯一存储在 kb-service 的 conversation 表。

### cas-server
- CampusAppointmentApplication（@SpringBootApplication + @MapperScan("com.laoliu.cas.**.mapper") + @ComponentScan("com.laoliu.cas")）。
- DbResetConfig：仅当环境变量 `APP_DB_RESET_ON_STARTUP=true`（compose 演示模式）时启动 clean+migrate。
- controller/ConfigDemoController（`GET /config-demo/greeting`，Nacos 热更新演示）、SentinelDemoController（`GET /sentinel-demo/limited`）。
- resources：application.yml（无 application.yml.example，无明文密钥，全部环境变量 + 本地默认值）、db/migration/V1~V5。

## 数据库（Flyway V1~V5，全新机器零手工 SQL）

| 表 | 要点 |
|---|---|
| `user` | role：0 普通用户 / 1 管理员 / 2 超管 / **3 教师**；email_notify 偏好 |
| `notification_policy` | 全局单行，邮件通道开关 |
| `service_category` | 固定 4 类（教师咨询/设备借用/教室空间/活动报名），不可在管理端增删改 |
| `services` | category_id（代码级外键）、campus(cq/xs)、image_url、capacity(-1 不限)、booked_count |
| `consultant` / `time_slot` | 咨询师挂校区服务；可约时段落库（非硬编码） |
| `room` | 教室 + 校区；同间同时段唯一 |
| `equipment` | total_stock / available_stock / unit / location |
| `item` | 预约单：service_id + 资源列其一（consultant_id/slot_id… 或 room_id… 或 equipment_id/quantity）；manage_status 0待审/1通过/2拒绝/3取消/4完成；reason |
| `carousel` | image_url / sort / enabled（≤6） |
| `consult_chat_conversation` / `consult_chat_message` | V5 新增，学生⇄教师 1:1 唯一会话；read_flag 未读 |

种子：V1 两校区服务/咨询师/教室/设备/轮播图/分类；V2 admin@campus.com、user@campus.com（密码 123456）；V3 教师账号 + 咨询师回填。

**迁移约定**：V*.sql 只面向全新库做增量建表/种子，不写 ALTER 既有表；已上线库的结构演进直接对库执行 SQL，参考 `UPGRADE-service-category.md`、`UPGRADE-teacher-role.md`。禁止改写历史 V 文件（checksum）。`sql/` 目录是演进/参考脚本，不参与 Flyway。

## 关键业务规则

- **防冲突**：咨询/教室按时段重叠查询 + 行级锁；设备 `available_stock`、活动 `capacity/booked_count` 原子扣减；取消/拒绝回补。活动容量够即直通成功（无人工审核）。
- **幂等**：下单 SQL 60s 窗口同用户同服务去重，重复返回 `BOOKING_REPEATED`。
- **审核邮件**：全局策略 + 用户 `email_notify` 双开关；拒绝必填原因。
- **统一返回**：`CommonResult<T>`（cas-common.result）；业务错误抛 `BusinessException(ErrorCode)`，由 starter-web 的 GlobalExceptionHandler 兜底，禁止 Controller 手写 JSON / try-catch。
- **错误码**：`*ErrorCode` 接口（Book/Common/Email/Login/Role/Service/ServiceStatus/User/Chat），历史上 HTTP 码与领域码混用，新增时向领域码靠拢。

## 编码约定

Do：Controller 返回 `CommonResult<T>`；用 `SecurityFrameworkUtils` 取当前用户；用 `@RequireRole` 鉴权；跨模块走 `api/`；domain 保持纯 Java + Lombok；Mapper XML 放 `classpath*:/mapper/**/*.xml`；新密钥走环境变量并在 `.env.example` 登记。

Don't：cas-server/cas-common 写业务；跨模块直接注入 Mapper；注入 HttpServletRequest 取用户；`CommonResult.error("字符串")`；domain 加 Spring 注解；application.yml 放真实凭据；恢复已下线的 Qwen AI 链。

## 测试现状

82 个 `@Test` / 13 个测试类（JUnit5 + Mockito，纯单元测试，不起 Docker）：
- appointment（41）：BookServiceImplTest 15、ServiceStatusServiceImplTest 10、ServiceServiceImplTest 8、TeacherAuditServiceImplTest 5、AvailabilityControllerTest 3。
- system（30）：AuthServiceTest 15、RoleServiceImplTest 10、EmailVerificationServiceImplTest 3、UserServiceImplTest 2。
- infra（5）：QRCodeServiceImplTest 3、EmailServiceImplTest 2。
- thirdparty（6）：WeatherApiImplTest 4、SmsServiceImplTest 2。

## 仍存在的已知限制（真实，未修）

1. Error code 口径不统一（HTTP 码 / 领域码混用）。
2. CAS 用 `CommonResult`，KB 用 `ApiResponse`，跨服务响应模型未统一（牵涉前端契约）。
3. Maven groupId 不统一（com.laoliu vs com.kb）。
4. DTO 命名混用（*ReqVO / *Request / *DTO / *RespVO）。
5. `QwenConfig` / `DeepSeekConfig` 是孤儿配置类，可删。
6. Sentinel 已接入但 Nacos 流控规则为空，预留生产调优。
7. KB 侧消费 appointment.changed 仅记日志，索引更新 TODO（属 kb-service）。
