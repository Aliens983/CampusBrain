# cas-module-system — 用户与账号模块

提供注册登录、验证码、密码管理、角色权限与通知策略等账号体系能力。

## 核心功能
- **认证**：登录（图形验证码）、注册（邮箱验证码）、忘记密码；`/auth` 分组。
- **验证码**：算术 CAPTCHA（Hutool，Redis 存答案 TTL）、邮箱 6 位验证码（Redis 限频 60s）。
- **密码**：BCrypt 存储；个人中心改密 `PUT /users/password`（校验旧密码）。
- **角色权限**：`@RequireRole` 注解 + `RoleAspect` AOP 拦截（权限不足抛异常走全局处理器），四级角色 USER(0)/ADMIN(1)/SUPER_ADMIN(2)/TEACHER(3)；超管全放行，教师可访问开放给 USER 的接口，教师专属接口须显式列 TEACHER。
- **通知偏好**：`notification_policy`（全局单行）+ `user.email_notify` 用户开关。
- **文件上传集成**：验证码图片经 `infra.FileService` 落 `uploads/captcha/`。

## 目录结构
```
com.laoliu.cas.system
├── interfaces/controller/
│   ├── app/          # LoginController、RegisterController、EmailController、GraphicController（/auth /captcha）
│   └── admin/        # UserController、RoleAdminController（/admin/users）、NotifyPolicyAdminController（/admin/settings）、EmailAdminController（/admin/email）
├── interfaces/dto/   # request（登录/注册/重置/改密/验证码）/ response / assembler
├── application/service/        # AuthService、CaptchaService、EmailVerificationService、RoleService、UserService（+ impl）
├── domain/                     # User 实体 + UserRepository（纯 Java）
├── infrastructure/
│   ├── aspect/                 # RoleAspect（@RequireRole 拦截）
│   └── persistence/            # UserDO / UserMapper / UserRepositoryImpl
└── api/                        # UserInfoApi、GetUserIdViaTokenApi（供其他模块调用）
```

## 主要 REST 分组（网关前缀 `/api/v1`）
| 路径 | 说明 |
|---|---|
| `POST /auth/login` · `POST /auth/register` · `POST /auth/reset` | 登录 / 注册 / 忘记密码 |
| `POST /auth/verification-code` | 发送邮箱验证码 |
| `GET /captcha` | 算术图形验证码 |
| `GET/PUT /users`、`PUT /users/password` | 资料 / 改密 |
| `GET/PUT /admin/users`、`GET/PUT /admin/users/role` | 用户列表 / 角色 |
| `GET/PUT /admin/settings/notify` | 全局通知策略 |
| `POST /admin/email` | 管理端邮件接口（用途见 `EmailAdminController`） |

## 数据表（Flyway V1）
`user`（role 0 普通/1 管理员/2 超管/3 教师 · email_notify）、`notification_policy`（单行策略）。

## 依赖
依赖 `cas-module-infra`（邮件/文件）；被 `cas-module-appointment` 依赖（经 `UserInfoApi` 取用户）。
