# CampusAppointmentSystem — Backend CLAUDE.md

This file is the definitive guide for Claude Code when working on this backend. It documents every module, every service, every convention, and every known issue. Read this first before making any code changes.

> ⚠️ **当前状态（2026-08-21 更新）**：本项目已重构为微服务的一部分。下方正文是**重构前**的参考，多数「已知问题」已修复、接口路径与模块结构已变化，**以 `../README.md` 和代码为准**。关键变化摘要：
>
> - **微服务化**：cas-service 是 3 个服务之一（还有 gateway、kb-service），统一由 `common-auth` + `gateway` 做 JWT 网关鉴权 + 内网签名。
> - **安全已修复**：注册提权、IDOR、管理端裸奔、`/ai` 公开、JWT 密钥外部化、角色层级 Bug 均已修。**全仓库 `mvn test` 120 个测试全绿**（CAS 65 + KB 55）。
> - **服务间调用**：KB 通过 OpenFeign + Nacos 服务发现 + 内网签名头直连 CAS 只读接口；`InternalAuthFilter` 校验签名通过后写入 SecurityContext，放行服务间调用。
> - **预约幂等**：`insertServices` 改为幂等 SQL（60 秒窗口同用户同服务去重），新增 `BOOKING_REPEATED` 错误码。
> - **新增依赖**：Nacos 注册/配置中心、Sentinel 限流（Nacos 数据源）、RabbitMQ（预约变更事件）。
> - **新增接口**：`PUT /users/me`（更新资料）、`GET /appointments/availability`（实时余量）、`/config-demo/**`（配置热更新）、`/sentinel-demo/**`（限流演示）。
> - **配置**：`spring.application.name=cas-service`；`spring.config.import=optional:nacos:cas-service.yaml`；已移除 MyBatis `StdOutImpl`、日志级别 info。

---

## Quick Reference Card

```
Port:      18080 (not 8080)
Java:      17
Spring:    Boot 3.3.5
ORM:       MyBatis-Plus 3.5.5
DB:        MySQL (cas_db), user root
Cache:     Redis localhost:6379 db 0
Mail:      SMTP 163.com (dmregy@163.com), SSL 465
Auth:      Stateless JWT (HMAC-SHA512, 24h expiration)
API Docs:  Knife4j 4.5.0 → http://localhost:18080/doc.html
Build:     mvn clean package -DskipTests   (NO mvnw)
Run:       java -jar cas-server/target/cas-server-1.0.0.jar
Base pkg:  com.laoliu.cas
Group ID:  com.laoliu
```

---

## Module Dependency Graph (Bottom → Top)

```
cas-dependencies (BOM)
    ↓ imports
cas-framework (pom aggregator of starters)
    ├── cas-common                         ← shared kernel (24 files)
    ├── cas-spring-boot-starter-web        ← GlobalExceptionHandler
    ├── cas-spring-boot-starter-security   ← JWTFilter + SecurityConfig
    ├── cas-spring-boot-starter-mybatis    ← MyBatis-Plus config
    ├── cas-spring-boot-starter-redis      ← RedisTemplate + RedisUtil
    ├── cas-spring-boot-starter-mq         ← EMPTY stub (no implementation)
    └── cas-spring-boot-starter-test       ← BaseApplicationTest
    ↓ used by
cas-module-infra          ← files, QR, email  (depends: common + thirdparty)
cas-module-system         ← users, auth, roles (depends: infra + thirdparty)
cas-module-appointment    ← booking, services  (depends: system + infra)
cas-thirdparty            ← AI, weather, OSS, SMS (depends: common only)
    ↓ aggregated by
cas-server                ← entry point, application.yml, @MapperScan, @ComponentScan
```

**Hard rules:**
- `infra` MUST NOT depend on `system` or `appointment`
- Business modules MUST NOT depend on each other directly
- `thirdparty` depends ONLY on `cas-common`
- `cas-server` contains ZERO business code — only boot config

---

## DDD Four-Layer Layout (Every Business Module)

