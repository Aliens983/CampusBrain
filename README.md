<div align="center">

# 智汇校园 · CampusBrain

<p align="center">
  <img src="https://readme-typing-svg.demolab.com?font=Fira+Code&weight=700&size=32&duration=2800&pause=1200&color=3B82F6&center=true&vCenter=true&width=780&lines=CampusBrain+%C2%B7+Smart+Campus+Platform;Campus+Appointment+%C3%97+RAG+Knowledge+Q%26A;Spring+Cloud+Alibaba+Microservices;Unified+JWT+Gateway+Auth+%2B+SSO" alt="Typing SVG" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/Spring_Cloud_Alibaba-2023.0.1.2-1677FF?style=for-the-badge&logo=spring&logoColor=white" alt="Spring Cloud Alibaba" />
  <img src="https://img.shields.io/badge/Spring_Cloud_Gateway-3.3-3DDC84?style=for-the-badge&logo=spring&logoColor=white" alt="Gateway" />
  <img src="https://img.shields.io/badge/Nacos-2.3.2-1E88E5?style=for-the-badge&logo=nacos&logoColor=white" alt="Nacos" />
  <img src="https://img.shields.io/badge/Sentinel-1.8-E64A19?style=for-the-badge&logo=sentinel&logoColor=white" alt="Sentinel" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Vue-3.4-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=white" alt="Vue 3" />
  <img src="https://img.shields.io/badge/TypeScript-5.6-3178C6?style=for-the-badge&logo=typescript&logoColor=white" alt="TypeScript" />
  <img src="https://img.shields.io/badge/Element_Plus-2.9-409EFF?style=for-the-badge&logo=element&logoColor=white" alt="Element Plus" />
  <img src="https://img.shields.io/badge/MyBatis_Plus-3.5.5-1E90FF?style=for-the-badge&logo=mybatis&logoColor=white" alt="MyBatis-Plus" />
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL" />
  <img src="https://img.shields.io/badge/Redis-7.0-DC382D?style=for-the-badge&logo=redis&logoColor=white" alt="Redis" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Elasticsearch-8.12-005571?style=for-the-badge&logo=elasticsearch&logoColor=white" alt="Elasticsearch" />
  <img src="https://img.shields.io/badge/Qdrant-1.9-DC244C?style=for-the-badge&logo=qdrant&logoColor=white" alt="Qdrant" />
  <img src="https://img.shields.io/badge/RabbitMQ-3.13-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white" alt="RabbitMQ" />
  <img src="https://img.shields.io/badge/MinIO-latest-C72E49?style=for-the-badge&logo=minio&logoColor=white" alt="MinIO" />
  <img src="https://img.shields.io/badge/Docker-🐳-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=flat-square" alt="License" />
  <img src="https://img.shields.io/badge/Version-1.0.0-blue?style=flat-square" alt="Version" />
  <img src="https://img.shields.io/badge/Architecture-Microservices_·_DDD-ff69b4?style=flat-square" alt="Architecture" />
  <img src="https://img.shields.io/badge/Auth-Gateway_JWT_·_RBAC-green?style=flat-square" alt="Auth" />
  <img src="https://img.shields.io/badge/Retrieval-RAG_Hybrid-9b59b6?style=flat-square" alt="RAG" />
  <img src="https://img.shields.io/badge/Deploy-Docker_Compose-2496ED?style=flat-square" alt="Docker" />
</p>

</div>

> **CampusBrain 校园大脑** —— 基于 Spring Cloud Alibaba 微服务架构的智慧校园平台，将**校园预约系统（CAS）**与**知识库问答平台（KB）**合为一体：统一 Vue 前端、统一网关 JWT 鉴权，KB 作为 CAS 的专属 AI 助手，提供知识库 RAG 问答，并已实现基于 Function Calling 的预约数据实时查询。

---

## 一、功能状态总览（如实）

> 下表为当前代码的真实状态（`mvn test` 125 个测试全绿验证）。**标注 ⚠️ 的项依赖外部配置或尚未端到端验证**。

