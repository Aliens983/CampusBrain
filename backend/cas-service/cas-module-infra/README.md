# cas-module-infra — 基础设施服务模块

为业务模块提供通用基础设施能力（文件/邮件/二维码）。跨模块契约 `api.file.FileService`、`api.email.EmailService` 定义在独立的零实现 artifact **`cas-module-infra-api`** 中，本模块提供实现并由 `cas-server` 运行时装配；`system`、`appointment` 编译期只依赖契约。

## 核心功能
- **本地文件上传**：`FileService` 生成 UUID 文件名存入 `uploads[/子目录]`，返回 `/uploads/…` URL；Tomcat `transferTo` 用**绝对路径**规避临时目录问题；支持按子目录分类（验证码 `captcha`、轮播图 `carousel`、封面等）。
- **文件删除**：`FileService.deleteByUrl(url)` 按访问 URL 反解物理路径删除（轮播图删除等链路调用，删除失败只记日志不阻断业务）。
- **文件管理接口**：`POST /admin/files`（本地上传，返回相对 URL）、`POST /admin/files/oss`（阿里云 OSS）。
- **邮件**：`EmailService.sendEmail(...)` @Async（JavaMail 465 SSL）。
- **二维码**：`GET /app/qr-code`（Hutool 生成）。

## 目录结构
```
com.laoliu.cas.infra
├── interfaces/controller/
│   ├── admin/        # FileAdminController（/admin/files）、OSSAdminController
│   └── app/          # QRCodeAppController（/app/qr-code）
├── interfaces/dto/
└── application/service/
    ├── FileServiceImpl（实现 cas-module-infra-api 的 api.file.FileService）
    ├── EmailServiceImpl（实现 cas-module-infra-api 的 api.email.EmailService）
    └── QRCodeService / QRCodeServiceImpl
```
> 本模块自包含，**无 domain 层**；对外契约（`FileService`/`EmailService`）位于 `cas-module-infra-api`，上层业务模块只注入契约接口。

## 主要接口
| 路径 | 说明 |
|---|---|
| `POST /admin/files` | 本地上传（`@RequestParam file`），返回 `/uploads[/子目录]/<uuid>.<ext>` |
| `POST /admin/files/oss` | 上传到阿里云 OSS |
| `GET /app/qr-code` | 生成二维码 |

## 依赖与约束
- 只依赖 `cas-framework`，**不依赖任何业务模块**（依赖图最底层之一）。
- 上传目录可配置：`file.upload.dir=uploads`、`file.upload.url-prefix=/uploads`。
- UUID 文件名天然避免重名覆盖；物理文件删除由调用方显式触发 `deleteByUrl`（如轮播图管理），本模块不做自动级联删除。
