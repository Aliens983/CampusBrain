# cas-module-infra — 基础设施服务模块

为业务模块提供通用基础设施能力（文件/邮件/二维码），供 `system`、`appointment` 跨模块调用（`EmailService`、`FileService`）。

## 核心功能
- **本地文件上传**：`FileService` 生成 UUID 文件名存入 `uploads[/子目录]`，返回 `/uploads/…` URL；Tomcat `transferTo` 用**绝对路径**规避临时目录问题；支持按子目录分类（验证码 `captcha`、轮播图 `carousel`、封面等）。
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
    ├── FileService / FileServiceImpl
    ├── EmailService / EmailServiceImpl
    └── QRCodeService / QRCodeServiceImpl
```
> 本模块自包含，**无 domain 层**；作为底层能力被上层业务模块经 `api`/`application` 接口注入使用。

## 主要接口
| 路径 | 说明 |
|---|---|
| `POST /admin/files` | 本地上传（`@RequestParam file`），返回 `/uploads[/子目录]/<uuid>.<ext>` |
| `POST /admin/files/oss` | 上传到阿里云 OSS |
| `GET /app/qr-code` | 生成二维码 |

## 依赖与约束
- 只依赖 `cas-framework`，**不依赖任何业务模块**（依赖图最底层之一）。
- 上传目录可配置：`file.upload.dir=uploads`、`file.upload.url-prefix=/uploads`。
- UUID 文件名天然避免重名覆盖；删除/清理由调用方（如轮播图管理）决定，本模块不自动删文件。