| 能力 | 状态 | 说明 |
|---|---|---|
| 校园预约（CAS） | ✅ 完整 | 服务/会议室/设备/咨询预约、审核、取消，DDD 分层 |
| 知识库问答（KB） | ✅ 完整 | 文档上传 → 分块 → 向量化 → RAG 检索 → LLM 回答，SSE 流式；**本地资料优先 + 无资料 DeepSeek 兜底；缓存已禁用，每次实时回答** |
| 微服务架构 | ✅ 完整 | Gateway + Nacos 注册/配置 + CAS/KB 两服务，Docker Compose 编排 |
| 网关统一鉴权 | ✅ 完整 | JWT 验签 + 内网签名 `X-Internal-Sign` + 时间戳防重放 |
| 预约实时查询（Function Calling） | ⚠️ 已实现 | Feign + Nacos 直连 CAS + LangChain4j `@Tool`；**需配置 `DEEPSEEK_API_KEY` 并重启两端后演示** |
| RabbitMQ 预约事件 | ⚠️ 部分 | CAS 已发布 `appointment.changed` 事件；KB 收到仅记录日志，**索引/缓存更新为 TODO** |
| 安全修复 | ✅ 已修复 | 注册提权、IDOR、管理端越权、`/ai` 公开均已修复 |
| Nacos 配置中心 | ✅ 已接入 | 提供 `/config-demo` 热更新演示；`cas-service.yaml` 为占位 |
| Sentinel 限流 | ⚠️ 演示 | 已接入 + `/sentinel-demo` 演示接口；Nacos 流控规则当前为空 |

---

## 二、架构总览

```
                    ┌──────────────────────────────────────┐
   浏览器 / 单页应用  │ frontend/ 统一 Vue 前端（CAS + AI 助手）│
                    └─────────────────┬────────────────────┘
                                      │ /api
                    ┌─────────────────▼────────────────────┐
                    │  gateway（Spring Cloud Gateway）      │  ← 唯一入口
                    │  · 统一 JWT 鉴权                       │
                    │  · 透传 X-User-Id / X-User-Role       │
                    │  · 内网签名 X-Internal-Sign + 时间戳    │
                    └───────┬──────────────┬───────────────┘
               /api/v1/**   │              │  /api/v1/kb/**
                    ┌───────▼──────┐  ┌────▼─────────────┐
                    │ cas-service  │  │ kb-service       │
                    │ :18080       │  │ :8081            │
                    │ 预约/用户/审核 │  │ RAG 问答/文档管理 │
                    └──────┬───────┘  └────┬─────────────┘
                           │ 实时查询(Feign+内网签名)│
                           └────────►──────┘
                           │  RabbitMQ 事件（KB 已接收，索引更新 TODO）
                           └────────►──────┘
        Nacos(8848/9848) 注册中心 + 配置中心（config-demo 热更新演示）
        Sentinel 限流（Nacos 动态规则，当前为空）
        MySQL · Redis · ES · Qdrant · RabbitMQ · MinIO
```

**两个业务服务之间的数据关联**：

- **预约余量实时查询**：KB 通过 OpenFeign + Nacos 服务发现直连 CAS 只读接口（`/appointments/availability`），以内网签名头鉴权；LangChain4j `AppointmentTool`（`@Tool`）+ AiServices 实现 Function Calling，AI 助手在识别到预约类问题时可返回实时数据。
- **预约变更事件**：CAS 预约创建/取消后发布 RabbitMQ `appointment.changed` 事件，KB 已实现监听并接收，当前仅记录日志（索引/缓存更新为 TODO）。

## 三、目录结构

```
CampusBrain/
├── README.md                  ← 本文档
├── docs/                      ← 架构设计/实施计划/审计报告（本地笔记，gitignore）
│
├── backend/                   ← 微服务后端（Maven 多模块）
│   ├── pom.xml                ← 顶层聚合 POM（含 Spring Cloud 版本管理）
│   ├── common-auth/           ← 共享认证：JWT + 内网签名
│   ├── gateway/               ← 网关（唯一入口）
│   ├── cas-service/           ← 校园预约（7 个 Maven 子模块，DDD 分层）
│   ├── kb-service/            ← 知识库问答（RAG + Feign + Tool）
│   ├── docker-compose.yml     ← 基础设施编排（Nacos + KB 中间件）
│   └── README.md              ← 后端启动指南
│
└── frontend/                  ← 统一前端（Vue 3 + TS + Element Plus）
    ├── README.md              ← 前端开发指南
    └── src/
        ├── modules/user/      ← CAS 用户端页面（工作台/服务/预约/个人中心/AI 助手）
        ├── modules/admin/     ← CAS 管理端页面（驾驶舱/服务/审核/用户/系统/工具箱）
        ├── modules/assistant/ ← KB AI 助手页（QaPortal）
        ├── common/            ← 统一 store + request
        └── services/          ← API 调用层（正在收敛至 common）
```

## 四、快速开始

### 1. 前置条件

```bash
# 环境变量（后端依赖）—— 在 shell 中导出，或在 docker-compose .env 中设置
export DEEPSEEK_API_KEY='<DeepSeek 密钥>'   # AI 问答必需，否则 LLM 调用失败
export DB_PASSWORD='<CAS 数据库密码>'        # CAS 宿主机 MySQL
export JWT_SECRET='<32字节以上随机串>'
export INTERNAL_SIGN_SECRET='<随机串>'
```

