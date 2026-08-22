# 统一前端 — 智汇校园 · CampusBrain

基于 **Vue 3 + TypeScript + Vite + Element Plus + Pinia** 的统一单页应用，整合校园预约系统（CAS）与知识库 AI 助手（KB）。

## 功能概览

- **CAS 用户端**：工作台 / 服务中心 / 会议室 / 设备借用 / 咨询服务 / 我的预约 / 消息 / 个人中心
- **CAS 管理端**：管理驾驶舱 / 服务治理 / 预约审核 / 用户管理 / 系统管理
- **KB AI 助手**（`/assistant`）：知识库问答 + 文档上传/管理，基于 RAG + Function Calling，可实时查询预约数据（**需后端配置 `DEEPSEEK_API_KEY` 后问答可用**）

## 目录结构

```
frontend/
├── src/
│   ├── modules/
│   │   ├── user/          # CAS 用户端（路由 + 视图）
│   │   ├── admin/         # CAS 管理端（路由 + 视图）
│   │   └── assistant/     # KB AI 助手（QaPortal 等）
│   ├── common/            # 统一基础设施
│   │   ├── stores/user.ts # 用户 store（pinia + persist）
│   │   ├── utils/request.ts  # axios 封装（统一 401/错误处理）
│   │   ├── utils/auth.ts  # 角色/登录工具
│   │   └── types.ts       # 共享类型
│   ├── services/          # API 调用层（api/index/portal/campus）
│   ├── views/             # 页面组件（auth/dashboard/bookings/admin 等）
│   ├── router/index.ts    # 路由 + 守卫
│   ├── layout/            # 用户/管理端布局
│   └── types/             # 类型定义
├── vite.config.ts         # 代理：/api/v1/kb→网关8888透传，/api→网关8888补/v1前缀
└── package.json
```

> API 层正在收敛中：新代码统一使用 `@/common/utils/request` 与 `@/common/stores/user`；旧的 `@/utils/request`、`@/stores/user` 仍有部分视图引用，尚未完全删除。

## 快速开始

### 环境要求

- Node.js >= 18, npm >= 9

### 安装与开发

```bash
npm install
npm run dev         # http://localhost:3000
```

### 构建 / 检查

```bash
npm run build        # vue-tsc && vite build
npm run type-check   # vue-tsc --noEmit
npm run lint         # eslint
```

## 代理配置（vite.config.ts）

所有请求统一走网关（`localhost:8888`，本地开发时 GatewayApplication 端口；Docker 部署为 `localhost:80`）：

| 路径 | 目标 | 说明 |
|---|---|---|
| `/api/v1/kb` | `http://localhost:8888` | KB 路径已带 `/v1`，直接透传网关 |
| `/api` | `http://localhost:8888` | CAS 路径补 `/v1` 前缀（rewrite `/api` → `/api/v1`）后转发网关 |

> 网关按 `/api/v1/kb/**` → kb-service、其余 → cas-service 路由。生产 nginx 与 vite 代理保持一致（拆两条 `location`）。

## 登录与账号

- 统一登录走 CAS 认证接口（`/api/v1/auth/login`），token 存 `localStorage`（persist key `enterprise_frontend_user`）。
- 后端离线时前端按用户名关键字分流演示角色，见 **`DEMO_ACCOUNTS.md`**。

## 关键说明

- **AI 助手 SSE**：问答走 `EventSource /api/v1/kb/qa/ask/stream?token=...`，网关已支持从 query 读 token（EventSource 无法设 header）；回答完成后收到 `messageId` 事件，用于点赞/点踩反馈（`POST /api/v1/kb/qa/feedback`）。
- **token 统一**：前端只维护一套 token（CAS JWT），经网关透传身份；KB 的 AI 问答需后端配置 `DEEPSEEK_API_KEY`，否则 LLM 调用失败。
