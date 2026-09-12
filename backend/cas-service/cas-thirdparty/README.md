# cas-thirdparty — 第三方集成模块

隔离外部 API（天气、阿里云 OSS / SMS），保持业务模块与外部系统解耦。仅依赖 `cas-common`/框架，不依赖任何业务模块。

> **AI 对话能力已下线（2026-09-07）**：旧的 Qwen `/ai/chat`（CallTheModelController/CallModelService/ChatReqVO/RespVO）
> 及 `ai_chat_history` 整套（entity/DO/mapper/repository/表）均已删除。平台 AI 对话统一由 **kb-service** 的 RAG 问答承担
> （会话存 KB `conversation` 表）。本模块仅保留 `QwenConfig` / `DeepSeekConfig` 两个**无消费方的孤儿配置类**，可留可删。

## 核心功能
- **天气查询**：`GET /weather`、`GET /weather/local`（RestTemplate → cn.apihz.cn）。
- **阿里云 OSS**：`OSSService`（对象存储，供 infra 的 `POST /admin/files/oss` 调用）。
- **阿里云短信**：`SmsService`（Dysmsapi 发送，当前无业务调用方）。

## 目录结构
```
com.laoliu.cas.thirdparty
├── interfaces/controller/WeatherController（/weather、/weather/local）
├── interfaces/dto/response/WeatherResponse
├── api/WeatherApi + impl/WeatherApiImpl
├── application/service/  OSSService(Impl)、SmsService(Impl)
└── infrastructure/config/ AliyunConfig、OSSConfig、QwenConfig、DeepSeekConfig（后两者孤儿）
```

## 主要接口
| 路径 | 说明 |
|---|---|
| `GET /weather`、`GET /weather/local` | 天气查询（需 `WEATHER_API_ID/KEY`） |

> OSS / SMS 无独立 Controller，由 `cas-module-infra` 的 `OSSAdminController` 等内部调用。

## 配置
外部凭证经环境变量注入（`WEATHER_API_*`、`ALIYUN_OSS_*`、`ALIYUN_SMS_*`、`DEEPSEEK_API_KEY`/`QWEN_API_KEY` 仅预留），
`application.yml` 不含明文密钥；未配置对应 Key 时功能降级（不影响预约主流程）。

## 测试
WeatherApiImplTest（4）、SmsServiceImplTest（2），共 6 个 `@Test`。
