# AGENTS.md — cas-module-infra

Infrastructure module: file upload (local), QR code generation, email sending. Serves as the technical foundation for business modules.

## Complete File List

```
com.laoliu.cas.infra
├── interfaces/
│   ├── controller/admin/
│   │   ├── FileAdminController.java   ← POST /admin/file/upload
│   │   └── OSSAdminController.java    ← POST /admin/oss/upload
│   └── dto/
│       └── FileUploadReqVO.java       ← ONLY DTO with @NotNull Bean Validation
├── application/service/
│   ├── EmailService.java              ← sendEmail(to, subject, content) — @Async
│   ├── FileService.java               ← uploadFile(MultipartFile) / uploadFile(File)
│   ├── QRCodeService.java             ← generateQRCode(text) → OSS URL
│   └── impl/
│       ├── EmailServiceImpl.java      ← JavaMailSender, reads from spring.mail.username
│       ├── FileServiceImpl.java       ← saves to ./uploads/, UUID rename, returns URL
│       └── QRCodeServiceImpl.java     ← Hutool QrCodeUtil → BufferedImage → byte[] → OSS
```

## Service Details

### EmailService
- Interface: `sendEmail(String to, String subject, String content)`
- Implementation: `@Async`, `SimpleMailMessage`, reads sender from `spring.mail.username` property
- Error handling: catches exception → logs → throws `BusinessException(CommonErrorCode.EMAIL_SEND_FAILED)`
- SMTP: 163.com, SSL port 465

### FileService
- Local filesystem storage at `file.upload.dir` (default `./uploads/`)
- UUID-based renaming: `UUID.randomUUID().toString() + extension`
- Returns URL: `file.upload.url-prefix` + filename
- Two overloads: one takes `MultipartFile`, one takes `File` (used by CaptchaService)
- Handles extension extraction from original filename

### QRCodeService
- Generates QR code image via Hutool `QrCodeUtil.generate()`
- Converts `BufferedImage` → PNG `byte[]`
- Wraps in custom `ByteArrayMultipartFile` (inline class implementing `MultipartFile`)
- Uploads to OSS via `OSSService.uploadFile()` from cas-thirdparty
- **Dependency concern**: QR generation will fail if OSS is not configured

## Known Issues
1. **No file type/size validation** — any file type accepted
2. **No download endpoint** — only upload, no streaming/download
3. **QR code depends on OSS** — tightly coupled to Aliyun OSS
4. **No infra-level domain entities or repositories** — simpler than business modules

## Dependencies
- Depends: `cas-framework`, `cas-thirdparty` (for OSS in QRCodeService)
- Does NOT depend: `cas-module-system`, `cas-module-appointment`

## Cross-Module APIs Provided
- `EmailService` → used by system (AuthService, EmailVerificationService) and appointment (ServiceStatusService)
- `FileService` → used by system (CaptchaService)
- `QRCodeService` → not currently consumed by other modules
