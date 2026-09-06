# cas-service — 校园预约服务

> 本模块是 CampusBrain 微服务体系中的**校园预约系统（CAS）**独立服务，端口 **18080**（servlet context-path `/api/v1`）。
> 整体架构、端口、启动与部署见 **`../README.md`**；本文件聚焦 cas-service 的模块划分、领域模型与接口。

CAS 以 **杭州师范大学两校区（仓前 cq / 下沙 xs）** 建模：服务目录、咨询师、教室、设备均按校区分离；覆盖咨询、教室、设备、活动四类预约，全部走 Flyway 自动建表 + 种子数据。

## 技术栈

| | |
|---|---|
| 框架 | Spring Boot 3.3.5 · Spring Cloud Alibaba 2023.0.1.2（Nacos 注册/配置 + Sentinel） |
| 数据 | MyBatis-Plus 3.5.5 · MySQL 8（库 `cas_db`）· Redis（验证码/限频）· **Flyway**（迁移） |
| 消息/通知 | RabbitMQ（发布 `appointment.changed`）· JavaMail（审核邮件）· 阿里云短信/OSS（可选） |
| 结构 | DDD 四层 + Maven 多模块（`com.laoliu.cas`） |

## Maven 模块

```
cas-service/
├── cas-dependencies    依赖 BOM（第三方版本统一）
├── cas-framework       框架聚合：cas-common + 各 cas-spring-boot-starter-*（web/security/mybatis/redis/test）
├── cas-module-infra    基础设施服务：本地文件上传(按子目录 uuid 命名)、OSS、二维码、邮件
├── cas-module-system   用户与账号：登录/注册/图形验证码/邮箱验证码/忘记密码/改密/角色/通知策略
├── cas-module-appointment  预约核心：服务目录、四类预约、审核、时段/库存防冲突、自动完成、轮播图
├── cas-thirdparty      第三方集成：天气 / Qwen AI(/ai) / 阿里云
└── cas-server          启动入口：application.yml、@MapperScan、Demo 控制器、Flyway 脚本
```

依赖约束：`infra` 只依赖 framework；业务模块经 `api/` 接口互相调用、不直接注入对方 Mapper；`server` 不写业务代码。

## DDD 分层

业务模块内统一 `interfaces（controller admin/app + dto）/ application（service + impl）/ domain（entity + repository）/ infrastructure（persistence：dataobject + mapper + repositoryImpl）/ api（跨模块接口）`。`domain/` 保持纯 Java、零框架注解。轮播图（carousel）作为独立子包位于 `cas-module-appointment`。

## 预约领域模型（对应 `db/migration/V1` 表）

```
user ───────────────┐
services（服务目录：category/campus/image_url/capacity/booked_count）
 ├─ consultant     咨询师（挂 心理咨询/学业辅导 服务）
 │    └─ time_slot 该咨询师某日可约时段
 ├─ room           教室（挂 空闲教室 服务）
 ├─ equipment      设备（total_stock / available_stock / unit / location）
 ├─ item ──────────┘ 预约单：用户 × 服务 × 资源
carousel            首页轮播图（image_url/sort/enabled）
notification_policy 全局通知策略（单行，邮件通道开关）
file_info / ai_chat_history
```

`item`（预约单）关键列：`service_id` + 资源列其一 —— 咨询 `consultant_id/slot_id/slot_date/start_time/end_time`；设备 `equipment_id/quantity`；教室 `room_id + slot_date/start_time/end_time`。`manage_status`：`0 待审 → 1 通过 / 2 拒绝 / 3 取消 / 4 完成`，拒绝必有 `reason`。

### 四类预约与防冲突
| 类型 | 数据 | 防冲突/防超卖策略 |
|---|---|---|
| 咨询 | `consultant` + `time_slot` | 同一咨询师同时段占用即冲突；`slot_date/start_time/end_time` 重叠查询 + 行锁 |
| 教室 | `room`（空闲教室服务） | **一间教室同一时间段仅一人可约**，唯一窗口 + 重叠拒绝 |
| 设备 | `equipment` 窗口借用 | `available_stock` 库存原子扣减 + 时段窗口；取消/拒绝回补 |
| 活动 | `services.capacity` | `capacity`(-1 不限)/`booked_count` 原子扣减，超额返回 `BOOKING_CAPACITY_FULL` |

