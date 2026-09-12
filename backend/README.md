# 后端 — CampusBrain 微服务

统一后端仓库，聚合 **gateway（网关）**、**cas-service（校园预约）**、**kb-service（知识库问答）** 三个可运行服务 + **common-auth** 共享认证库。Maven 多模块，单仓 `mvn` 构建；既可本地 IDE / `java -jar` 开发调试，也可 Docker Compose 全栈部署。

## 一、架构与端口

```
frontend(统一前端) → gateway :8888  ← 唯一入口，统一 JWT 鉴权
                       ├─ /api/v1/**       → cas-service :18080（自身 context-path=/api/v1）
                       └─ /api/v1/kb/**    → kb-service  :8081 （StripPrefix=2 后命中 /kb/**）
                    Nacos :8848/9848   注册中心 + 配置中心（cas-service.yaml 热更新）
                    Sentinel           限流（Nacos 动态规则 cas-sentinel-flow-rules）
```

| 模块 | 说明 | 端口 |
|---|---|---|
| `common-auth` | 共享认证：JWT 工具 + 内网签名（时间戳防重放） | — |
| `gateway` | Spring Cloud Gateway：JWT 验签、路由、身份头透传 | 8888 |
| `cas-service` | 校园预约系统（DDD 多模块，Maven 子模块见下） | 18080 |
| `kb-service` | 知识库问答 / RAG 平台 | 8081 |

> 端口均为本地宿主；容器部署内部端口见 `docker-compose.business.yml`（gateway 容器内为 :80，宿主映射 8888；cas/kb 与本地一致）。

### cas-service 内部模块（Maven 结构）
```
cas-dependencies         依赖 BOM（版本统一管理）
cas-framework            框架聚合：cas-common + 各 spring-boot-starter-*（web/security/mybatis/redis/test…）
cas-module-infra         文件上传(本地/OSS)、二维码、邮件
cas-module-system        用户/注册登录/验证码/角色/通知策略
cas-module-appointment   预约核心：服务目录、咨询/教室/设备/活动、审核、轮播图
cas-thirdparty           天气、AI 模型(Qwen)、阿里云短信/OSS
cas-server               启动入口(application.yml/@MapperScan)，含 Flyway 迁移脚本
```

## 二、依赖中间件

**开发模式**（宿主运行，`docker-compose.yml` 只起基础设施）：

| 组件 | 地址 | 归属 / 说明 |
|---|---|---|
| Nacos | localhost:8848 / 9848 | 注册中心 + 配置中心（compose `nacos`） |
| MySQL | localhost:3306 | CAS，库 `cas_db`（**宿主机实例**，需自备） |
| Redis | localhost:6379 | CAS（宿主机实例） |
| MySQL | localhost:3307 | KB，库 `knowledge_base`（compose `kb-mysql`） |
| Redis | localhost:6380 | KB（compose `kb-redis`） |
| Elasticsearch | localhost:9200 | KB 关键词检索（`kb-es`） |
| Qdrant | localhost:6334 | KB 向量检索（`kb-qdrant`） |
| RabbitMQ | localhost:5672 | 事件总线，admin/admin123（`kb-rabbitmq`） |
| MinIO | localhost:9000 | KB 文档存储，bucket `knowledge-base-docs`（`kb-minio`） |

## 三、环境变量（backend/.env）

> 密钥全部环境变量化并 gitignore；从 `.env.example` 复制 `.env` 填写。下列为**必填 / 常用**项。

| 变量 | 用途 | 默认 |
|---|---|---|
| `MYSQL_ROOT_PASSWORD` | CAS 数据库（root）密码，**必填** | 无 |
| `JWT_SECRET` / `INTERNAL_SIGN_SECRET` | JWT / 内网签名密钥，生产必改 | 内置示例 |
| `KB_MYSQL_ROOT_PASSWORD` | KB MySQL 密码 | root123 |
| `RABBITMQ_PASSWORD` | RabbitMQ | admin123 |
| `MINIO_ACCESS_KEY` / `MINIO_ROOT_PASSWORD` | MinIO | minioadmin / minioadmin123 |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP 发信（可选） | smtp.163.com / 465 |
| `WEATHER_API_ID` / `WEATHER_API_KEY` | 天气 API（可选） | — |
| `DEEPSEEK_API_KEY` | 预留：CAS 侧旧 Qwen `/ai/chat` 已下线（2026-09-07），当前无消费方 | — |
| `OPENAI_API_KEY` | KB Chat（DeepSeek 兼容接口） | — |
| `EMBEDDING_API_KEY` | KB Embedding（硅基流动 Qwen3-Embedding-0.6B） | — |
| `ALIYUN_OSS_ACCESS_KEY_ID` / `_SECRET` | 阿里云 OSS（可选） | — |

