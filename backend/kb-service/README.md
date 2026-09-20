# kb-service — 知识库问答服务（RAG）

> CampusBrain 微服务体系中的**知识库 AI 助手（KB）**独立服务，端口 **8081**（servlet context-path `/api/v1/kb`，网关原样转发不剥前缀）。
> 整体架构、端口、中间件与部署见 **`../README.md`**；本文件聚焦 kb-service 的分层、问答链路与配置。

KB 以 RAG（检索增强生成）为核心：管理员上传文档 → 异步解析分块 → 向量化入库；用户问答经「意图判定 → 缓存 → 混合检索 → LLM 生成（同步/SSE 流式）」，并以 LangChain4j `@Tool` 深度集成 CAS 预约查询与两段式下单。

## 技术栈

| | |
|---|---|
| 框架 | Spring Boot 3.3.5 · Spring Cloud Alibaba（Nacos 注册/配置 · OpenFeign）· Resilience4j 熔断 |
| 存储 | MySQL 8（库 `knowledge_base`，MyBatis-Plus）· Redis（会话/QA 缓存/限流/分布式锁） |
| 检索 | Elasticsearch 8（BM25 关键词）+ Qdrant 1.9（向量，Cosine，1024 维） |
| 模型 | LangChain4j + OpenAI 兼容接口：Chat（DeepSeek `deepseek-chat`）、Embedding（硅基流动 Qwen3-Embedding-0.6B） |
| 消息 | RabbitMQ（文档解析异步消费） |

## 分层结构（DDD）

```
com.kb
├── interfaces/rest/          # QaController(/qa) · DocumentController(/documents)
├── application/service/      # 应用编排：QaApplicationService（ask/askStreaming 模板）、
│                             #   AnswerPipeline（RAG 回答管线）、CacheGuard（缓存守卫）、
│                             #   PendingBookingExecutor（待确认预约）、DocumentApplicationService
├── domain/                   # 纯 Java 领域层（零框架注解，含边界测试守卫）
│   ├── chat/                 #   ChatSession 会话上下文、ChatContextHolder 请求级上下文
│   ├── conversation/ document/ event/ knowledgegraph/
│   └── rag/                  #   LlmService、VectorStoreService、IntentClassifier（端口）等
└── infrastructure/
    ├── rag/                  # llm（流式模型/门控）· retrieval（向量+关键词+混合降级）·
    │                         #   intent（关键词意图实现）· chunker/parser/embedding/reranker/graph/rewrite/tool
    ├── cache/                # Redis 会话仓储、QA 缓存、语义缓存（Qdrant collection）、缓存淘汰器
    ├── mq/  schedule/        # 文档处理消费者/生产者、处理锁、僵尸文档回收、索引删除补偿
    ├── persistence/          # mysql（dataobject/mapper）· elasticsearch · qdrant
    └── client/  security/  ratelimit/  metrics/  config/
```

跨模块端口（如 `IntentClassifier`、文档写路径端口）定义在 `domain`，实现下沉 `infrastructure`；纯委托 adapter 已删除，由现有组件直接 implements 端口。

## 核心链路

**文档处理**：上传（业务侧 50MB 二次校验 + 展示标题脱敏）→ MQ 异步消费 → 解析/分块（sliding_window 512/重叠 50）→ 批量 Embedding → MySQL 元数据 + ES 关键词索引 + Qdrant 向量（均带文档归属标识）。处理状态机卡死由 `StuckDocumentReclaimer` 定时回收重投；外部索引删除失败进 `index_delete_failure` 对账表，补偿任务周期重试至解决，超过重试上限置 GIVE_UP 并暴露指标告警。

**问答**：意图判定（预约/知识）→ Redis QA 缓存 + Qdrant 语义缓存（阈值 0.95，TTL 24h，按归属过滤）→ ES + Qdrant 双路召回（各 top10，RRF 融合 top5，单侧超时/异常降级为空结果）→ LLM 生成；「无法回答」门控命中时切直答，语义缓存拒写套话答案防注入放大。流式入口支持客户端断连取消（取消指标可观测）。

**CAS 集成**：`infrastructure/rag/tool` 的工具集经 Feign + 内网签名调 CAS `/appointments/assistant/**`；查询类实时回填余量，动作类只登记草稿，用户在确认卡片点确认后才真正下单。异步线程身份按「SecurityContext → 请求级 ChatContextHolder → 服务身份」三级获取，不继承、不残留。

