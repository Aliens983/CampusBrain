# cas-service — AI 助手工作指南（CLAUDE.md）

> 本文件是 AI 编码助手在 cas-service（校园预约系统）内工作的权威指南。最后核对：**2026-09-12**（含当日僵尸端点移除、登录验证码、设备库存口径、孤儿配置清理、CORS 白名单五项修复），与当前代码一致。
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
迁移:        Flyway（classpath:db/migration，V1~V8）
API 文档:    Knife4j → http://localhost:18080/api/v1/doc.html（经网关放行）
Group/包:    com.laoliu / com.laoliu.cas
构建运行:    mvn -pl cas-service/cas-server -am package -DskipTests
             java -jar cas-server/target/cas-server-1.0.0.jar（无 mvnw，用系统 mvn）
测试:        cd cas-service && mvn -B test → 170 个 @Test（24 个测试类；cas-server 含 14 个需外部 MySQL 的 V8 IT）
             ⚠ 用 -pl cas-service -am 只会构建聚合 pom、不跑子模块测试，必须进 cas-service 目录跑
密钥:        全部 ${ENV_VAR} 注入（application.yml 仅内置本地示例默认值，生产必须覆盖）
```

## 微服务中的位置

```
frontend → gateway:8888（JWT 验签 + 身份头透传）
             ├─ /api/v1/**（除 /kb）→ cas-service:18080（context-path 也是 /api/v1，不剥前缀）
             └─ /api/v1/kb/**       → kb-service:8081（context-path 也是 /api/v1/kb，不剥前缀）
kb-service ──Feign + Nacos + X-Internal-Sign──> GET /appointments/availability（cas 只读接口）
cas-service ──RabbitMQ appointment.changed──> kb-service（消费后联动失效问答/语义缓存）
```

- JWT 由 gateway 统一验签；cas 信任网关注入的身份头。`InternalAuthFilter`（cas-spring-boot-starter-security）校验服务间请求的 `X-Internal-Sign`（HMAC + 时间戳新鲜度），通过后写入 SecurityContext。
- 放行路径在 `SecurityAutoConfiguration` 配置（auth/captcha/swagger/error/uploads 等）。
- **CORS 统一在网关 globalcors 处理**（4.1.8），业务服务不再注册 CorsFilter：`WebAutoConfiguration` 中只留有说明注释，预检 OPTIONS 由网关直接应答，业务流量只来自网关内网转发。如需调整跨域白名单，改 gateway 配置而非本服务。

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
cas-module-infra-api    跨模块契约（FileService / EmailService），无实现、零框架依赖（仅 spring-web 签名）
cas-module-infra        文件 / 邮件 / 二维码（依赖 infra-api + framework + thirdparty 的 OSSService）
cas-thirdparty          天气、阿里云 OSS、短信（仅依赖 common；AI 对话链已删除，见下）
cas-module-system-api   跨模块契约（UserInfoApi / NotificationSettingsApi + UserInfoDTO），零业务依赖
cas-module-system       用户 / 认证 / 角色 / 通知策略（依赖 system-api + infra-api + thirdparty）
cas-module-appointment  预约核心（仅依赖 system-api + infra-api 契约，编译期不依赖两模块实现）
cas-server              启动入口：CampusAppointmentApplication + application.yml + Flyway
```

硬约束：`server` 零业务代码；业务模块**编译期只依赖对方的 *-api 契约 artifact**（2.3），实现装配发生在 cas-server；跨模块调用走 `XxxApi` 接口；thirdparty 不依赖业务模块。

## DDD 分层（每个业务模块一致）

```
interfaces/   controller/{admin,app,teacher,assistant} + convert（不再放 DTO）
application/  service + impl（编排，不直接碰 MyBatis）+ dto/{request,response}（2.2 后应用层契约统一定义于此，interfaces 直接复用）
domain/       entity（纯 POJO，无 Spring 注解）+ repository（接口）+ view（读模型）
infrastructure/ persistence/{dataobject,mapper,repository/*Impl} + task/mq/config/aspect
跨模块 api    XxxApi + DTO 独立为 cas-module-system-api / cas-module-infra-api 两个零实现 artifact（2.3）
```

domain 层禁止引用 interfaces/application 包，由 `DomainLayerBoundaryTest` 字节码扫描守护（2.1/2.2）。

所有业务子域（含轮播图 carousel、AI 预约助手 assistant）均已统一到上述四层包结构，不再允许新建扁平子域包。assistant 的控制器位于 `interfaces/controller/assistant/`，DTO 位于 `application/dto/`，服务位于 `application/service(+impl)`；carousel 经 domain 实体 `Carousel` + `CarouselRepository` 仓储接入。

### 授权约定（2.3.2，硬约束）

- **路径前缀不承载权限语义**：`/admin`、`/teacher`、`/app` 只表示接口分组，Spring Security 不再对 `/admin/**` 配置任何角色规则。
- **授权唯一来源**是方法级 `@RequireRole`（`RoleAspect` 每次请求实时查库判定，因此角色调整立即生效，不依赖 JWT 内陈旧 claim）；新端点必须显式标注。
- 守护测试 `AdminEndpointAuthorizationGuardTest`（cas-server）扫描全部控制器，凡类级路径以 `/admin` 开头的 HTTP 映射方法缺注解即构建失败。
- 例外：`/appointments/**`（含 assistant 内网接口）整体由 `ROLE_INTERNAL`（HMAC 签名）保护，不走用户角色体系（4.9：matcher 已由 `/appointments/assistant/**` 收严为整个前缀，该前缀仅 AvailabilityController + AppointmentAssistantController 暴露）。

## 当前各模块真实内容

### cas-module-appointment（最大模块）
- **服务目录**：ServiceController `GET /app/services`、`/{id}`、`/mine`；ServiceAdminController `/admin/services`（GET 分页 / POST 新增 / PUT `/{id}` / GET `/by-user`）；ServiceCategoryController `GET /app/service-categories`（固定 4 类字典，配套 ServiceCategory* 全套）。
- **预约**：BookAppController `/app/bookings`（POST 统一下单、GET 列表、GET `/{id}`、`PATCH /{id}/cancel`）；ServiceStatusController `GET /app/bookings/mine`；ServiceStatusAdminController `/admin/bookings`（GET 分页 + `PATCH /{id}/approve|reject`，拒绝必填原因）。
- ⚠ 原 `POST /app/bookings/{room,equipment,consultation}` 三端点已于 **2026-09-12 删除**（把 roomId/equipmentId/consultantId 误当 serviceId，且丢弃 date/startTime/endTime/purpose）。资源预约走各自专用端点：教室 `POST /app/rooms/{roomId}/book`、设备 `POST /app/equipment/{equipmentId}/book`、咨询 `POST /app/consultations/{consultantId}/book`。
- **资源**：ConsultationAppController `/app/consultations`（列表 / `{id}` / `{consultantId}/slots` / `{consultantId}/book`）；RoomAppController `/app/rooms`（GET、POST `/{roomId}/book`）；EquipmentAppController `/app/equipment`（GET、`/categories`、`/{id}`、`/{equipmentId}/book`）。
- **教师端**：TeacherAuditController `/teacher/bookings`（GET 我名下申请、`PATCH /{id}/approve|reject`），Service/Impl + TeacherAuditRequest。
- **咨询沟通**：ConsultChatAppController `/app/chat/consult/conversations/**`（列表、unread-count、open-with-consultant/open-with-student/open-by-booking、消息按 afterId 增量拉取、发消息、已读），参与者本人鉴权。
- **余量（给 KB）**：AvailabilityController `GET /appointments/availability`（内网签名）+ `/appointments/mine`。
- **轮播图**（四层包，domain 实体 Carousel + CarouselRepository）：CarouselAdminController（/admin/carousel，GET/POST/DELETE/{id}/reorder）、CarouselAppController（GET /app/carousel）。
- **AI 预约助手**（四层包：`interfaces/controller/assistant` + `application/service` + `application/dto`）：AppointmentAssistantController（`/appointments/assistant/**`，内网签名供 KB 调用）
  - 查询：`GET /services?campus=&category=&keyword=`、`/consultants?campus=&keyword=&date=`、`/consultants/{id}/slots?date=`、`/rooms?campus=&date=&startTime=&endTime=`、`/equipment?campus=&keyword=&date=&startTime=&endTime=`、`/my-bookings?manageStatus=`
  - 预约（两段式）：`POST /bookings/draft`（只校验预览，草稿存 Redis TTL 10min，key 按 userId 隔离）→ `POST /bookings/{draftId}/confirm`（二次校验后通过 `ConsultationService` / `RoomService` / `EquipmentService` / `BookService` 接口下单）；`GET|DELETE /bookings/draft/{draftId}`、`POST /bookings/{orderId}/cancel`
  - 校验不通过时返回 `valid=false` + `invalidReason`（不抛异常），便于 AI 直接转述给用户。
- **领域实体**：ServiceItem、ServiceCategory、Consultant、TimeSlot、Room、Equipment、Carousel、ConsultChatConversation、ConsultChatMessage（预约单 item 表不建领域实体，经 ItemDO + domain/view 的 BookingQueryView 读写）。
- **定时/MQ**：infrastructure/task/BookingAutoCompleteTask（60s 扫描过期置 COMPLETED）+ AppointmentScheduleConfig；infrastructure/mq/BookingEventPublisher + RabbitMqConfig + AppointmentChangedEvent（发 appointment.changed）。
- **MQ 拓扑（2026-09-12 改造）**：显式声明 `DirectExchange cas.appointment.exchange` + Binding，不再依赖默认 exchange 的隐式绑定；队列名沿用 `appointment.changed` 以免存量消息丢失。消息体为 `AppointmentChangedEvent` 经 ObjectMapper 序列化的 JSON（取代手工拼接字符串，后者无转义、易产出非法 JSON），序列化失败只记日志、不影响预约主流程。**KB 侧 `AppointmentEventConfig` 的同名常量需与此处同步。**

### cas-module-system
- controller/app：LoginController（`POST /auth/login`、`/auth/reset`）、RegisterController（`POST /auth/register`）、EmailController（`POST /auth/verification-code`）、GraphicController（`GET /captcha`）。
- **登录强制校验图形验证码**（2026-09-12 起）：`POST /auth/login` 必须传 `captchaUuid` + `captchaCode`，验证码**一次性**（`CaptchaService.validateCaptcha` 取出即删）。同一账号连续失败 **5 次锁定 15 分钟**（Redis `login:fail:{email}` 原子 INCR），登录成功清零。⚠ 前后端需同时发布，否则全员无法登录。
- controller/admin：UserController（`@RequestMapping("/users")`：`/`、`/me`、`/list`、POST、PUT `/me`、`/me/notify` GET/PUT、PUT `/password`、GET `/me/bookings`）、RoleAdminController（`/admin/users/role` GET/PUT）、NotifyPolicyAdminController（`/admin/settings/notify` GET/PUT）、EmailAdminController（`POST /admin/email`）。
- aspect/RoleAspect：**已修复**——权限不足抛 `ForbiddenException`/`UnauthorizedException`，由 GlobalExceptionHandler 统一返回；超管放行全部，TEACHER 可访问开放给 USER 的接口，教师专属接口须显式列 TEACHER。
- api 契约：`UserInfoApi`、`NotificationSettingsApi` + `UserInfoDTO` 位于独立 artifact **cas-module-system-api**；实现 `UserInfoApiImpl` 在本模块 api/impl，`NotificationSettingsService` 直接实现该契约。`GetUserIdViaTokenApi` 定义在 cas-common。
- 另有 NotificationPolicy*（全局策略 + 用户偏好）、BookingRecord*（我的预约视图）。

### cas-module-infra
- FileAdminController `POST /admin/files`（本地上传，绝对路径 transferTo，支持子目录 uuid 命名）、OSSAdminController `POST /admin/files/oss`；QRCodeAppController `GET /app/qr-code`。
- FileService / EmailService 契约位于 **cas-module-infra-api**（2.3，FileService 含字节版 uploadFile 与 deleteByUrl）；本模块提供实现（@Async，JavaMail 465 SSL）/ QRCodeService（Hutool → OSSService）。无 domain 层。

### cas-thirdparty（注意：AI 对话链已整体删除）
- 现存：WeatherController（`GET /weather`、`/weather/local`）、WeatherApi(Impl)、OSSService(Impl)、SmsService(Impl)、AliyunConfig、OSSConfig。
- 注：原孤儿配置类 `QwenConfig` / `DeepSeekConfig` 已于 **2026-09-12 删除**。
- 已于 **2026-09-07 下线删除**：CallTheModelController、CallModelService(Impl)、ChatReqVO/RespVO、AiChatHistory 实体/DO/Mapper/Repository、`ai_chat_history` 表（Flyway V1 已同步裁剪）。不要再恢复或引用 `/ai/chat`、`/callTheLargeModel`。AI 对话唯一存储在 kb-service 的 conversation 表。

### cas-server
- CampusAppointmentApplication（@SpringBootApplication + @MapperScan("com.laoliu.cas.**.mapper") + @ComponentScan("com.laoliu.cas")）。
- DbResetConfig：三道开关齐备才执行——`APP_DB_RESET_ON_STARTUP=true` + `FLYWAY_CLEAN_DISABLED=false` + `APP_REDIS_FLUSH_ON_RESET=true`（后者仅控制是否 FLUSHDB；缺它只跳过 Redis 并打 WARN，前两个矛盾时 Bean 初始化 fail-fast），compose 默认全 false/true/false（6.2/6.3）。
- controller/ConfigDemoController（`GET /config-demo/greeting`，Nacos 热更新演示）、SentinelDemoController（`GET /sentinel-demo/limited`）。
- resources：application.yml（无 application.yml.example，无明文密钥，全部环境变量 + 本地默认值；DB/Redis 主机经 `${DB_HOST:localhost}` 等占位符注入，6.5）、db/migration/V1~V8。

## 数据库（Flyway V1~V8，全新机器零手工 SQL）

| 表 | 要点 |
|---|---|
| `user` | role 唯一来源 common-auth `RolePolicy`：0 普通用户 / 1 管理员 / 2 超级管理员 / 3 教师（可经接口分配仅 0/1/3，超管不可经接口设置）；email_notify 偏好 |
| `notification_policy` | 全局单行，邮件通道开关 |
| `service_category` | 固定 4 类（教师咨询/设备借用/教室空间/活动报名），不可在管理端增删改 |
| `services` | category_id（代码级外键）、campus(cq/xs)、image_url、capacity(-1 不限)、booked_count |
| `consultant` / `time_slot` | 咨询师挂校区服务；可约时段落库（非硬编码） |
| `room` | 教室 + 校区；同间同时段唯一 |
| `equipment` | total_stock / available_stock / unit / location |
| `item` | 预约单：service_id + 资源列其一（consultant_id/slot_id… 或 room_id… 或 equipment_id/quantity）；manage_status 0待审/1通过/2拒绝/3取消/4完成；reason |
| `carousel` | image_url / sort / enabled（上限 `${carousel.max-count:6}`，40030 CAROUSEL_LIMIT_EXCEEDED） |
| `consult_chat_conversation` / `consult_chat_message` | 学生⇄教师 1:1 唯一会话；read_flag 未读 |

种子：V1 两校区服务/咨询师/教室/设备/轮播图/分类；V2 admin@campus.com、user@campus.com（密码 123456）；V3 教师账号 + 咨询师回填。

**迁移约定（开发期）**：当前无线上存量数据，全部表结构直接维护在 `V1__init_schema.sql` 全量基线中（含生成列唯一索引、end_date、email 唯一索引等），不写 ALTER 增量脚本；结构变更后用 `backend/scripts/reset-dev-env.sh` 清空开发库（MySQL/Redis/Qdrant/ES），重启时 Flyway 重放 V1~V3。`sql/` 目录是演进/参考脚本，不参与 Flyway。将来上线、存在不可丢弃的存量数据后，再恢复「Vn 只追加、不改旧文件（checksum）」规范。

## 关键业务规则

- **防冲突**：咨询/教室按时段重叠查询 + 行级锁；活动 `capacity/booked_count` 原子扣减；取消/拒绝回补。活动容量够即直通成功（无人工审核）。
- **设备库存模型（易误解）**：`equipment.available_stock` 是**静态总台数、不扣减**；实际占用由 `sumEquipmentOverlap()` 按"日期+时段重叠"动态统计。它与 `services.booked_count`（累计计数）是两套不同模型。
- **设备借用必须走专用端点**：通用下单 `POST /app/bookings` 会拒绝 `equipment` 类服务（`EQUIPMENT_REQUIRE_DEDICATED_API` 40020）——它不携带 equipmentId/时段/数量，会造成时段占用统计不到而超借。
- **幂等**：下单 SQL 60s 窗口同用户同服务去重，重复返回 `BOOKING_REPEATED`。
- **审核邮件**：全局策略 + 用户 `email_notify` 双开关；拒绝必填原因。
- **统一返回**：`CommonResult<T>`（cas-common.result）；业务错误抛 `BusinessException(ErrorCode)`，由 starter-web 的 GlobalExceptionHandler 兜底，禁止 Controller 手写 JSON / try-catch。
- **跨服务响应码契约**：CAS 与 KB 统一 `code = 200`（Integer）表示成功。KB 侧已于 2026-09-12 由 `0` 改为 `200`、`Object` 改为 `Integer`，字符串错误码（A001/U002/G001…）映射为数值区间；前端判断统一引用 `API_SUCCESS_CODE` 常量，勿再写死字面量或双写兼容 `0/200`。
- **HTTP 语义（2026-09-12 起）**：业务异常 **400** / 未授权 **401** / 禁权 **403** / 未找到 **404** / 兜底 **500**。此前业务异常也返回 200（仅靠 body 的 code 区分），现已补齐 `@ResponseStatus`；catch-all 只返回通用文案，完整堆栈仅进日志、不泄露内部信息。⚠ 外部调用方若依赖旧的 200 需同步适配。
- **异常处理器位置（易找错）**：专职实现在 `cas-spring-boot-starter-web` 的 **`com.laoliu.cas.web.exception.GlobalExceptionHandler`**。`WebAutoConfiguration` 上虽也有 `@RestControllerAdvice`，但**仅保留 `IllegalArgumentException`（400）**——其余类型一律交给前者。同一异常被两个 advice 同时声明时，生效哪个取决于注册顺序、行为不确定，新增处理前务必先确认目标类型是否已被 `GlobalExceptionHandler` 覆盖。
- **错误码**：`*ErrorCode` 接口（Book/Common/Email/Login/Role/Service/ServiceStatus/User/Chat），历史上 HTTP 码与领域码混用，新增时向领域码靠拢。

## 编码约定

Do：Controller 返回 `CommonResult<T>`；用 `SecurityFrameworkUtils` 取当前用户；用 `@RequireRole` 鉴权；跨模块走 `api/`；domain 保持纯 Java + Lombok；Mapper XML 放 `classpath*:/mapper/**/*.xml`；新密钥走环境变量并在 `.env.example` 登记。

Don't：cas-server/cas-common 写业务；跨模块直接注入 Mapper；注入 HttpServletRequest 取用户；`CommonResult.error("字符串")`；domain 加 Spring 注解；application.yml 放真实凭据；恢复已下线的 Qwen AI 链。

## 测试现状

170 个 `@Test` / 24 个测试类（JUnit5 + Mockito；除 cas-server 的 V8 IT 外均为纯单元测试，不起 Docker）：
- appointment（83，11 类）：BookServiceImplTest 18、AppointmentAssistantServiceImplTest 16、ServiceStatusServiceImplTest 16、CarouselServiceImplTest 7、ServiceItemServiceImplTest 8、TeacherAuditServiceImplTest 5、AvailabilityControllerTest 3、ConsultChatServiceImplTest 3、BookingEventPublisherTest 4、BookingAutoCompleteTaskTest 2、DomainLayerBoundaryTest 1。
- system（48，5 类）：AuthServiceTest 18、RoleServiceImplTest 14、RoleAspectTest 11、EmailVerificationServiceImplTest 3、UserServiceImplTest 2。
- infra（13，3 类）：FileServiceImplDeleteByUrlTest 8、QRCodeServiceImplTest 3、EmailServiceImplTest 2。
- thirdparty（6）：WeatherApiImplTest 4、SmsServiceImplTest 2。
- cas-server（20，3 类）：V8BookingGuardDbTest 14（需外部 MySQL，IT_MYSQL_* 环境变量）、DbResetConfigTest 5、AdminEndpointAuthorizationGuardTest 1（管理端控制器授权守护，纯反射）。
- 模块外同域：cas-spring-boot-starter-security 6（InternalAuthFilterTest 3 等）、gateway 16（AuthGlobalFilterTest 等）。

## 仍存在的已知限制（真实，未修）

1. Error code 口径不统一（HTTP 码 / 领域码混用）。
2. CAS 用 `CommonResult`，KB 用 `ApiResponse`，跨服务响应模型未统一（牵涉前端契约）。
3. Maven groupId 不统一（com.laoliu vs com.kb）。
4. DTO 命名混用（*ReqVO / *Request / *DTO / *RespVO）。
5. ~~`QwenConfig` / `DeepSeekConfig` 是孤儿配置类~~ ✅ 已于 2026-09-12 删除。
6. Sentinel 已接入但 Nacos 流控规则为空，预留生产调优。
7. KB 侧消费 appointment.changed 仅记日志，索引更新 TODO（属 kb-service）。