```
<module>/src/main/java/com/laoliu/cas/<domain>/
│
├── interfaces/                    ← REST boundary
│   ├── controller/
│   │   ├── admin/                 ← Admin endpoints, guarded with @RequireRole
│   │   └── app/                   ← User-facing endpoints
│   ├── dto/
│   │   ├── request/               ← *Request DTOs (inbound)
│   │   └── response/              ← *Response DTOs (outbound)
│   └── assembler/                 ← Entity ↔ DTO converters
│
├── application/                   ← Orchestration layer
│   └── service/
│       ├── XxxService.java        ← Interface (application concerns)
│       └── impl/
│           └── XxxServiceImpl.java ← Implementation (orchestration only, NO direct DB)
│
├── domain/                        ← Pure business logic
│   ├── entity/                    ← Domain entities (NO Spring annotations!)
│   └── repository/                ← Repository interfaces (contract only)
│
├── infrastructure/                ← Technical implementation
│   ├── persistence/
│   │   ├── dataobject/            ← *DO classes (MyBatis-Plus entities)
│   │   ├── mapper/                ← MyBatis Mapper interfaces (@Mapper annotated)
│   │   └── repository/            ← Repository interface implementations
│   ├── external/                  ← External API adapters
│   └── aspect/                    ← AOP aspects (RoleAspect, etc.)
│
└── api/                           ← Cross-module public API
    ├── XxxApi.java                ← Interface (what other modules can call)
    └── XxxApiImpl.java            ← @Component implementation
```

