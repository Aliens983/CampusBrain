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
| `/bookings/:id` | 预约详情 | 单条详情 / 取消 |
| `/assistant` | AI 助手 | KB 知识库问答（QaPortal）；文档上传仅管理员 |
| `/profile` | 个人中心 | 资料编辑、**修改密码**（旧密码校验）、邮件通知偏好 |

**教师端**（布局 `src/layout/TeacherLayoutShell.vue`，需教师角色，`/teacher/*`，登录即咨询师本人）

| 路径 | 页面 | 说明 |
|---|---|---|
| `/teacher/review` | 待我审核 | 学生申请我名下咨询档期列表，通过/拒绝（拒绝填原因） |
| `/teacher/consultations` | 我的咨询 | 名下档期被约情况，按状态筛选 |
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
- **上传**：封面上传走 `/admin/files`（后端本地 `uploads/`，按子目录存放）；轮播图管理在 `/admin/system` 独立上传并落 `carousel` 目录。
