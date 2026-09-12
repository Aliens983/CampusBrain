<div align="center">

# 智汇校园 · CampusBrain

<p align="center">
  <img src="https://readme-typing-svg.demolab.com?font=Fira+Code&weight=700&size=32&duration=2800&pause=1200&color=3FB6FF&center=true&vCenter=true&width=780&lines=CampusBrain+%C2%B7+Smart+Campus+Platform;Campus+Appointment+%C3%97+RAG+Knowledge+Q%26A;Spring+Cloud+Alibaba+Microservices;HZNU+%E6%A0%A1%E5%BE%BD%E8%93%9D+%C2%B7+%E9%A2%84%E7%BA%A6%E6%8C%89%E6%A0%A1%E5%8C%BA" alt="Typing SVG" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/Spring_Cloud_Alibaba-2023.0.1.2-1677FF?style=for-the-badge&logo=spring&logoColor=white" alt="Spring Cloud Alibaba" />
  <img src="https://img.shields.io/badge/Spring_Cloud_Gateway-3.3-3DDC84?style=for-the-badge&logo=spring&logoColor=white" alt="Gateway" />
  <img src="https://img.shields.io/badge/Nacos-2.3.2-1E88E5?style=for-the-badge&logo=nacos&logoColor=white" alt="Nacos" />
  <img src="https://img.shields.io/badge/Flyway-10.x-CC0200?style=for-the-badge&logo=flyway&logoColor=white" alt="Flyway" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Vue-3.4-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=white" alt="Vue 3" />
  <img src="https://img.shields.io/badge/TypeScript-5.6-3178C6?style=for-the-badge&logo=typescript&logoColor=white" alt="TypeScript" />
  <img src="https://img.shields.io/badge/Element_Plus-2.8-409EFF?style=for-the-badge&logo=element&logoColor=white" alt="Element Plus" />
  <img src="https://img.shields.io/badge/MyBatis_Plus-3.5.5-1E90FF?style=for-the-badge&logo=mybatis&logoColor=white" alt="MyBatis-Plus" />
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL" />
  <img src="https://img.shields.io/badge/Redis-7.x-DC382D?style=for-the-badge&logo=redis&logoColor=white" alt="Redis" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Elasticsearch-8.12-005571?style=for-the-badge&logo=elasticsearch&logoColor=white" alt="Elasticsearch" />
  <img src="https://img.shields.io/badge/Qdrant-1.9-DC244C?style=for-the-badge&logo=qdrant&logoColor=white" alt="Qdrant" />
  <img src="https://img.shields.io/badge/RabbitMQ-3.13-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white" alt="RabbitMQ" />
  <img src="https://img.shields.io/badge/MinIO-latest-C72E49?style=for-the-badge&logo=minio&logoColor=white" alt="MinIO" />
  <img src="https://img.shields.io/badge/Docker-%F0%9F%90%B3-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/CI-GitHub_Actions-2088FF?style=flat-square" alt="GitHub Actions" />
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=flat-square" alt="License" />
  <img src="https://img.shields.io/badge/Architecture-Microservices_%C2%B7_DDD-ff69b4?style=flat-square" alt="Architecture" />
  <img src="https://img.shields.io/badge/Auth-Gateway_JWT_%C2%B7_RBAC-green?style=flat-square" alt="Auth" />
  <img src="https://img.shields.io/badge/Booking-Slot%2FStock_Anti--conflict-9b59b6?style=flat-square" alt="Booking" />
</p>

</div>

**CampusBrain 智汇校园** —— 面向高校的智慧校园预约平台（毕设 / 简历项目）。以 Spring Cloud Alibaba 微服务将**校园预约系统（CAS）**与**知识库问答平台（KB）**合为一体：统一 Vue 前端、统一网关 JWT 鉴权；KB 作为专属 AI 助手，提供知识库 RAG 问答，并可 **Function Calling 实时查询预约数据**。

平台围绕**杭州师范大学两校区**场景建模（仓前 / 下沙），预约服务、咨询师、教室、设备均**按校区分离**；界面采用 **HZNU 校徽蓝 `#3FB6FF`** 主题。

