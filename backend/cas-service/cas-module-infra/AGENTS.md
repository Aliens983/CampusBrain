# AGENTS.md — cas-module-infra

基础设施模块：本地文件上传、邮件、二维码；无 domain 层，自包含。权威说明见上层 `../CLAUDE.md`。

## 文件清单（com.laoliu.cas.infra）

```
interfaces/
├── controller/admin/  FileAdminController（@RequestMapping("/admin/files")，POST 本地上传）
│                      OSSAdminController（/admin/files，POST /oss → 阿里云 OSS）
├── controller/app/    QRCodeAppController（GET /app/qr-code）
└── dto/request/       FileUploadReqVO
application/service/  FileService(Impl)、EmailService(Impl)、QRCodeService(Impl)
```

## 服务说明

### FileService
- 本地落盘：`file.upload.dir`（=uploads），Tomcat `transferTo` 用**绝对路径**规避临时目录问题。
- UUID 重命名，支持子目录分类（captcha / carousel / 封面…），返回相对 URL（`/uploads/...`）。
- 两个重载：MultipartFile 与 File（后者供验证码/二维码临时文件上传）。

### EmailService
- `sendEmail(to, subject, content)`，`@Async` + SimpleMailMessage；发件人取 `spring.mail.username`。
- SMTP 163，SSL 465；异常转 `BusinessException(CommonErrorCode.EMAIL_SEND_FAILED)`。

### QRCodeService
- Hutool `QrCodeUtil.generate(content,300,300)` → 临时 PNG → **经本地 FileService 落盘**（不再依赖 OSS），
  返回 `serverAddress + contextPath + /uploads/...` 完整 URL； finally 删除临时文件。

## 接口

| 路径 | 说明 |
|---|---|
| `POST /admin/files` | 本地上传，返回 `/uploads[/子目录]/<uuid>.<ext>` |
| `POST /admin/files/oss` | 上传到阿里云 OSS（经 thirdparty OSSService，可选） |
| `GET /app/qr-code` | 生成二维码图片 URL |

## 已知限制

- 上传不做文件类型/大小白名单校验（大小受 multipart 全局 50MB 限制）；无下载/流式接口。
- 文件删除/清理由调用方负责，本模块不自动删。

## 依赖与对外 API

- 依赖 `cas-framework`、`cas-thirdparty`（仅 OSSAdminController 用 OSSService）。
- 不依赖 system / appointment。
- 对外：`EmailService`（system 认证、appointment 审核通知）、`FileService`（system 验证码、appointment 轮播/封面）、`QRCodeService`（/app/qr-code）。

## 测试

2 个测试类 / 5 个 `@Test`：QRCodeServiceImplTest 3、EmailServiceImplTest 2。
