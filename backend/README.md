# 后端 — 智汇校园 · CampusBrain

校园预约系统（CAS）+ 知识库问答平台（KB）合体为 Spring Cloud Alibaba 微服务。

## 架构

```
frontend(统一前端) → gateway(8888，唯一入口)
                        ├─ /api/v1/**      → cas-service (18080)
                        └─ /api/v1/kb/**   → kb-service (8081)
                     Nacos(8848/9848) 注册中心 + 配置中心
                     Sentinel 限流（Nacos 动态规则）
```

| 模块 | 说明 | 端口 |
|---|---|---|
| `common-auth` | 共享认证：JWT + 内网签名（含时间戳新鲜度） | — |
| `gateway` | Spring Cloud Gateway，统一 JWT 鉴权 + 路由 | 8888（本地）/ 80（docker） |
| `cas-service` | 校园预约系统（7 个 Maven 子模块，DDD 分层） | 18080 |
| `kb-service` | 知识库问答平台（扁平化单模块） | 8081 |

## 依赖中间件

| 组件 | 地址 | 说明 |
|---|---|---|
| Nacos | localhost:8848 / 9848 | 服务注册 + 配置中心 |
| MySQL（CAS） | localhost:3306 | 宿主机，库 `cas_db` |
| Redis（CAS） | localhost:6379 | 宿主机 |
| MySQL（KB） | localhost:3307 | 库 `knowledge_base` |
| Redis（KB） | localhost:6380 | |
| ES | localhost:9200 | |
| Qdrant | localhost:6334 | |
| RabbitMQ | localhost:5672 | admin/admin123 |
| MinIO | localhost:9000 | minioadmin/minioadmin123 |

## 环境变量（必须）

> 敏感配置已通过环境变量注入（Docker 部署不含明文真实凭据），以下变量缺失会导致服务无法启动或功能异常。

| 变量 | 用途 | 默认 |
|---|---|---|
| `DB_PASSWORD` | CAS 数据库密码 | 空（必填） |
| `SMTP_USERNAME` / `SMTP_PASSWORD` | 邮件发送 | 空 |
| `JWT_SECRET` | JWT 签名密钥 | 内置示例（生产必改） |
| `INTERNAL_SIGN_SECRET` | 内网签名密钥 | 内置示例（生产必改） |
| `WEATHER_API_ID` / `WEATHER_API_KEY` | 天气 API | 空 |
| `DEEPSEEK_API_KEY` | DeepSeek 大模型 | 空 |
| `ALIYUN_OSS_ACCESS_KEY_ID` / `_SECRET` | 阿里云 OSS | 空 |

## 启动步骤

### 1. 启动基础设施

```bash
docker compose up -d          # Nacos + KB 中间件
# CAS 的 MySQL/Redis 用宿主机已有实例，确保 cas_db 已建表
```

### 2. 构建

```bash
mvn clean package -DskipTests
```

### 3. 导出环境变量并启动三个服务

```bash
export DB_PASSWORD='<你的CAS数据库密码>'
export SMTP_USERNAME='...' SMTP_PASSWORD='...'
export JWT_SECRET='<32字节以上随机串>' INTERNAL_SIGN_SECRET='<随机串>'

# 终端 1: CAS
java -jar cas-service/cas-server/target/cas-server-1.0.0.jar

# 终端 2: KB
java -jar kb-service/target/kb-service-1.0.0.jar

# 终端 3: 网关（本地用 8888，避免与 MinIO 9000 冲突）
GATEWAY_PORT=8888 java -jar gateway/target/gateway-1.0.0.jar
```

### 4. 验证

```bash
# Nacos 服务列表：应含 gateway / cas-service / kb-service
curl http://localhost:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=10

# 网关路由 CAS / KB（应 200）
curl http://localhost:8888/api/v1/captcha
curl http://localhost:8888/api/v1/kb/health

# Nacos 配置中心热更新（CAS 启动后访问）
curl http://localhost:18080/api/v1/config-demo/greeting

# Sentinel 限流演示（快速连点会触发限流）
curl http://localhost:18080/api/v1/sentinel-demo/limited
```

## 关键能力

- **统一 JWT 网关鉴权**：网关验签 JWT、透传身份头 + 内网签名（5 分钟时间戳新鲜度），服务细粒度授权。
- **KB 智能助手**：问答按「本地资料优先 + DeepSeek 兜底」路由——本地资料有结果走 RAG（引用资料），无结果或 RAG 无法回答则 DeepSeek 直接回答；**问答缓存已全部禁用**，每次实时检索 + LLM 回答（避免答非所问/历史串题）；预约余量通过 OpenFeign + Nacos 服务发现 + 内网签名直连 CAS 只读接口（`/appointments/availability`），LangChain4j `AppointmentTool`（`@Tool`）+ AiServices 实现 Function Calling（**需配置 `DEEPSEEK_API_KEY` 后演示**）。当前 AI 问答为**单轮问答**（前端每次提问独立会话），无资料路径为 SSE 流式，RAG/预约路径一次性返回。
- **预约乐观锁 + 库存扣减**：services 表含 `capacity`（-1=不限）/`booked_count`；预约时原子条件更新扣减（`WHERE capacity=-1 OR booked_count<capacity`）防并发超卖，容量满返回 `BOOKING_CAPACITY_FULL`；取消/审核拒绝时释放库存（`booked_count-1`），重复预约经幂等 SQL 去重并随事务回滚。
- **RabbitMQ 预约事件**：CAS 发布 `appointment.changed` 事件，KB 已实现监听并接收（当前仅记录日志，索引/缓存更新为 TODO）。
- **Nacos 配置中心**：`cas-service.yaml` 托管配置，提供 `/config-demo` 热更新演示。
- **Sentinel 限流**：已接入并提供 `/sentinel-demo` 演示接口；Nacos 流控规则（`cas-sentinel-flow-rules`）当前为空。
- **安全加固**：注册提权、IDOR、管理端越权、`/ai` 公开、内网签名防重放均已修复。

## 测试

```bash
# 全量测试：134 个测试全部通过（CAS 73 + KB 61），kb-service 用 H2 + MockBean 隔离中间件
cd backend && mvn test
```

## Docker（仅中间件）

`docker-compose.yml` **只编排中间件**（Nacos + KB 的 MySQL/Redis/ES/Qdrant/RabbitMQ/MinIO）。三个业务服务（gateway/cas-service/kb-service）在**本地运行**（IDEA 或 `java -jar`），见「启动步骤」。