## 四、本地启动

### 1. 起基础设施（Nacos + KB 中间件）
```bash
cd backend
cp .env.example .env      # 填 MYSQL_ROOT_PASSWORD 等
docker compose up -d      # nacos + kb-mysql/redis/es/qdrant/rabbitmq/minio
```
> CAS 的 MySQL/Redis 用宿主机实例；库 `cas_db` 无需手工建表，**Flyway 会在 CAS 首次启动时自动建表并灌种子**。

### 2. 起三个服务（推荐一键脚本）
```bash
./scripts/run-local.sh gateway    # 先起网关，:8888
./scripts/run-local.sh cas        # :18080  —— 自动执行 Flyway（V1~V5：schema + 种子账号 + 服务分类 + 咨询沟通表）
./scripts/run-local.sh kb         # :8081
```
脚本行为：加载 `.env` → `mvn -DskipTests -pl <模块> -am package` → `java -jar`；`--fast` 跳过打包。

等价手动命令：
```bash
mvn clean package -DskipTests
java -jar cas-service/cas-server/target/cas-server-1.0.0.jar   # CAS
java -jar kb-service/target/kb-service-1.0.0.jar                # KB
java -jar gateway/target/gateway-1.0.0.jar                      # 网关
```

### 3. 种子账号（V2）
`admin@campus.com / 123456`（管理员） · `user@campus.com / 123456`（普通用户）。

### 4. 冒烟验证
```bash
# Nacos 服务列表应含 gateway / cas-service / kb-service
curl "http://localhost:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=10"
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8888/api/v1/captcha   # →200（图形验证码）
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8888/api/v1/kb/health # →200（KB）
```

## 五、数据库与 Flyway 约定

- CAS 迁移位于 `cas-service/cas-server/src/main/resources/db/migration/`：`V1__init_schema.sql`（表结构 + 校区种子 + 轮播图；服务表直接建 `category_id`，不存 `category` 编码串）、`V2__seed_initial_users.sql`（初始账号）、`V3__seed_teacher_users.sql`（教师账号 + 咨询师 user_id 回填）、`V4__service_category.sql`（**仅新增**分类表 `service_category` + 固定 4 类种子：教师咨询/设备借用/教室空间/活动报名）、`V5__consult_chat.sql`（**仅新增**咨询沟通会话表 `consult_chat_conversation` + 消息表 `consult_chat_message`，学生⇄教师 1:1 在线留言）。
- **迁移约定**：V*.sql 面向**全新机器**，只含建表/种子等增量，**不写 ALTER/UPDATE 改既有表结构**。服务分类落库 = 服务分类：全新库由 V1 直接建出 `services.category_id`，老库按 `cas-service/UPGRADE-service-category.md` 直接 SQL 演进（ALTER + 回填 + DROP `category`），V4 建表/补种子幂等可重复。
- KB 迁移位于 `kb-service/src/main/resources/db/migration/`：`V1__init_document_and_conversation.sql`（文档/分块/会话等）。多租户 `tenant_id` 已于 2026-09-12 下线：原 `V4__add_tenant_id_to_business_tables.sql` 连同 `TenantContext`/`TenantFilter` 一并移除，业务代码与 H2 测试 schema 均不再保留租户字段。
- **新机器**：CAS/KB 首次启动自动执行全部迁移，零手工 SQL。
- **已有库**：迁移文件用于全新环境，已上线的库请直接执行 SQL 演进，**不要改写历史 `V*.sql` 去适配旧库**（否则 Flyway checksum 校验失败）；如需调整结构，本地直接对库执行 SQL 即可。

## 六、核心能力（对应当前代码）