**Layer rules:**
- **domain/** — zero framework annotations, pure Java. If you see `@Service`/`@Component`/`@Autowired` here, it's a violation.
- **application/** — orchestrates domain objects, calls repositories. No direct MyBatis/DB access.
- **infrastructure/** — implements domain repository interfaces. Contains MyBatis mappers, DOs, XML.
- **interfaces/** — controllers only receive/return DTOs, never domain entities.
- **api/** — the ONLY way another module calls into this module.

---

## Complete File Inventory

### cas-common (24 files, the shared kernel)

```
com.laoliu.cas.common
├── annotation/
│   └── RequireRole.java              ← @RequireRole({ADMIN, SUPER_ADMIN})
├── api/
│   └── GetUserIdViaTokenApi.java     ← Cross-module current-user-id interface
├── enums/
│   ├── ManageStatus.java             ← SUBMIT(0)/APPROVED(1)/REJECTED(2)/CANCELLED(3)
│   ├── ServiceStatus.java            ← service state enum
│   └── UserRoleEnum.java             ← USER(0)/ADMIN(1)/SUPER_ADMIN(2)
├── exception/
│   ├── BusinessException.java        ← General business exception (has ErrorCode + 可变参数)
│   ├── ErrorCode.java                ← ErrorCode record(code int, message String)
│   ├── ForbiddenException.java
│   ├── ResourceNotFoundException.java
│   └── UnauthorizedException.java
├── exception/code/
│   ├── BookErrorCode.java            ← 预约相关错误码
│   ├── CommonErrorCode.java          ← 通用错误码 (EMAIL_SEND_FAILED etc.)
│   ├── EmailErrorCode.java
│   ├── LoginErrorCode.java
│   ├── RoleErrorCode.java
│   ├── ServiceErrorCode.java
│   ├── ServiceStatusErrorCode.java   ← STATUS_NOT_FOUND, AUDIT_FAILED, AUDIT_REASON_REQUIRED, etc.
│   └── UserErrorCode.java
├── result/
│   └── CommonResult.java             ← CommonResult<T>(code, message, data) + static factories
├── security/
│   ├── JWTUtils.java                 ← HMAC-SHA512 JWT create/parse
│   ├── LoginUser.java                ← SecurityContext principal carrier
│   └── SecurityFrameworkUtils.java   ← getLoginUser()/getLoginUserId()/getLoginUserRole()
└── util/
    ├── CodeGenerator.java            ← 6-digit random code
    └── PasswordUtils.java            ← BCrypt encode/matches (static PasswordEncoder)
```

### cas-module-system (22 files — user/auth/role management)

```
com.laoliu.cas.system
├── interfaces/
│   ├── assembler/
│   │   └── UserAssembler.java        ← User → UserResponse DTO converter
│   ├── controller/
│   │   ├── admin/
│   │   │   ├── EmailAdminController.java
│   │   │   ├── RoleAdminController.java
│   │   │   └── UserController.java
│   │   └── app/
│   │       └── EmailController.java  ← Bridge: POST /email
│   ├── dto/
│   │   ├── request/
│   │   │   ├── AdminCreateUserRequest.java
│   │   │   ├── ChangePasswordRequest.java
│   │   │   ├── EmailRequest.java
│   │   │   ├── ResetPasswordRequest.java
│   │   │   ├── UserLoginRequest.java
│   │   │   └── UserRegisterRequest.java
│   │   └── response/
│   │       ├── EmailResponse.java
│   │       ├── UserInfoAndServicesViaMPRespVO.java
│   │       └── UserResponse.java
│   └── vo/                           ← NOT standard DDD; internal VOs
│       ├── CaptchaResult.java
│       ├── UserRegisterVO.java
│       └── VerifyCodeReqVO.java
├── application/service/
│   ├── AuthService.java              ← login/register/reset-password
│   ├── CaptchaService.java           ← math captcha generation
│   ├── EmailVerificationService.java ← email code generation + send with rate limit
│   ├── RoleService.java
│   ├── UserService.java
│   └── impl/
│       ├── AuthServiceImpl.java
│       ├── CaptchaServiceImpl.java
│       ├── EmailVerificationServiceImpl.java
│       ├── RoleServiceImpl.java
│       └── UserServiceImpl.java
├── domain/
│   ├── entity/
│   │   └── User.java                 ← Anemic (no behavior methods)
│   └── repository/
│       └── UserRepository.java
├── infrastructure/
│   ├── aspect/
│   │   └── RoleAspect.java           ← @RequireRole interceptor (HAS ISSUES)
│   └── persistence/
│       ├── dataobject/UserDO.java
│       ├── mapper/UserMapper.java
│       └── repository/UserRepositoryImpl.java
└── api/
    ├── UserInfoApi.java              ← getUserById(Long) → UserInfoDTO
    ├── UserInfoApiImpl.java
    └── dto/UserInfoDTO.java
```

### cas-module-appointment (22 files — core business)

```
com.laoliu.cas.appointment
├── interfaces/
│   ├── controller/
│   │   ├── admin/
│   │   │   ├── ServiceAdminController.java      ← GET/POST /admin/service
│   │   │   └── ServiceStatusAdminController.java ← POST /admin/service-status/audit/{pass,reject}
│   │   └── app/
│   │       ├── BookAppController.java           ← POST /book, /book/{room,equipment,consultation}
│   │       ├── ConsultationAppController.java
│   │       ├── EquipmentAppController.java
│   │       ├── ServiceAppController.java
│   │       ├── ServiceController.java           ← Bridge: GET /service
│   │       ├── ServiceStatusAppController.java
│   │       └── ServiceStatusController.java     ← Bridge: GET /service-status/user
│   ├── dto/
│   │   ├── request/
│   │   │   ├── AuditRequest.java                ← orderId, status(1/2), reason
│   │   │   ├── ServiceAddRequest.java
│   │   │   └── SpecializedBookingRequest.java
│   │   └── response/
│   │       ├── BookingDTO.java
│   │       ├── BookResultResponse.java
│   │       ├── ConsultantResponse.java          ← FAKE data
│   │       ├── EquipmentResponse.java           ← FAKE data
│   │       └── ServiceStatusResponse.java
├── application/service/
│   ├── BookService.java / impl/BookServiceImpl.java
│   ├── ConsultationService.java / impl/ConsultationServiceImpl.java     ← FAKE data
│   ├── EquipmentService.java / impl/EquipmentServiceImpl.java           ← FAKE data
│   ├── ServiceService.java / impl/ServiceServiceImpl.java
│   └── ServiceStatusService.java / impl/ServiceStatusServiceImpl.java   ← audit logic
├── domain/
│   ├── entity/
│   │   ├── AppointmentRecord.java     ← Anemic
│   │   └── Service.java              ← Has domain behaviors: isAvailable(), enable(), disable()
│   └── repository/
│       ├── BookingRepository.java
│       └── ServiceRepository.java
└── infrastructure/persistence/
    ├── dataobject/
    │   ├── AppointmentRecordDO.java
    │   ├── ItemDO.java
    │   └── ServicesDO.java
    ├── mapper/
    │   ├── ItemMapper.java
    │   └── ServiceMapper.java
    └── repository/
        ├── BookingRepositoryImpl.java
        └── ServiceRepositoryImpl.java
```

### cas-module-infra (9 files — infrastructure services)

```
com.laoliu.cas.infra
├── interfaces/controller/admin/
│   ├── FileAdminController.java      ← POST /admin/file/upload
│   └── OSSAdminController.java      ← POST /admin/oss/upload
├── interfaces/dto/
│   └── FileUploadReqVO.java          ← Only DTO with @NotNull Bean Validation
├── application/service/
│   ├── EmailService.java             ← sendEmail(to, subject, content) @Async
│   ├── FileService.java              ← local file upload
│   ├── QRCodeService.java            ← QR code generate → OSS upload
│   └── impl/
│       ├── EmailServiceImpl.java     ← JavaMailSender + spring.mail.username
│       ├── FileServiceImpl.java      ← ./uploads/, UUID rename
│       └── QRCodeServiceImpl.java    ← Hutool QrCodeUtil → ByteArrayMultipartFile → OSS
└── (no domain/ or api/ subpackages — infra is self-contained)
```

### cas-thirdparty (16 files — external integrations)

```
com.laoliu.cas.thirdparty
├── config/
│   ├── AliyunConfig.java             ← SMS Client bean
│   ├── DeepSeekConfig.java           ← EXISTS BUT NEVER USED (dead code)
│   └── QwenConfig.java               ← WebClient bean for DashScope
├── controller/
│   ├── CallTheModelController.java   ← POST /callTheLargeModel
│   └── WeatherController.java        ← GET /weather
├── service/
│   ├── WeatherApi.java / impl/WeatherApiImpl.java       ← RestTemplate → cn.apihz.cn
│   ├── CallModelService.java / impl/CallModelServiceImpl.java ← WebClient → Qwen API
│   ├── OSSService.java / impl/OSSServiceImpl.java       ← Aliyun OSS upload
│   └── SmsService.java / impl/SmsServiceImpl.java       ← Aliyun SMS send
├── domain/
│   ├── entity/AiChatHistory.java     ← Anemic entity
│   └── repository/AiChatHistoryRepository.java
├── infrastructure/persistence/
│   ├── dataobject/AiChatHistoryDO.java
│   ├── mapper/AiChatHistoryMapper.java
│   └── repository/AiChatHistoryRepositoryImpl.java
├── dto/
│   ├── ChatReqVO.java
│   ├── ChatRespVO.java
│   └── WeatherResponse.java
└── (resources/mapper/AiChatHistoryMapper.xml)
```

### cas-server (1 file + config)

```
com.laoliu.cas.server
└── CampusAppointmentApplication.java   ← @SpringBootApplication, @MapperScan, @ComponentScan

src/main/resources/
├── application.yml                     ← Real credentials (DB/SMTP/JWT/API keys) — TREAT AS SECRETS
└── application.yml.example             ← Template without real credentials
```

---

## Key Business Flows

### 1. Login Flow
```
POST /login {email, password, captchaId, captchaCode}
  → AuthService.login()
    → UserRepository.findByEmail() → verify BCrypt password → JWTUtils.generateToken()
    → Return CommonResult<LoginUser>
```

### 2. Registration Flow
```
Step 1: POST /email {email} → EmailVerificationServiceImpl
          → Redis SET "rate_limit:email:{email}" TTL 60s (rate limit)
          → CodeGenerator.generate() → Redis SET "verification_code:{email}" TTL 300s
          → EmailService.sendEmail() @Async

Step 2: POST /register/verify-code {email, code, password, name, ...}
          → AuthService.register()
            → Redis GET "verification_code:{email}" → compare code
            → UserRepository.findByEmail() → check uniqueness
            → PasswordUtils.encode(password) → UserRepository.save()
            → Redis DELETE "verification_code:{email}" → JWTUtils.generateToken()
```

### 3. Booking Flow
```
POST /book {serviceIds: [1, 2]}
  → BookServiceImpl.bookService()
    → Validate serviceIds not empty → ServiceRepository.selectByIds()
    → Filter available services → @Transactional
    → BookingRepository.insertServices(userId, serviceIds)
    → Return BookResultResponse (success + failed lists)

Specialized bookings:
  POST /book/room        → doSpecializedBooking(request, "会议室")
  POST /book/equipment   → doSpecializedBooking(request, "设备")
  POST /book/consultation → doSpecializedBooking(request, "咨询")
  All three call the same BookServiceImpl.bookService() underneath
```

### 4. Audit Flow (Approve / Reject)
```
POST /admin/service-status/audit/pass  {orderId, status:1, reason?}
  → @RequireRole({ADMIN, SUPER_ADMIN})
  → ServiceStatusServiceImpl.auditPass(orderId, reason)
    → getServiceStatusByOrderId() → check non-null
    → auditService(orderId, APPROVED, reason)
      → SQL: UPDATE item SET manage_status=1, reason=? WHERE order_id=? AND manage_status=0
    → Send email: "预约审核通过通知" + service details + optional reason

POST /admin/service-status/audit/reject {orderId, status:2, reason}
  → @RequireRole({ADMIN, SUPER_ADMIN})
  → ServiceStatusServiceImpl.auditReject(orderId, reason)
    → Validate reason not blank → throw AUDIT_REASON_REQUIRED if blank
    → getServiceStatusByOrderId() → check non-null
    → auditService(orderId, REJECTED, reason)
      → SQL: UPDATE item SET manage_status=2, reason=? WHERE order_id=? AND manage_status=0
    → Send email: "预约审核未通过通知" + service details + rejection reason
```

### 5. Captcha Flow
```
GET /graphic/get?uuid=xxx
  → CaptchaServiceImpl.createCaptcha(uuid)
    → Hutool ShearCaptcha + MathGenerator → createImage()
    → Calculator.conversion() evaluates math expression
    → Redis SET "captcha:{uuid}" = answer, TTL 300s
    → Write captcha PNG to temp file → FileService.uploadFile() → delete temp file
    → Return captcha image URL
```

---

## Database Schema (MySQL cas_db, InnoDB, utf8mb4)

### Tables

| Table | Key Columns | Notes |
|-------|-------------|-------|
| `user` | id, name, grade, sex, age, email, password, role | role: 0=USER, 1=ADMIN, 2=SUPER_ADMIN |
| `services` | service_id, service_name, service_describe, service_state | service_state: 0=disabled, 1=enabled |
| `item` | order_id, user_id, service_id, manage_status, reason, create_time, update_time | FK→user.id, FK→services.service_id, manage_status: 0=待审核,1=通过,2=拒绝,3=取消 |
| `file_info` | file_name, file_path, file_uuid, upload_user, is_deleted | Soft delete via is_deleted flag |
| `ai_chat_history` | user_id, model, user_message, ai_response, response_time_ms | Chat history persisted but never queried via API |

### SQL Scripts (in `sql/` directory)
- `database.sql` — CREATE DATABASE
- `user.sql` — user table DDL
- `services.sql` — services table DDL
- `item.sql` — item table DDL
- `file.sql` — file_info table DDL
- `ai_chat_history.sql` — AI chat history DDL
- `data.sql` — Sample data (5 services: 自习室预约, 心理咨询, 学业辅导, 考试报名, 社团活动)
- `indexes.sql` — Additional index creation

**Note:** No Flyway/Liquibase. SQL scripts are run manually.

---

## Auth & Permission System

### JWT Details
- Signing: HMAC-SHA512 (key = SHA-512 hash of `jwt.secret` from application.yml)
- Claims: userId, name, role, email, iat, exp
- Expiration: 86400000 ms (24 hours)
- Token format: `Authorization: Bearer <token>`

### Security Filter Chain
- `SecurityAutoConfiguration` in cas-spring-boot-starter-security
- `SessionCreationPolicy.STATELESS`, CSRF disabled
- `@EnableMethodSecurity(prePostEnabled = true)`
- `JWTFilter extends OncePerRequestFilter` → extracts token → validates → sets SecurityContext

### Permit-All Paths (no auth required)
```
/api/auth/**, /api/public/**, /login, /login/reset, /graphic/get,
/register/verify-code, /email, /error, /api/files/**, /uploads/**,
/doc.html, /swagger-ui/**, /v3/api-docs/**, /webjars/**,
/favicon.ico, /hello, /callTheLargeModel/**
```

### Role-Based Access
- `@RequireRole(UserRoleEnum.ADMIN)` — single role
- `@RequireRole({ADMIN, SUPER_ADMIN})` — multiple roles
- Enforced by `RoleAspect` (AOP around-advice) in cas-module-system
- **KNOWN BUG**: `RoleAspect` writes manual JSON to `HttpServletResponse` instead of throwing exceptions — THIS BYPASSES `GlobalExceptionHandler`
- **KNOWN BUG**: `RoleServiceImpl.changeRoleById()` has the role assignment inverted (toggling to "common user" sets role=1=ADMIN)

### Getting Current User
```java
// In any service/controller:
LoginUser user = SecurityFrameworkUtils.getLoginUser();
Long userId = SecurityFrameworkUtils.getLoginUserId();
Integer role = SecurityFrameworkUtils.getLoginUserRole();

// For cross-module calls, inject:
@Autowired
private GetUserIdViaTokenApi getUserIdViaTokenApi;
```

---

## Response & Error Handling

### CommonResult<T>
```java
CommonResult.success(data);                    // code=200, message="成功"
CommonResult.success("custom msg", data);       // code=200
CommonResult.error(errorCode);                  // uses ErrorCode.code + ErrorCode.message
CommonResult.error(errorCode, params...);       // message with format params
```

### Exception Hierarchy
```
RuntimeException
├── BusinessException(code, message, params...)   ← General business error (GlobalExceptionHandler maps to ErrorCode.code)
├── ForbiddenException                            ← 403
├── UnauthorizedException                         ← 401
└── ResourceNotFoundException                     ← 404
```

### GlobalExceptionHandler (catches 9 exception types)
Located at `cas-spring-boot-starter-web/.../GlobalExceptionHandler.java`

| Exception | HTTP Status |
|-----------|-------------|
| `NoResourceFoundException` | 404 |
| `BusinessException` | From e.getCode() |
| `MethodArgumentNotValidException` | 400 (aggregates field errors) |
| `BindException` | 400 |
| `HttpRequestMethodNotSupportedException` | 405 |
| `UnauthorizedException` | 401 |
| `ForbiddenException` | 403 |
| `ResourceNotFoundException` | 404 |
| `RuntimeException` | 500 |
| `Exception` (catch-all) | 500 |

### Error Code Conventions (Inconsistent — needs fixing)
- Some use HTTP codes: 400, 404
- Some use domain codes: 10001 (from BookErrorCode)
- Some use magic numbers: 3838438 (AUDIT_REASON_REQUIRED), 404404404 (USER_EMAIL_NOT_FOUND)

---

## Cross-Module API Pattern

When module A needs data from module B:

1. Module B defines `api/XxxApi.java` (interface) + `api/XxxApiImpl.java` (@Component)
2. Module A declares dependency on module B in pom.xml
3. Module A injects `XxxApi` via `@Autowired` constructor

**Existing cross-module APIs:**

| API Interface | Location | Provided By | Used By |
|---|---|---|---|
| `UserInfoApi` | cas-module-system/api/ | system | appointment (BookServiceImpl, ServiceAdminController) |
| `GetUserIdViaTokenApi` | cas-common/api/ | system | system (RoleAspect) |
| `EmailService` | cas-module-infra/application/ | infra | system (AuthService), appointment (ServiceStatusServiceImpl) |
| `FileService` | cas-module-infra/application/ | infra | system (CaptchaServiceImpl) |
| `OSSService` | cas-thirdparty/service/ | thirdparty | infra (QRCodeServiceImpl, OSSAdminController) |

**Anti-patterns (DO NOT do this):**
- `ServiceController` directly injects `ServiceRepository` alongside `ServiceService`
- `GraphicVerificationAppController` directly injects `RedisUtil`

---

## Testing

### Current State

The project has **20 unit tests** in the `cas-module-appointment` module, using **JUnit 5 + Mockito**. There are no integration tests yet.

| Test Class | Location | Tests | What It Covers |
|------------|----------|-------|----------------|
| `BookServiceImplTest` | `cas-module-appointment/src/test/.../impl/` | 11 | Booking creation, validation (empty/null IDs, service not found, disabled), cancellation, query by user/order ID |
| `ServiceStatusServiceImplTest` | `cas-module-appointment/src/test/.../impl/` | 9 | Audit approval (with/without reason), audit rejection (reason validation: null/empty/blank), order not found, update failure |

### Testing Infrastructure

- **Test framework**: JUnit 5 (Jupiter) + Mockito (`@ExtendWith(MockitoExtension.class)`)
- **Test base class**: `BaseApplicationTest` in `cas-spring-boot-starter-test` (abstract, `@SpringBootTest`)
- **Dependency**: `spring-boot-starter-test` (pulled in by `cas-module-appointment/pom.xml`, scope `test`)
- **Pattern**: Given-When-Then with `@Nested` inner classes for grouping

### What's Missing

| Gap | Priority |
|-----|----------|
| `cas-module-system` tests | High — AuthService, UserService, CaptchaService |
| `cas-module-infra` tests | Medium — EmailService, FileService |
| Controller integration tests | Medium — `@WebMvcTest` or `@SpringBootTest` + MockMvc |
| Repository integration tests | Low — `@MybatisPlusTest` against real DB |

---

## Known Issues & TODOs（当前真实状态）

> 下列为**截至 2026-08-22 仍未解决**的问题。曾列于此处的多数历史问题（测试覆盖、分页、Bean Validation、重复 Controller、RoleAspect 绕过、角色切换颠倒、无 @Cacheable、无幂等）**均已修复**，此处不再列出。

### 未解决
1. **Error codes 不统一** — 同一 `*ErrorCode` 接口内 HTTP 码（400/404）与领域码（40001…）混用。
2. **响应模型跨服务不统一** — CAS 用 `CommonResult`，KB 用 `ApiResponse`（`code` 为 Object）。统一需同步改前端契约，暂缓。
3. **Maven 治理不统一** — CAS groupId `com.laoliu`，KB `com.kb`；统一 BOM/Parent 需迁包名，风险高，暂缓。
4. **DTO 命名不一致** — `*ReqVO` / `*Request` / `*DTO` / `*Response` 混用。
5. **`DeepSeekConfig` 死代码** — 未被任何 Bean 注入使用。
6. **无数据库迁移工具** — CAS 的 SQL 脚本手工执行（KB 已用 Flyway）。
7. **咨询时段硬编码** — `getAvailableTimeSlots()` 仍返回固定 6 个时段，未落库。
8. **设备图片无数据源** — `EquipmentResponse.image` 无 DB 列支撑。

---

## Coding Conventions

### Do
- ✅ Controllers return `CommonResult<T>` from `cas-common.result`
- ✅ Use `SecurityFrameworkUtils` to get current user (NOT `HttpServletRequest`)
- ✅ Use `@RequireRole` annotation for access control (NOT inline role checks)
- ✅ Add new error codes to the appropriate `*ErrorCode` interface
- ✅ Throw `BusinessException` for expected errors (NOT return `CommonResult.error()`)
- ✅ Cross-module calls through `api/` interfaces
- ✅ Domain entities stay pure (no Spring annotations)
- ✅ Use Lombok `@Builder`, `@Data` on entities
- ✅ MyBatis mapper XML at `classpath*:/mapper/**/*.xml`

### Don't
- ❌ No business code in `cas-server` or `cas-common`
- ❌ No direct Mapper calls from other modules — use `api/` interface
- ❌ No `HttpServletRequest` injection — use `SecurityFrameworkUtils`
- ❌ No `CommonResult.error("string")` — use `CommonResult.error(ErrorCode)`
- ❌ No Spring annotations in `domain/` layer
- ❌ No `try-catch` in controllers — let `GlobalExceptionHandler` handle it
- ❌ No new credentials in `application.yml` — use environment variables

---

## Maven Module Quick Build Commands

```bash
# Build everything
mvn clean package -DskipTests

