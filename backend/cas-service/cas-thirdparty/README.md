# cas-thirdparty — 第三方集成模块

隔离外部 API（天气、AI 大模型、阿里云 OSS/SMS），保持业务模块与外部系统解耦。仅依赖 `cas-common`，不依赖任何业务模块。

## 核心功能
- **天气查询**：`GET /weather/local`（RestTemplate → cn.apihz.cn），按经纬度/城市返回天气。
- **AI 对话**：`POST /ai/chat`（WebClient → DashScope/Qwen，`qwen.api-key`）；预留 DeepSeek 配置。
- **阿里云 OSS / SMS**：`OSSService`（对象存储，供 infra 的 `/admin/files/oss` 调用）、`SmsService`（短信）。
- **对话历史**：`ai_chat_history` 持久化（entity/repository/mapper）。

## 目录结构
```
com.laoliu.cas.thirdparty
├── config/             # AliyunConfig、QwenConfig、DeepSeekConfig
├── controller/         # WeatherController（/weather）、CallTheModelController（/ai）
├── service/            # WeatherApi、CallModelService、OSSService、SmsService（+ impl）
├── dto/                # 请求/响应 DTO
├── domain/entity + repository/        # AiChatHistory
└── infrastructure/persistence/        # AiChatHistoryDO / Mapper / RepositoryImpl
```

## 主要接口
| 路径 | 说明 |
|---|---|
| `GET /weather/local` | 天气查询（需 `WEATHER_API_ID/KEY`） |
| `POST /ai/chat` | AI 对话（需 Qwen/DeepSeek Key） |

## 配置
外部凭证经环境变量注入（`WEATHER_API_*`、`DEEPSEEK_API_KEY`、`QWEN_API_KEY`、`ALIYUN_OSS_*`、`ALIYUN_SMS_*`），`application.yml` 不含明文密钥；未配置对应 Key 时功能降级（不影响预约主流程）。
