# 前端 — CampusBrain 统一前端

面向 **校园预约（CAS）+ 知识库 AI 助手（KB）** 的统一单页应用。Vue 3 `<script setup>` + TypeScript + Vite + Element Plus + Pinia。主题采用杭师大 **HZNU 校徽蓝 `#3FB6FF`**。

## 技术栈
Vue 3.4 · TypeScript 5.6 · Vite 5 · Element Plus 2.8 · Pinia（+ persistedstate）· Vue Router 4 · SCSS · ECharts · dayjs

## 路由 / 页面（对应 `src/router/index.ts` + 各模块 router）

**用户端**（布局 `src/layout/UserLayoutShell.vue`，需登录）

| 路径 | 页面 | 说明 |
|---|---|---|
| `/dashboard` | 工作台 | 两栏：标题 + **首页轮播图**（鼠标按住左右拖动切图）/ 今日安排 + 预约入口；按校区展示 |
| `/services` | 服务中心 | **校区切换** + 分类筛选（空闲教室/咨询/设备/活动），服务卡片（封面图） |
| `/service/:id` | 服务详情 | 按服务类型进入不同预约表单：咨询选人+时段 / 教室选房+时段 / 设备选数量+借用窗口 / 活动申请 |
| `/bookings` | 我的预约 | 预约列表（校区标签 + 资源×数量明细 + 状态） |
| `/bookings/:id` | 预约详情 | 单条详情 / 取消；咨询类提供「联系咨询老师」在线留言 |
| `/chat` | 咨询消息 | 与咨询教师的会话列表（未读徽标）；也可从选咨询师卡片/预约详情发起 |
| `/chat/:id` | 咨询沟通 | 单条会话，气泡消息，~3s 轮询接收新消息 |
| `/assistant` | AI 助手 | KB 知识库问答（QaPortal）；对话按会话持久化，可新建/切换/回看历史；文档上传仅管理员 |
| `/profile` | 个人中心 | 资料编辑、**修改密码**（旧密码校验）、邮件通知偏好 |

**教师端**（布局 `src/layout/TeacherLayoutShell.vue`，需教师角色，`/teacher/*`，登录即咨询师本人）

| 路径 | 页面 | 说明 |
|---|---|---|
| `/teacher/review` | 待我审核 | 学生申请我名下咨询档期列表，通过/拒绝（拒绝填原因） |
| `/teacher/consultations` | 我的咨询 | 名下档期被约情况，按状态筛选 |
| `/teacher/messages` | 咨询消息 | 会话列表（未读徽标）；待我审核/我的咨询每行可「回复」学生 |
| `/teacher/messages/:id` | 咨询沟通 | 单条会话，气泡消息，~3s 轮询接收 |
| `/teacher/profile` | 个人中心 | 复用用户个人中心 |

**管理端**（布局 `src/layout/AdminLayoutShell.vue`，需管理员，`/admin/*`）

| 路径 | 页面 | 说明 |
|---|---|---|
| `/admin` | 管理概览 | 驾驶舱：平台用户数、服务模块数等指标卡片 |
| `/admin/services` | 服务治理 | 服务上下架、编辑 / 新增、封面上传 |
| `/admin/bookings` | 预约审核 | 逐条通过 / 拒绝（拒绝必填原因），按校区查看 |
| `/admin/users` | 用户与权限 | 用户搜索、角色管理 |
| `/admin/system` | 系统设置 | **轮播图管理**（上传/删除/拖拽排序，≤6 张）、**通知策略** |
| `/admin/tools` | 工具箱 | 天气查询、二维码生成 |

**公共**：`/login` 登录（图形验证码）、`/register` 邮箱注册、404。

