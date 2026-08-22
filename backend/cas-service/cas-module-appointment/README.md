# README.md - cas-module-appointment

## 模块职责

cas-module-appointment 是预约业务模块，提供服务预约等业务功能。

## 核心功能

- **服务管理**: 获取可用的服务列表
- **预约功能**: 用户预约服务

## 目录结构

采用 DDD 四层架构：

```
cas-module-appointment/
├── interfaces/                    # 接口层
│   └── controller/              # REST控制器
│
├── application/                   # 应用层
│   └── service/                 # 应用服务
│
├── domain/                      # 领域层
│   ├── entity/                  # 实体
│   └── repository/              # 仓储接口
│
├── infrastructure/              # 基础设施层
│   └── persistence/             # 持久化（Mapper, DO）
│
└── api/                        # 跨模块API
```

## 核心类说明

| 类名 | 包路径 | 说明 |
|------|--------|------|
| ServiceController | com.laoliu.cas.appointment.interfaces.controller.admin | 服务REST接口 |
| ServicesService | com.laoliu.cas.appointment.application.service | 服务应用服务接口 |
| ServicesServiceImpl | com.laoliu.cas.appointment.application.service.impl | 服务应用服务实现 |

## 依赖关系

```
依赖层级:
cas-server ─────────────────────────────────────────┐
                                                   │
cas-module-appointment ──> cas-module-system ───────┤
                         ──> cas-module-infra ───────┤
                                                   ├──> cas-framework
cas-thirdparty-aliyun ─────────────────────────────┘
```

## API接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /service | GET | 获取所有可用服务 |