---

## 一、功能状态（对应当前代码）

### 校园预约（CAS）—— 主体业务

| 能力 | 状态 | 说明 |
|---|---|---|
| 按校区分流 | ✅ | 仓前(cq)/下沙(xs) 各自一套服务目录与资源（咨询师/教室/设备）；用户端可切校区，工作台区分 |
| 服务目录 | ✅ | 多分类：`space` 教室空间 · `teacher` 教师咨询 · `equipment` 设备借用 · `activity` 活动报名，**分类字典落库 `service_category`（固定 4 类）**，`services.category_id` 代码级外键引用；支持服务上下架、封面图 |
| 咨询时段预约 | ✅ | 咨询师 + 日期可约时段（`time_slot` 落库）；预约占用时段，冲突被拒；审核/取消/到点自动释放 |
| 教室时段预约 | ✅ | **一间教室同一时间段仅一人可约**（唯一性约束 + 冲突检测），按 `slot_date + start/end` 排他 |
| 设备窗口借用 | ✅ | 固定时段窗口 + 库存扣减（`available_stock`）；到点自动归还（转 COMPLETED） |
| 活动容量预约 | ✅ 免审直通 | `capacity` 容量扣减，-1 不限；**容量够即直接成功（无人工审核）**，开始前可自助取消并释放名额 |
| 预约审核/取消 | ✅ | 0待审→1通过/2拒绝/3取消/4完成；拒绝必填原因；审核/取消自动释放占用的时段与库存 |
| 教师自审档期 | ✅ | 教师端 `/teacher/*`：待我审核 / 我的咨询，教师只审自己名下咨询预约 |
| 咨询在线沟通 | ✅ | 学生⇄教师 1:1 留言（`consult_chat_conversation`/`consult_chat_message`，Flyway V5）；仅教师咨询场景开放，按 `afterId` 增量轮询 + 未读/已读 |
| 自动完成调度 | ✅ | `@Scheduled` 定时扫描，窗口过期自动置 COMPLETED（设备到点归还、教室释放） |
| 邮件通知 | ✅ | 审核结果邮件（受**全局通知策略** + **用户邮件偏好**开关控制） |
| 首页轮播图 | ✅ | 管理端上传 / 删除 / 拖拽排序（最多 6 张），用户端工作台渲染；默认 6 张校区/校园图 |
| 账号体系 | ✅ | 图形验证码登录、邮箱验证码注册、忘记密码、个人中心改密（旧密码校验）；四级 RBAC：学生/教师/管理员/超管，教师自审名下咨询档期 |
| 通知设置 | ✅ | 管理端策略（邮件通道开关）+ 用户偏好联动 |

### 知识库问答（KB）与工程能力

| 能力 | 状态 | 说明 |
|---|---|---|
| RAG 知识库问答 | ✅ | 文档上传 → 解析分块 → Embedding(Qwen3) → **ES 关键词 + Qdrant 向量双路召回 → RRF 融合** → DeepSeek 生成 |
| AI 助手入口 | ✅ | 前端 `/assistant`（QaPortal），知识库资料优先回答；文档上传仅管理员 |
| 预约实时查询 | ⚠️ 需 Key | KB 经 Feign + Nacos + 内网签名直连 CAS 只读余量接口；LangChain4j `@Tool` 实现 Function Calling（需配 `OPENAI_API_KEY`/`EMBEDDING_API_KEY`） |
| RabbitMQ 预约事件 | ⚠️ 部分 | CAS 发布 `appointment.changed`；KB 已监听接收，仅记录日志（索引更新为 TODO） |
| CI 质量门禁 | ✅ | GitHub Actions：后端 `mvn -B test` + 前端 type-check/build，push 自动触发 |
| 交付脚本 | ✅ | `backend/scripts/run-local.sh`（本地一键起服务）/ `publish.sh`（一行发版）/ `deploy-server.sh`（服务器部署） |
| 数据库迁移 | ✅ | CAS 与 KB 均启用 **Flyway**，启动自动建表 + 种子数据（校区、咨询师、教师账号、教室、设备、轮播图、服务分类字典、初始账号） |

