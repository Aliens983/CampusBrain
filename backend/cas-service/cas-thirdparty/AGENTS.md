# AGENTS.md — cas-thirdparty

Third-party integration module: AI chat (Qwen/DeepSeek), weather API, Aliyun OSS, Aliyun SMS. Isolates external service calls from business logic.

## Complete File List

```
com.laoliu.cas.thirdparty
├── config/
│   ├── AliyunConfig.java              ← SMS Client bean (Dysmsapi)
│   ├── DeepSeekConfig.java            ← DEAD CODE — never used
│   └── QwenConfig.java                ← WebClient bean for DashScope API
├── controller/
│   ├── CallTheModelController.java     ← POST /callTheLargeModel (public, no auth)
│   └── WeatherController.java          ← GET /weather
├── dto/
│   ├── ChatReqVO.java                  ← model, message fields
│   ├── ChatRespVO.java                 ← aiResponse, responseTimeMs fields
│   └── WeatherResponse.java            ← city, temp, weather, wind etc.
├── service/
│   ├── CallModelService.java / impl/CallModelServiceImpl.java
│   │   → WebClient → Qwen DashScope API → parse JSON → persist AiChatHistory
│   ├── OSSService.java / impl/OSSServiceImpl.java
│   │   → Aliyun OSS SDK → upload MultipartFile → return URL
│   ├── SmsService.java / impl/SmsServiceImpl.java
│   │   → Aliyun Dysmsapi Client → sendSms(templateCode, phone, params)
│   └── WeatherApi.java / impl/WeatherApiImpl.java
│       → RestTemplate → cn.apihz.cn API → return WeatherResponse
├── domain/
│   ├── entity/AiChatHistory.java       ← ANEMIC entity
│   └── repository/AiChatHistoryRepository.java
└── infrastructure/persistence/
    ├── dataobject/AiChatHistoryDO.java
    ├── mapper/AiChatHistoryMapper.java
    └── repository/AiChatHistoryRepositoryImpl.java
```

## Service Details

### CallModelService (AI Chat)
- Calls Qwen DashScope API via `WebClient` (blocking mode: `.block()`)
- Constructs messages array: system prompt + user message
- Response parsing: `ObjectMapper` → extract `output.text`
- Persists chat history to `ai_chat_history` table (userId, model, userMessage, aiResponse, responseTimeMs)
- **Note**: Chat history is persisted but never exposed via any query API
- **DeepSeekConfig** exists but no service implementation uses it — dead code

### WeatherApi
- Calls `cn.apihz.cn` weather API via `RestTemplate.getForObject()`
- API key/ID configured in `application.yml`
- Builds query string manually, validates response code=200
- Returns `WeatherResponse` DTO

### OSSService
- Uploads files to Aliyun OSS
- Config: `aliyun.oss.*` properties (endpoint, accessKeyId, accessKeySecret, bucketName)
- **Credentials are placeholder values** (`your-access-key-id`) — not actually usable
- Returns `https://{bucket}.{endpoint}/{filename}`
- Proper `finally` block to shutdown `OSSClient`
- **Note**: `OSSConfig.java` is a `@ConfigurationProperties` class but never injected — `OSSServiceImpl` reads `@Value` directly

### SmsService
- Wraps Aliyun Dysmsapi `sendSms()`
- Uses auto-configured `Client` bean from `AliyunConfig`
- Very thin wrapper — single API call

## Known Issues
1. **DeepSeekConfig is dead code** — never injected or called
2. **OSS credentials are placeholders** — `your-access-key-id` values
3. **OSSConfig not used** — `@ConfigurationProperties` class exists but `@Value` used instead
4. **AiChatHistory no read API** — data persisted but no endpoint to query history
5. **WebClient blocking** — `CallModelServiceImpl` uses `.block()` instead of reactive
6. **SMS never called** — no business flow triggers SMS sending

## Dependencies
- Depends: `cas-common` only (plus SDK jars: aliyun-oss, aliyun-dysmsapi)
- Does NOT depend: any business module or infra module
- **Note**: Actually depends on `cas-spring-boot-starter-mybatis` at runtime for AiChatHistory persistence — this is a dependency direction concern

## Mapper XML
- `AiChatHistoryMapper.xml` — INSERT chat history

## Cross-Module APIs Provided
- `OSSService` → used by infra (QRCodeServiceImpl, OSSAdminController)
- `SmsService` → not currently consumed
- `WeatherApi` → consumed by WeatherController
- `CallModelService` → consumed by CallTheModelController