### 2. 启动中间件（Docker，只含 Nacos + KB 中间件）

```bash
cd backend
cp .env.example .env              # 按需修改：中间件密码、JWT/内网签名密钥等
docker compose up -d              # Nacos + MySQL/Redis/ES/Qdrant/RabbitMQ/MinIO
```

> 三个业务服务（gateway/cas-service/kb-service）**在本地运行**，不占用 Docker。
> CAS 依赖宿主机 MySQL(3306)/Redis(6379)，需确保 `cas_db` 已建表。

### 3. 启动三个业务服务（本地）

```bash
# 导出环境变量（三个服务共用）
export DEEPSEEK_API_KEY='<DeepSeek 密钥>'
export DB_PASSWORD='<CAS 数据库密码>'
export JWT_SECRET='<32字节以上随机串>' INTERNAL_SIGN_SECRET='<随机串>'

# IDEA 分别启动，或 java -jar 依次启动：
#   GatewayApplication → 8888
#   CampusAppointmentApplication → 18080
#   KbApplication → 8081
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev        # http://localhost:3000
```

### 5. 验证

```bash
# Nacos 三个服务注册
curl http://localhost:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=10
# 网关路由到 KB
curl http://localhost:8888/api/v1/kb/health
```

浏览器访问 http://localhost:3000，登录后：
- CAS 功能：工作台 / 服务预约 / 我的预约 / 个人中心
- **AI 助手**（导航栏"AI 助手"）：上传文档后提问，走 RAG 问答；问"有哪些服务可预约"触发 Function Calling 返回实时数据

## 五、测试

```bash
cd backend && mvn test     # 125 个测试全绿（CAS 65 + KB 60）
cd frontend && npm run type-check && npm run lint   # 前端类型检查 + lint
```

## 六、关键设计

- **统一 JWT 网关鉴权**：网关验签 JWT，透传身份头 + 内网签名（5 分钟时间戳新鲜度）；服务内 `@RequireRole` 做细粒度授权。
- **服务间调用**：KB 用 OpenFeign + Nacos 服务发现直连 CAS 只读接口，`X-Internal-Sign` 内网签名标识受信服务（CAS `InternalAuthFilter` 校验后放行）。
- **Function Calling**：LangChain4j `AppointmentTool`（`@Tool`）+ AiServices，`QaApplicationService` 按意图分流，预约类问题走实时数据链路。
- **RAG 混合检索**：KB 文档解析 → 分块 → Embedding → ES 关键词 + Qdrant 向量双路召回 → RRF 融合 → LLM 生成（Resilience4j 熔断）。
- **安全加固**：注册提权、IDOR、管理端越权、`/ai` 公开、内网签名防重放均已修复。

## 七、已知限制（如实）

- **AI 问答依赖 `DEEPSEEK_API_KEY`**：未配置则 LLM 调用失败，AI 助手不可用。
- **缓存已全部禁用**：每次提问实时检索 + LLM 回答（避免答非所问/历史串题），代价是每次调用 LLM 有延迟与费用，见 `docs/为什么不用缓存.md`。
- **RabbitMQ 事件消费不完整**：KB 已接收预约变更事件但仅记录日志，索引/缓存更新尚未实现。
- **Sentinel 流控规则为空**：已接入并演示接口，但未配置真实限流规则。
- **响应模型跨服务不统一**：CAS 用 `CommonResult`，KB 用 `ApiResponse`。
- **Maven 治理不统一**：CAS（`com.laoliu`）与 KB（`com.kb`）groupId 不同。
- **咨询时段硬编码**：可用时段仍为固定 6 个，未落库。

## 八、文档索引

| 文档 | 用途 |
|---|---|
| `backend/README.md` | 后端模块/启动/环境变量详解 |
| `frontend/README.md` | 前端开发指南 |
| `docs/秋招简历项目审查报告.md` | 简历视角审查 + 修复进度追踪 |
| `docs/项目问题审计报告.md` | 安全/契约审计 |
| `docs/当前项目遗留问题清单.md` | 当前遗留问题与低成本建议 |
| `docs/为什么不用缓存.md` | AI 问答禁用缓存的原因与权衡 |
| `docs/微服务合体方案.md` | 架构决策与演进 |
| `docs/待办清单.md` | 剩余功能与技术债 |
| `docs/FunctionCalling演示记录.md` | Function Calling 实时查询的可复现演示步骤 |