## 二、架构

```
                    ┌───────────────────────────────────────┐
  浏览器 / 单页应用   │  frontend/ 统一 Vue 前端（预约 + AI 助手） │  :3000(dev) / nginx(:80)
                    └─────────────────┬─────────────────────┘
                                      │ /api（vite 代理补 /v1）
                    ┌─────────────────▼─────────────────────┐
                    │  gateway（Spring Cloud Gateway）:8888   │ ← 唯一入口
                    │  · 统一 JWT 验签 · @RequireRole RBAC 授权 │
                    │  · 内网签名 X-Internal-Sign + 时间戳防重放 │
                    └────────┬──────────────┬───────────────┘
              /api/v1/**    │              │   /api/v1/kb/**
                    ┌───────▼───────┐  ┌────▼─────────────┐
                    │ cas-service   │  │ kb-service       │
                    │ :18080 /api/v1│  │ :8081            │
                    │ 预约·用户·审核 │  │ RAG 问答·文档管理 │
                    └───────┬───────┘  └────┬─────────────┘
                            │ 余量实时查询(Feign+内网签名) │
                            └───────────►──────┘
                            │ RabbitMQ appointment.changed（KB 监听）
                            └───────────►──────┘
   Nacos(8848) 注册/配置  ·  MySQL(cas_db / knowledge_base)  ·  Redis
   Elasticsearch + Qdrant（KB 检索）· RabbitMQ · MinIO（KB 文档存储）
```

**服务间协作**
- **预约余量实时查询**：KB 经 OpenFeign + Nacos 服务发现调用 CAS 只读接口（`/appointments/availability`），以内网签名头标识受信服务；LangChain4j `AppointmentTool` + AiServices 在识别到预约问题时返回实时数据。
- **预约变更事件**：CAS 预约创建/取消后发布 RabbitMQ `appointment.changed`，KB 监听消费。

## 三、目录结构（git 追踪范围）

```
CampusBrain/
├── README.md                ← 本文档
├── Jenkinsfile              ← 服务器 Jenkins 流水线定义
├── .github/workflows/ci.yml ← GitHub Actions CI（后端测试 + 前端构建）
│
├── backend/                 ← Maven 多模块微服务后端
│   ├── common-auth/         ← 共享认证：JWT 工具 + 内网签名
│   ├── gateway/             ← Spring Cloud Gateway（统一鉴权入口，:8888）
│   ├── cas-service/         ← 校园预约（DDD 多模块，:18080）
│   ├── kb-service/          ← 知识库问答 / RAG（:8081）
│   ├── docker-compose.yml        ← 开发：基础设施（Nacos + KB 中间件）
│   ├── docker-compose.business.yml ← 服务器：全栈编排（含 cas-mysql/redis + 四服务 + 前端）
│   ├── scripts/             ← load-env / run-local / deploy-server / publish
│   └── README.md            ← 后端启动 / 模块 / 环境变量详解
│
└── frontend/                ← 统一前端（Vue 3 + TS + Element Plus + Pinia）
    ├── src/modules/user/    ← 预约用户端（工作台/服务/预约/个人中心/AI 助手）
    ├── src/modules/admin/   ← 预约管理端（概览/服务治理/审核/用户/系统设置/工具箱）
    ├── src/modules/assistant/← KB AI 助手页（QaPortal）
    └── README.md            ← 前端开发指南
```

> 仓库根目录的 `docs/`、`logs/`、`uploads/`、`.env` 等为个人笔记 / 运行产物 / 密钥，已 gitignore，不入库。

## 四、快速开始（本地开发）

### 1. 前置条件
JDK 17 · Maven 3.9 · Node ≥ 18 · Docker；本地 MySQL 与 Redis（CAS 用宿主实例）。

### 2. 配置环境变量
```bash
cd backend
cp .env.example .env        # 至少填 MYSQL_ROOT_PASSWORD；JWT_SECRET / INTERNAL_SIGN_SECRET 建议自定义
```
> 密钥已全部环境变量化，`application.yml` 不含真实凭据（生产必须覆盖内置示例默认值）。

