# AGENTS.md — cas-service（校园预约系统后端）

> **当前状态（2026-09-11 核对）**：本服务是微服务三件套之一（另有 gateway、kb-service），整体见 `../README.md`。给 AI 助手的完整权威指南见 **`CLAUDE.md`**（模块、接口、约定、已知限制均与代码同步）。

## 快速概览

- **模块**：7 个 Maven 子模块（cas-dependencies BOM → cas-framework 6 starters → infra/thirdparty → system → appointment → server 入口）。
- **端口**：18080，context-path `/api/v1`，Nacos 注册名 `cas-service`。
- **技术栈**：Java 17 · Spring Boot 3.3.5 · Spring Cloud Alibaba 2023.0.1.2（Nacos 注册/配置 + Sentinel）· MyBatis-Plus · RabbitMQ · Flyway（V1~V5）。
- **测试**：82 个 `@Test`（13 个测试类）全绿（`mvn -B -pl cas-service -am test`）。

## 常用命令

```bash
# 构建（含依赖）
mvn -pl cas-service/cas-server -am clean package -DskipTests

# 单测
mvn -B -pl cas-service -am test

# 运行（先在 backend/ 配好 .env，可用 ./scripts/run-local.sh cas）
java -jar cas-server/target/cas-server-1.0.0.jar
```

## 关键约定

- **认证**：JWT 由 gateway 统一验签并透传身份头；本服务通过 `SecurityFrameworkUtils` 取当前用户，`@RequireRole` 做细粒度授权；服务间调用由 `InternalAuthFilter` 校验 `X-Internal-Sign`（时间戳新鲜度）。
- **角色**：0 普通用户 / 1 管理员 / 2 超管 / 3 教师；超管全放行，教师可访问开放给 USER 的接口，教师专属接口须显式列 TEACHER。
- **跨模块**：只能通过 `api/` 接口，禁止直接调他模块 Mapper/Service。
- **DDD 分层**：interfaces / application / domain / infrastructure（+ api）；`domain/` 零框架注解。
- **响应**：统一 `CommonResult<T>`；业务异常抛 `BusinessException`，由 `GlobalExceptionHandler` 兜底。
- **下线红线**：Qwen `/ai/chat`、CallTheModel*、AiChatHistory*、`ai_chat_history` 表已于 2026-09-07 删除，勿恢复；AI 对话只在 kb-service。

> 子模块目录下的 `AGENTS.md` 为当前结构的精简快照，如与代码冲突，**以代码和本目录 `CLAUDE.md` 为准**。