- **释放**：审核拒绝 / 用户取消自动释放占用的时段与库存。
- **自动完成**：`BookingAutoCompleteTask`（`@EnableScheduling`，60s 轮询）将已过预约窗口的单自动置为 `完成`（设备到点归还、教室释放）。

## 主要接口

统一经网关前缀 `/api/v1` 访问（context-path `/api/v1`，gateway 透传；本地直连 18080 也相同）。按控制器分组（网关 8888 校验 JWT → `@RequireRole` 授权）：

| 分组 | 控制器路径 | 说明 |
|---|---|---|
| 账号 | `POST /auth/login|/reset`、`POST /auth/register`、`POST /auth/verification-code`、`GET /captcha`、`GET/PUT /users/*` | 登录/忘记密码/邮箱验证码注册/图形验证码/资料·改密 |
| 服务目录 | `GET /app/services`（列表/详情/`mine`） | 用户端浏览服务（带分类/校区/封面） |
| 预约 | `POST /app/bookings/room\|equipment\|consultation`、`GET /app/bookings/{id}` | 三类资源预约；`/app/bookings/mine` 我的预约 |
| 咨询资源 | `GET /app/consultations`、`GET /app/consultations/{id}/slots` | 咨询师列表 / 可约时段 |
| 教室/设备资源 | `GET /app/rooms`、`POST /app/rooms/{id}/book`、`GET /app/equipment(/categories)` | 资源浏览（详情含时段/库存） |
| 余量 | `GET /appointments/availability` | 实时余量（供 KB Function Calling 只读调用，内网签名鉴权） |
| 轮播图 | `GET /app/carousel`、`GET/POST/DELETE /admin/carousel`、`POST /admin/carousel/reorder` | 用户端启用列表；管理端上传(≤6)/删除/拖拽排序 |
| 管理端 | `GET /admin/services`(+`PUT`)、`GET/POST /admin/bookings`、`GET/PUT /admin/users`、`GET/PUT /admin/settings/notify`、`POST /admin/files` | 服务治理/预约审核/用户角色/通知策略/封面上传 |
| 其他 | `GET /weather/local`、`POST /ai/chat`、`GET /app/qr-code`、`GET /config-demo/greeting`、`GET /sentinel-demo/limited` | 天气 / AI / 二维码 / Nacos 热更新与 Sentinel 演示 |

## 数据库迁移与种子（Flyway）

迁移位于 `cas-server/src/main/resources/db/migration/`：
- `V1__init_schema.sql` —— 全部建表 + 校区种子：cq/xs 两套服务目录、咨询师（仓前肖/周/刘/石/管、下沙姚/裘/孙/管）、教室（勤园/恕园/A~E 号楼）、设备、初始轮播图 6 张。
- `V2__seed_initial_users.sql` —— 初始账号：`admin@campus.com` 与 `user@campus.com`，密码均 `123456`（BCrypt，登录后请改密）。

新机器首次启动 CAS 自动建库建表；**已有库**不改写历史 `V*.sql`（Flyway checksum），结构演进直接对库执行 SQL（约定见 `../README.md`）。

## 构建 / 运行 / 测试

```bash
# 本地起服务（backend 目录下，自动加载 .env + 打包）
cd ../..
./scripts/run-local.sh cas        # :18080，首次启动执行 Flyway

# 构建产物
mvn clean package -DskipTests     # cas-server/target/cas-server-1.0.0.jar

# 测试（appointment/infra/system/thirdparty 共 73 个测试方法）
mvn -B -pl cas-service -am test
```

## 代码约定
- Controller 返回 `CommonResult<T>`；业务异常抛 `BusinessException(ErrorCode)`。
- 当前用户取 `SecurityFrameworkUtils.getLoginUser()`；权限用 `@RequireRole` 注解。
- 密钥一律经环境变量注入（`.env`），不入库。