**归属隔离**：ES/Qdrant/语义缓存检索一律带「本人或全局共享」过滤，取不到登录身份按无权限处理（fail-closed）。文档 READY/FAILED 事件触发缓存淘汰并走合并窗口（批量上传只做一次全量淘汰），语义缓存另有过期点定时清理。

**可靠性**：文档处理锁为「Redis 令牌 + 本机在途登记」双判据，Watchdog 定时续约、悬停登记剪枝；接口限流用 Redis 滑动窗口 Lua；检索线程池容量对齐在途 SSE 并发，突发超额由检索超时降级兜底。

## 主要接口（外部前缀 `/api/v1/kb`）

| 路径 | 说明 |
|---|---|
| `POST /qa/ask` | 同步问答 |
| `GET /qa/ask/stream` | SSE 流式问答（事件：message / citations / slots / confirm / action 等） |
| `POST /qa/feedback`、`GET /qa/conversation/{sessionId}`、`POST /qa/session/{sessionId}/reset` | 反馈 / 会话历史 / 重置会话 |
| `POST /documents/upload` | 文档上传（管理员；50MB 上限） |
| `GET /documents`、`GET /documents/{id}`、`DELETE /documents/{id}`、`GET /documents/search` | 文档管理与检索 |
| `GET /health` | 健康检查 |

## 关键配置（`kb.*`，均带默认值，可经环境变量覆盖）

| 配置 | 默认 | 说明 |
|---|---|---|
| `kb.sse.max-concurrent` | 50 | 在途流式问答上限（检索线程池默认跟随此值） |
| `kb.retrieval.timeout-seconds` / `pool-size` | 30 / 0 | 混合检索总预算超时；线程池大小（0=跟随 SSE 上限） |
| `kb.qa.history-limit` | 8 | 拼装提示词携带的最近历史条数（问答/模型层同一口径） |
| `kb.cache.enabled` / `semantic-threshold` / `semantic-ttl-hours` | true / 0.95 / 24 | QA 缓存与语义缓存 |
| `kb.cache.semantic-cleanup-*` / `evict-merge-*` | 6h / 30s | 语义缓存过期点清理周期；文档事件缓存淘汰合并窗口 |
| `kb.document.processing-lock-ttl-seconds` / `-renew-ms` / `-prune-after-ms` | 1800 / 300000 / 3600000 | 文档处理锁 TTL、Watchdog 续约周期、本机登记剪枝阈值 |
| `kb.embedding.batch-size` / `parallelism` | 32 / 4 | Embedding 批大小与并发 |
| `kb.file-storage-path` | `../file` | 上传文档落盘目录（容器内为 `/file` 命名卷） |

中间件连接（MySQL/Redis/ES/Qdrant/RabbitMQ/Nacos）与 `OPENAI_API_KEY`、`EMBEDDING_API_KEY` 等见 `src/main/resources/application.yml` 与 `../.env.example`。

## 数据库迁移（Flyway）

- `V1__init_document_and_conversation.sql` —— document / document_chunk / conversation 等初始表。
- `V2__conversation_add_user_id.sql` —— AI 会话归属 `conversation.user_id`（历史/重置/反馈归属校验）。
- `V3__index_delete_failure.sql` —— 外部索引删除失败对账表（补偿重试 + 告警指标）。

## 构建 / 运行 / 测试

```bash
# 本地起服务（backend 目录下，自动加载 .env + 打包）
cd ..
./scripts/run-local.sh kb          # :8081，首次启动执行 Flyway

# 只构建本模块（在 backend/ 下执行）
mvn -B -pl kb-service -am clean package -DskipTests   # kb-service/target/kb-service-1.0.0.jar

# 测试：130 个测试方法（H2 + @MockBean 隔离 ES/MQ/Redis/Cas，无需 Docker）
mvn -B -pl kb-service -am test
```

## 代码约定

- Controller 返回 `ApiResponse<T>`（与 CAS 的 `CommonResult` 类名不同，但成功 code 均为 200）；业务异常走全局处理器，HTTP 状态语义 400/401/403/404/500。
- `domain/` 保持纯 Java、无 Spring 注解，并有领域边界测试禁止反向依赖。
- 密钥一律环境变量注入；缺省 LLM/Embedding Key 时 AI 问答不可用，登录与 CAS 预约不受影响。
