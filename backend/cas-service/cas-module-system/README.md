# README.md - cas-module-system

## 模块职责

cas-module-system 是系统管理模块，提供用户、角色、权限等系统管理功能。

## 核心功能

- **用户管理**: 用户注册、登录、信息查询
- **权限控制**: 基于角色的访问控制
- **认证授权**: JWT Token 认证

## 目录结构

采用 DDD 四层架构：

```
cas-module-system/
├── interfaces/                    # 接口层
│   ├── controller/               # REST控制器
│   ├── dto/                      # 数据传输对象
│   └── assembler/                # DTO转换器
│
├── application/                   # 应用层
│   └── service/                  # 应用服务
│
├── domain/                       # 领域层
│   ├── entity/                   # 实体
│   └── repository/               # 仓储接口
│
├── infrastructure/               # 基础设施层
│   └── persistence/              # 持久化（Mapper, DO）
│
└── api/                         # 跨模块API
```

## 核心类说明

| 类名 | 包路径 | 说明 |
|------|--------|------|
| UserController | com.laoliu.cas.system.interfaces.controller.admin | 用户REST接口 |
| UserService | com.laoliu.cas.system.application.service | 用户服务接口 |
| UserServiceImpl | com.laoliu.cas.system.application.service.impl | 用户服务实现 |
| UserMapper | com.laoliu.cas.system.infrastructure.persistence.mapper | MyBatis Mapper |
| GetUserIdViaTokenApi | com.laoliu.cas.system.api | 跨模块获取用户ID接口 |

## 依赖关系

```
依赖层级:
cas-module-appointment ──────────────────┐
                                        │
cas-module-system ──> cas-module-infra ─┤
                                        ├──> cas-framework
cas-thirdparty-aliyun ──────────────────┘
```

## 使用示例

### 引入依赖

```xml
<dependency>
    <groupId>com.laoliu</groupId>
    <artifactId>cas-module-system</artifactId>
    <version>1.0.0</version>
</dependency>
```

### API接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /user | GET | 获取当前用户信息 |
| /user/all_users | GET | 获取所有用户（需管理员权限） |
| /user/create | POST | 创建用户（需超级管理员权限） |
| /user/get_all_bookings | GET | 获取用户预约信息 |
