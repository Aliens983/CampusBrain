# AGENTS.md — cas-module-system

用户与账号模块：认证（登录/注册/忘记密码）、图形/邮箱验证码、用户资料与改密、四级角色、通知策略。权威说明见上层 `../CLAUDE.md`。

## 包结构（com.laoliu.cas.system）

```
interfaces/
├── controller/
│   ├── app/    LoginController(/auth：login、reset) · RegisterController(/auth/register)
│   │           EmailController(/auth/verification-code) · GraphicController(GET /captcha)
│   └── admin/  UserController(@RequestMapping("/users")：/、/me、/list、POST、PUT /me、
│   │               /me/notify GET|PUT、PUT /password、GET /me/bookings)
│               RoleAdminController(/admin/users/role GET|PUT)
│               NotifyPolicyAdminController(/admin/settings/notify GET|PUT)
│               EmailAdminController(POST /admin/email)
├── dto/        request（UserLoginRequest、UserRegisterRequest、VerifyCodeReqVO、ResetPasswordRequest、
│                   ChangePasswordRequest、UpdateProfileRequest、AdminCreateUserRequest、ChangeRoleRequest、
│                   UserPageReqVO、EmailRequest …）
│               response（UserResponse、EmailResponse、CaptchaRespVO、ChangeRoleRespVO、
│                   BookingRecordRespVO、UserInfoAndServicesViaMPRespVO …）
│               NotifyPolicyDTO、NotifyPrefDTO
├── convert/    UserConvert、BookingRecordConvert
└── assembler/  UserAssembler
application/
├── service/    AuthService、CaptchaService、EmailVerificationService、UserService、
│               RoleService、NotificationSettingsService（+ impl）
└── service/vo/ CaptchaResult、UserRegisterVO
domain/         entity/User · repository/UserRepository
infrastructure/
├── aspect/     RoleAspect（@RequireRole 拦截，抛 Unauthorized/ForbiddenException）
└── persistence/dataobject（UserDO、BookingRecordDO）
                mapper（UserMapper、NotificationPolicyMapper —— 后者注解 SQL 直查 notification_policy 单行，无 DO）· repository/UserRepositoryImpl
api/            UserInfoApi + impl/UserInfoApiImpl + dto/UserInfoDTO
                GetUserIdViaTokenApiImpl（接口定义在 cas-common）
```

> 历史问题均已修复：登录/注册/验证码**无重复 Controller**；**RoleAspect 改为抛异常**（走
> GlobalExceptionHandler，不再手写 JSON）；**角色切换颠倒 Bug 已修**；请求 DTO 已加 Bean Validation。

## 关键业务

- **登录**：邮箱+密码（BCrypt 校验）+ 图形验证码 → 签发 JWT。
- **注册/重置**：邮箱 6 位验证码（Redis `verification_code:{email}` TTL 300s，限频 `rate_limit:email:{email}` 60s）→ 写库。
- **验证码**：Hutool 算术验证码，答案存 Redis（captcha:{uuid} TTL 300s），图片经 infra FileService 落 uploads/captcha。
- **角色**：`UserRoleEnum` USER(0)/ADMIN(1)/SUPER_ADMIN(2)/TEACHER(3)；超管全放行，教师可访问开放给 USER 的接口。
- **通知**：NotificationSettingsService 管理全局 `notification_policy` 与用户 `email_notify` 偏好。

## 接口（网关前缀 /api/v1）

`POST /auth/login|/reset`、`POST /auth/register`、`POST /auth/verification-code`、`GET /captcha`、
`GET/POST/PUT /users/*`、`GET/PUT /admin/users/role`、`GET/PUT /admin/settings/notify`、`POST /admin/email`。

## 数据表

`user`（role 0~3、email_notify）、`notification_policy`（单行全局策略）；Mapper XML：resources/mapper/UserMapper.xml。

## 测试

4 个测试类 / 30 个 `@Test`：AuthServiceTest 15、RoleServiceImplTest 10、
EmailVerificationServiceImplTest 3、UserServiceImplTest 2。

## 依赖与对外 API

依赖 `cas-module-infra`（邮件/文件）、`cas-thirdparty`、`cas-framework`；不依赖 appointment。
对外提供 `UserInfoApi`（appointment 取用户）与 `GetUserIdViaTokenApi`（RoleAspect 等用）。
