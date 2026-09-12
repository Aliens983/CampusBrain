# AGENTS.md — cas-thirdparty

第三方集成模块：天气、阿里云 OSS、阿里云短信。隔离外部系统，仅依赖 cas-common/framework。权威说明见上层 `../CLAUDE.md`。

> ⚠️ **AI 对话链已整体下线（2026-09-07）**：CallTheModelController、CallModelService(Impl)、
> ChatReqVO/ChatRespVO、AiChatHistory（entity/DO/mapper/repository）、`ai_chat_history` 表均已删除。
> 原孤儿配置类 `infrastructure/config/QwenConfig.java`、`DeepSeekConfig.java` 也已于 **2026-09-12 删除**。
> AI 对话唯一入口在 kb-service，**勿据残留引用恢复 AI 功能**。

## 文件清单（com.laoliu.cas.thirdparty）

```
interfaces/
├── controller/      WeatherController（@RequestMapping("/weather")：GET 根、GET /local）
└── dto/response/    WeatherResponse
api/               WeatherApi（接口）+ impl/WeatherApiImpl（RestTemplate → cn.apihz.cn）
application/service/  OSSService(Impl)、SmsService(Impl)
infrastructure/config/ AliyunConfig（短信 Client bean）、OSSConfig（@ConfigurationProperties(prefix=aliyun.oss)）
                        （原 QwenConfig、DeepSeekConfig 孤儿配置类已于 2026-09-12 删除）
```

## 服务说明

- **WeatherApi**：RestTemplate 调 `https://cn.apihz.cn/api/tianqi/tqyb.php`，凭证 `weather.api.id/key`（环境变量）。
- **OSSService**：阿里云 OSS 上传，`OSSServiceImpl` 用 `@Value` 读 `aliyun.oss.*`（endpoint/ak/sk/bucket=coding-king-liu），
  返回 `https://{bucket}.{endpoint}/{uuid文件名}`；注意 `OSSConfig` 这个 @ConfigurationProperties 类并未被它使用。
- **SmsService**：包装 Aliyun Dysmsapi `sendSms`，Client bean 来自 AliyunConfig；当前无业务流程调用短信。

## 配置（application.yml，均环境变量化，无明文密钥）

`WEATHER_API_ID` / `WEATHER_API_KEY`、`DEEPSEEK_API_KEY`（预留无消费方）、`QWEN_API_KEY`（预留）、
`ALIYUN_OSS_ACCESS_KEY_ID/_SECRET`、`ALIYUN_SMS_ACCESS_KEY_ID/_SECRET`。未配置时对应能力降级，不影响预约主流程。

## 已知限制

1. ~~QwenConfig / DeepSeekConfig 孤儿配置，建议删除。~~ ✅ 已于 2026-09-12 删除；`QWEN_API_KEY` / `DEEPSEEK_API_KEY` 现无任何消费方，属历史遗留。
2. OSS 凭证默认空串，不配置则 `/admin/files/oss` 不可用。
3. OSSConfig 定义了但 OSSServiceImpl 实际用 @Value。
4. SMS 无业务调用方。

## 依赖与对外 API

- 仅依赖 `cas-common`（及 aliyun-oss / dysmsapi SDK），不依赖任何业务模块。
- 对外：`OSSService`（infra 的 OSSAdminController）、`WeatherApi`（WeatherController）；`SmsService` 暂无消费方。

## 测试

2 个测试类 / 6 个 `@Test`：WeatherApiImplTest 4、SmsServiceImplTest 2。