### CAS 预约
- **按校区分流**：仓前(cq)/下沙(xs) 各自服务目录（`services.campus`），咨询师（`consultant`）、教室（`room`）、设备（`equipment`）挂各自校区服务，用户端按校区隔离。
- **服务分类字典**：`service_category` 表固定 4 类（教师咨询/设备借用/教室空间/活动报名），`services.category_id` 代码级外键（不建 DB FK），读写时后端按 id 回填编码与中文名；只读接口 `GET /app/service-categories` 供前端下拉/展示，新增服务必须指定存在的 `categoryId`，分类不可在管理端增删改。
- **咨询时段预约**：咨询师在 `time_slot` 维护可约日期时段；选人 + 时段占位，同人同时段冲突被拒，审核/取消释放。
- **咨询沟通（学生⇄教师）**：仅「教师咨询」场景开放 1:1 在线留言，一条会话 = 一位学生 + 一位教师（`consult_chat_conversation` 唯一对）；学生可从选咨询师卡片/我的咨询预约发起，教师在待我审核/我的咨询里回复，或走消息中心；消息按 `afterId` 增量轮询拉取 + 未读/已读管理。REST 见 `ConsultChatAppController`（`/app/chat/consult/**`，参与者鉴权，代码级外键）。
- **教室时段预约**：`room` 一间教室 + `slot_date/start_time/end_time` 唯一，重复窗口拒绝。
- **设备窗口借用**：`equipment.total_stock/available_stock` + 时段窗口 + 数量，库存原子扣减，到点自动归还。
- **活动容量**：`capacity`（-1 不限）+ `booked_count` 原子扣减，超额拒绝。
- **预约状态机**：`manage_status` 0待审/1通过/2拒绝/3取消/4完成；`BookingAutoCompleteTask`（`@EnableScheduling`，60s 轮询）把已过窗口的预约自动置为完成。
- **通知**：`notification_policy`（全局）+ `user.email_notify`（用户偏好）双开关，审核结果邮件按开关发送。
- **轮播图**：`carousel` 表 + 管理端上传/删除/拖拽排序（≤6）+ 用户端列表。
- **账号**：图形验证码登录、邮箱验证码注册、忘记密码、改密（旧密码校验）、`@RequireRole` 四级 RBAC（普通用户/教师/管理员/超管；教师审「自己名下咨询档期」）。

### KB 知识库
- 文档上传 → 解析 → 分块（sliding_window 512/50）→ Embedding（硅基流动 Qwen3-Embedding-0.6B，1024 维）
- 检索：ES 关键词（top10）+ Qdrant 向量（top10）→ RRF 融合（top5）→ DeepSeek（`deepseek-chat`）生成，Resilience4j 熔断。
- Function Calling：Feign + Nacos + 内网签名直连 CAS `/appointments/availability`，LangChain4j `@Tool`。
- 存储：KB 元数据在 `knowledge_base`(MySQL)，文档正文在 MinIO，关键词索引 ES，向量 Qdrant。

## 七、测试与 CI

```bash
cd backend && mvn -B test
```
- 后端共 **131 个测试方法**（CAS 82 · KB 49），CAS 分布在 appointment/infra/system/thirdparty，KB 集成测试用 **H2 + `@MockBean` 隔离** ES/MQ/Redis/Cas 等中间件（无需 Docker）。
- GitHub Actions `.github/workflows/ci.yml`：push/PR 自动跑 `mvn -B test` + 前端 type-check/build。

## 八、Docker 部署（服务器）

| 文件 | 用途 |
|---|---|
| `docker-compose.yml` | 开发用基础设施（Nacos + KB 中间件） |
| `docker-compose.business.yml` | 服务器全栈：`cas-mysql`(cas_db) + `cas-redis` + `gateway`/`cas-service`/`kb-service`/`frontend` 四服务 + `nacos`；镜像 tag 由 `${TAG:-latest}` 注入，`pull_policy: never` 保证用本地刚构建的镜像 |

部署约束：**必须在 `/opt/campusbrain/backend` 目录内执行 compose**（卷/网络按目录项目名关联，错位会建空 MySQL 卷）。

常用脚本（`backend/scripts/`）：
- `load-env.sh`：source 加载 `.env`
- `run-local.sh`：本地一键起单服务
- `deploy-server.sh`：服务器按改动增量构建镜像并 compose up（幂等：HEAD 未变直接退出）
- `publish.sh`：一行发版（推 GitHub + 服务器镜像，触发 CI/Jenkins）

## 九、代码约定
- Controller 统一返回 `CommonResult<T>`；业务异常抛 `BusinessException(ErrorCode)` 由全局处理器兜底。
- 跨模块调用走 `api/` 接口，模块间不直接依赖 Mapper。
- `domain/` 纯净实体，无 Spring 注解；应用层编排、基础设施层落实现。
- 新增密钥不入库，一律经环境变量注入。
