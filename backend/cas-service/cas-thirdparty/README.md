# README.md - cas-thirdparty-aliyun

## 模块职责

cas-thirdparty-aliyun 是第三方集成模块，隔离阿里云等第三方系统接口调用，保持业务模块的独立性。

## 核心功能

- **短信服务**: 阿里云SMS短信发送
- **配置隔离**: 第三方配置集中管理

## 目录结构

```
cas-thirdparty-aliyun/
├── service/                      # 服务接口和实现
│   ├── SmsService.java         # 短信服务接口
│   └── impl/                   # 实现类
└── config/                      # 配置类
```

## 核心类说明

| 类名 | 包路径 | 说明 |
|------|--------|------|
| SmsService | com.laoliu.cas.thirdparty.aliyun.service | 短信服务接口 |
| SmsServiceImpl | com.laoliu.cas.thirdparty.aliyun.service.impl | 短信服务实现 |

## 依赖关系

```
cas-module-system ───────────────────────────────────────┐
                                                         │
cas-thirdparty-aliyun ─────────────────────────> cas-framework
```

## 配置示例

```yaml
aliyun:
  sms:
    region-id: cn-hangzhou
    domain: dysysmsapi.aliyuncs.com
    access-key-id: your-access-key-id
    access-key-secret: your-access-key-secret
```

## 注意事项

1. 本模块保持轻量，只依赖 cas-common
2. 不依赖任何业务模块
3. 配置敏感信息时建议使用加密或环境变量
