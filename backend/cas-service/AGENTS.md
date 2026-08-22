# AGENTS.md — cas-service（校园预约系统后端）

> **当前状态（2026-08-21）**：本服务已并入微服务架构（见 `../README.md`）。完整后端指南见 **`CLAUDE.md`**（顶部含最新状态摘要）。

## 快速概览

- **模块**：7 个 Maven 子模块（cas-dependencies BOM → cas-framework 6 starters → infra/thirdparty → system → appointment → server 入口）。
- **端口**：18080，context-path `/api/v1`，服务注册名 `cas-service`。
- **技术栈**：Java 17 · Spring Boot 3.3.5 · MyBatis-Plus · Spring Cloud Alibaba（Nacos 注册/配置 + Sentinel 限流）。
- **测试**：65 个单测全绿（`mvn clean test`）。

## 常用命令

```bash
# 构建（含依赖）
mvn clean package -DskipTests

# 单测
mvn clean test

# 运行（需先 export DB_PASSWORD 等环境变量，见 ../README.md）
java -jar cas-server/target/cas-server-1.0.0.jar
```

## 关键约定

- **认证**：网关统一 JWT 鉴权；本服务通过 `SecurityFrameworkUtils` 取当前用户，`@RequireRole` 做细粒度授权；内网签名由 `InternalAuthFilter` 校验（5 分钟时间戳新鲜度）。
- **跨模块**：只能通过 `api/` 接口，禁止直接调他模块 Mapper/Service。
- **DDD 分层**：interfaces / application / domain / infrastructure / api。
- **响应**：统一 `CommonResult<T>`；业务异常抛 `BusinessException`，由 `GlobalExceptionHandler` 处理。

> 子模块目录下的 `AGENTS.md` / `README.md` 为重构前历史快照，若与代码冲突**以代码和本 CLAUDE.md 为准**。
