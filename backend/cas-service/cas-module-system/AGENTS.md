# AGENTS.md — cas-module-system

System management module: user CRUD, authentication (login/register/reset-password), role management, captcha, email verification.

## Complete File List

```
com.laoliu.cas.system
├── interfaces/
│   ├── assembler/
│   │   └── UserAssembler.java              ← User entity → UserResponse DTO
│   ├── controller/
│   │   ├── admin/
│   │   │   ├── EmailAdminController.java   ← admin email endpoints
│   │   │   ├── RoleAdminController.java    ← role query + change
│   │   │   └── UserController.java         ← user CRUD (admin)
│   │   ├── app/
│   │   │   └── EmailController.java        ← Bridge: POST /email
│   │   ├── GraphicController.java          ← Bridge: GET /graphic/get (CAPTCHA)
│   │   ├── GraphicVerificationAppController.java ← DUPLICATE of GraphicController
│   │   ├── LoginAppController.java
│   │   ├── LoginController.java            ← Bridge: POST /login
│   │   ├── RegisterAppController.java
│   │   └── RegisterController.java         ← Bridge: POST /register/verify-code
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
│   └── vo/
│       ├── CaptchaResult.java
│       ├── UserRegisterVO.java
│       └── VerifyCodeReqVO.java
├── application/service/
│   ├── AuthService.java                    ← login(), register(), resetPassword()
│   ├── CaptchaService.java                 ← createCaptcha(uuid)
│   ├── EmailVerificationService.java       ← sendVerificationCode(email)
│   ├── RoleService.java                    ← changeRoleById(userId, role)
│   ├── UserService.java                    ← getUserById(), getAllUsers()
│   └── impl/
│       ├── AuthServiceImpl.java            ← STRONGEST business logic in codebase
│       ├── CaptchaServiceImpl.java         ← Hutool ShearCaptcha + MathGenerator
│       ├── EmailVerificationServiceImpl.java ← Redis rate limit + code generation
│       ├── RoleServiceImpl.java            ← BUG: role toggle is inverted
│       └── UserServiceImpl.java
├── domain/
│   ├── entity/User.java                    ← ANEMIC — no behavior methods
│   └── repository/UserRepository.java
├── infrastructure/
│   ├── aspect/RoleAspect.java              ← @RequireRole interceptor (HAS ISSUES)
│   └── persistence/
│       ├── dataobject/UserDO.java
│       ├── mapper/UserMapper.java
│       └── repository/UserRepositoryImpl.java
└── api/
    ├── UserInfoApi.java                    ← getUserById(Long) → UserInfoDTO
    ├── UserInfoApiImpl.java
    └── dto/UserInfoDTO.java
```

## Key Business Logic

### AuthService (most complex service in the project)
- `login(email, password)` → find by email → BCrypt verify → generate JWT → return LoginUser
- `register(request)` → validate email code from Redis → check uniqueness → encode password → save user → delete Redis key → return JWT
- `resetPassword(email, code, newPassword)` → verify code → update password → return new JWT

### EmailVerificationService
- Generates 6-digit code → stores in Redis `verification_code:{email}` (TTL 300s)
- Rate limiting: Redis key `rate_limit:email:{email}` (TTL 60s)
- Calls `EmailService.sendEmail()` (@Async)

### CaptchaService
- Math captcha: Hutool `ShearCaptcha` + `MathGenerator` → creates expression image
- Evaluates answer via `Calculator.conversion()`
- Stores answer in Redis `captcha:{uuid}` (TTL 300s)
- Writes image to temp file → upload via FileService → delete temp file

## Known Issues
1. **Role toggle bug** — `RoleServiceImpl.changeRoleById()`: switching to "普通用户" sets role=1 (ADMIN) in SQL
2. **RoleAspect** — writes manual JSON to HttpServletResponse instead of throwing exceptions (bypasses GlobalExceptionHandler)
3. **Duplicate controllers** — LoginController/LoginAppController, RegisterController/RegisterAppController, GraphicController/GraphicVerificationAppController are functional duplicates
4. **No Bean Validation** — all request DTOs lack `@NotBlank`/`@Email` etc.
5. **User entity is anemic** — no `changePassword()`, `hasRole()`, `updateProfile()` methods

## Dependencies
- Depends: `cas-module-infra`, `cas-thirdparty`, `cas-framework`
- Does NOT depend: `cas-module-appointment`

## Cross-Module APIs Provided
- `UserInfoApi` → used by appointment module to get user details
- `GetUserIdViaTokenApi` → used internally and by RoleAspect

## Database
- Table: `user` (id, name, grade, sex, age, email, password, role)
- Mapper XML: `cas-module-system/src/main/resources/mapper/UserMapper.xml`