### 3. 启动基础设施 + 三个服务
```bash
cd backend
docker compose up -d                                  # Nacos + KB 的 MySQL/Redis/ES/Qdrant/RabbitMQ/MinIO
./scripts/run-local.sh gateway   # :8888  （网关）
./scripts/run-local.sh cas       # :18080 （预约 CAS，Flyway 自动建表 + 种子）
./scripts/run-local.sh kb        # :8081  （知识库 KB）
```
`run-local.sh` 自动加载 `.env` → `mvn package`（跳过测试）→ `java -jar`；代码没改可加 `--fast` 直接起 jar。

### 4. 启动前端
```bash
cd frontend
npm install
npm run dev        # http://localhost:3000
```

### 5. 登录账号（Flyway V2 种子）
| 角色 | 邮箱 | 密码 |
|---|---|---|
| 管理员 | `admin@campus.com` | `123456` |
| 普通用户 | `user@campus.com` | `123456` |

### 6. 验证
- 浏览器登录后：工作台 → 服务中心（切校区、按分类选服务）→ 咨询/教室/设备预约 → 我的预约；
- 管理端（admin 账号）：服务治理 / 预约审核 / 系统设置（轮播图、通知策略）；
- AI 助手 `/assistant`：配置好 LLM Key 后可 RAG 问答。

## 五、测试与 CI
```bash
cd backend && mvn -B test     # 131 个测试方法：CAS 82 + KB 49（KB 用 H2 + @MockBean 隔离中间件）
cd frontend && npm run type-check && npm run build   # vue-tsc + vite
```
推送到 GitHub 自动触发 `ci.yml`（后端 test + 前端 type-check/build）作为质量门禁。

## 六、关键设计
- **统一网关鉴权**：网关验签 JWT → 透传身份头；服务内 `@RequireRole` 细粒度授权；服务间用 `X-Internal-Sign` 内网签名。
- **预约防冲突**：时段类预约（咨询/教室）行级加锁 + 重叠查询保证唯一；设备/活动库存 `available_stock` / `capacity` 原子扣减防超卖，取消/拒绝释放。
- **两校区数据模型**：服务、咨询师、教室、设备均带 `campus`/挂校区服务，用户端按校区隔离浏览。
- **Flyway 迁移**：启动自动建库建表并灌入校区种子与初始账号（新机器零手工 SQL）。
- **RAG 混合检索**：ES 关键词 + Qdrant 向量双路召回 → RRF 融合 → LLM（Resilience4j 熔断）。

## 七、已知限制
- **AI 问答依赖外部 Key**：KB 需 `OPENAI_API_KEY`（DeepSeek 兼容）+ `EMBEDDING_API_KEY`（硅基流动），缺省时 AI 助手不可用（登录/预约不受影响）。
- **RabbitMQ 消费不完整**：KB 收到预约事件仅记录日志，索引/缓存更新仍为 TODO。
- **响应模型 / Maven 治理不统一**：CAS 用 `CommonResult`（`com.laoliu`），KB 用 `ApiResponse`（`com.kb`），属历史演进结果，未强统。
- **Sentinel 已接入但流控规则为空**（Nacos `cas-sentinel-flow-rules`），预留生产调优。
- **邮件 / 短信 / 天气 / OSS** 为可选外部集成，未配对应 Key 时相应能力降级。

## 八、文档索引
| 文档 | 内容 |
|---|---|
| `backend/README.md` | 后端模块、端口、中间件、环境变量、启动与部署 |
| `frontend/README.md` | 前端技术栈、路由、开发 / 构建、代理、账号 |
| `backend/cas-service/README.md` | cas-service 模块级说明（DDD 分层、预约领域模型） |
| `backend/cas-service/CLAUDE.md` | cas-service 给 AI 助手的权威工作指南（模块/接口/约定/现状） |
| `backend/cas-service/**/AGENTS.md` | cas-service 及各子模块的 Agent 快速指引 |
| `backend/cas-service/UPGRADE-*.md` | 老库手工演进说明（服务分类落库、教师角色） |
