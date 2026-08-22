# README.md - cas-module-infra

## 模块职责

cas-module-infra 是基础设施模块，为业务模块提供通用基础设施服务。

## 核心功能

- **文件服务**: 提供文件上传、存储管理功能
- **基础设施**: 为其他业务模块提供通用基础设施能力

## 目录结构

采用 DDD 四层架构：

```
cas-module-infra/
├── interfaces/                    # 接口层
│   ├── controller/               # REST控制器
│   └── dto/                      # 数据传输对象
│
├── application/                   # 应用层
│   └── service/                  # 应用服务接口及实现
│
├── domain/                       # 领域层
│   ├── entity/                   # 实体
│   └── repository/               # 仓储接口
│
├── infrastructure/              # 基础设施层
│   ├── persistence/              # 持久化
│   └── external/                # 外部服务调用
│
└── api/                         # 跨模块API
```

## 核心类说明

| 类名 | 包路径 | 说明 |
|------|--------|------|
| FileController | com.laoliu.cas.infra.interfaces.controller | 文件上传REST接口 |
| FileService | com.laoliu.cas.infra.application.service | 文件服务接口 |
| FileServiceImpl | com.laoliu.cas.infra.application.service.impl | 文件服务实现 |
| FileUploadReqVO | com.laoliu.cas.infra.interfaces.dto.request | 文件上传请求对象 |

## 依赖关系

```
依赖层级（从上到下）:
cas-module-system ─────────────┐
cas-module-appointment ───────┤
                               ├──> cas-module-infra ──> cas-framework
cas-thirdparty-aliyun ────────┘
```

## 使用示例

### 引入依赖

```xml
<dependency>
    <groupId>com.laoliu</groupId>
    <artifactId>cas-module-infra</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 配置文件

```yaml
file:
  upload:
    dir: uploads
    url-prefix: /uploads
```

## 注意事项

1. 本模块只依赖 framework 层，不依赖任何业务模块
2. 文件上传默认保存在 `uploads` 目录
3. 采用 UUID 生成文件名，避免文件覆盖