# Build just appointment module + its dependencies
mvn -pl cas-module-appointment -am clean compile

# Build common changes
mvn -pl cas-framework/cas-common -am clean compile

# Build + run
mvn clean package -DskipTests && java -jar cas-server/target/cas-server-1.0.0.jar

# Run tests for appointment module
mvn -pl cas-module-appointment -am test

# Run a single test class
mvn -pl cas-module-appointment -am test -Dtest=BookServiceImplTest

# Run a single test method
mvn -pl cas-module-appointment -am test -Dtest=ServiceStatusServiceImplTest#shouldApproveAndSendEmailWithoutReason
```

### Dependency Versions (BOM managed in cas-dependencies)

| Dependency | Version |
|-----------|---------|
| Spring Boot | 3.3.5 |
| MyBatis-Plus | 3.5.5 |
| jjwt | 0.12.6 |
| Hutool | 5.8.32 |
| Fastjson2 | 2.0.56 |
| MapStruct | 1.6.3 |
| Knife4j | 4.5.0 |
| Aliyun SMS SDK | 2.0.2 |
| MySQL Connector | runtime scope |

---

## Bridge Controllers (Flat Paths)

These controllers bypass the `/api/` and `/app/` / `/admin/` prefix convention for frontend convenience:

| Controller | Endpoint | Module |
|-----------|----------|--------|
| `LoginController` | `POST /login` | system |
| `GraphicController` | `GET /graphic/get` | system |
| `RegisterController` | `POST /register/verify-code` | system |
| `EmailController` | `POST /email` | system |
| `ServiceController` | `GET /service` | appointment |
| `ServiceStatusController` | `GET /service-status/user` | appointment |