## 目录结构（活跃代码）
```
frontend/
├── src/
│   ├── router/index.ts        # 根路由：/login /register + userRoutes + adminRoutes
│   ├── modules/
│   │   ├── user/              # 用户端：router + views/{dashboard,services,bookings,profile}
│   │   ├── admin/             # 管理端：router（视图在 src/views/admin/*）
│   │   └── assistant/views/QaPortal.vue   # AI 助手
│   ├── views/
│   │   ├── auth/              # LoginPage / RegisterPage / 404
│   │   └── admin/             # 管理概览/服务治理/预约审核/用户权限/系统设置/工具箱
│   ├── layout/                # UserLayoutShell / AdminLayoutShell
│   ├── common/                # stores/user、utils/request、utils/auth、campus 映射等
│   ├── assets/styles/         # global.css（主题变量）+ variables.scss（自动注入）
│   └── services/              # API 调用层（campus 等）
├── vite.config.ts
└── package.json
```
> 早期曾并存一套 `src/views/{dashboard,services,bookings,consultation,rooms,equipment,profile}` 页面；主路由已全部迁到 `src/modules/*`，旧目录仅登录/注册/管理端等仍在使用，其余为遗留副本。

## 快速开始

### 环境
Node.js ≥ 18、npm ≥ 9。开发时后端需按 `backend/README.md` 起好 gateway/cas/kb 与基础设施。

```bash
npm install
npm run dev          # http://localhost:3000（vite 自动打开）
```

### 构建 / 检查
```bash
npm run build        # vue-tsc && vite build（产物 dist/）
npm run type-check   # vue-tsc --noEmit
npm run lint         # eslint . --fix
```

## 代理（vite.config.ts）
所有请求统一打到网关 `localhost:8888`：

| 路径 | 目标 | 说明 |
|---|---|---|
| `/api/v1/kb` | `http://localhost:8888` | KB 路径已带 `/v1`，直接透传 |
| `/api` | `http://localhost:8888` | CAS 路径补 `/v1` 前缀后转发（rewrite `/api` → `/api/v1`） |

网关按 `/api/v1/kb/**` → kb-service、其余 → cas-service 路由。图片静态资源 `/uploads/**`（含轮播图、封面、验证码）同样经 `/api` 前缀代理到网关放行。生产 `frontend/nginx.conf` 保持两条同构的 `location`。

## 登录与账号
- 统一走 CAS 认证（`/api/v1/auth/login`，图形验证码）；token 由 Pinia persistedstate 持久化到 localStorage，路由守卫按角色决定首页/管理端准入。
- 初始种子账号（Flyway V2）：管理员 `admin@campus.com`、普通用户 `user@campus.com`，密码均 `123456`。

## 关键说明
- **主题**：校徽蓝 `#3FB6FF` 收敛在 `src/assets/styles/global.css` 的 `:root` 变量与 `variables.scss`（作为 vite scss `additionalData` 自动注入），改一处全局生效。
- **AI 助手可用性**：依赖后端配置 LLM Key（KB `OPENAI_API_KEY`/`EMBEDDING_API_KEY`，见 `backend/README.md`）；未配置时登录 / 预约等主流程不受影响，仅问答不可用。
- **AI 对话持久化**：/assistant 的问答本就逐条写入 KB `conversation` 表（session_id 维度）；此前前端每次提问新建随机会话导致“看着没存”。现改为**稳定会话**：同一会话续聊沿用、进入页面自动从 `GET /kb/qa/conversation/{sessionId}` 载入历史渲染，并支持「历史会话」下拉切换 / 「新会话」。会话索引按用户存于本地 localStorage（服务端会话数据在 KB 库，跨设备历史列表留待后续加 user 维度）。为避免同会话里“换个问题却重复上一轮答案”，KB 回答为**单句独立问答**（不把历史喂给 LLM/改写器，历史仅用于回看落库）。另：CAS 侧旧的那套孤儿 Qwen `/api/v1/ai/chat` 及其 `ai_chat_history` 表（cas-thirdparty CallTheModel*/AiChatHistory* 链）已于 2026-09-07 整体下线删除——KB `conversation` 表现是唯一的 AI 聊天存储，勿再混淆两套。
- **上传**：封面上传走 `/admin/files`（后端本地 `uploads/`，按子目录存放）；轮播图管理在 `/admin/system` 独立上传并落 `carousel` 目录。
- **服务分类字典**：业务分类（教师咨询/设备借用/教室空间/活动报名）来自后端 `service_category` 表（`GET /app/service-categories`），服务卡片与筛选（业务类别/校区）文案均为库驱动（`categoryName`/校区），新增服务下拉与提交 `categoryId` 同源；已移除前端硬编码的“服务范围=校园统一预约中心 / 使用说明 / 当前可申请”等占位文案，代之以真实服务描述、校区与状态。
