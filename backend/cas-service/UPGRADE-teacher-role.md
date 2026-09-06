# 服务器升级指引：教师角色 + 活动免审直通

> 适用：**已有数据的服务器库**（如 `/opt/campusbrain` 上 `cas-mysql` 容器里的 `cas_db`）。
> 新机器（空库）无需此文档——Flyway 会从 `V1`（含 `consultant.user_id` 列）+ `V3`（教师种子）自动建好。

## 一、背景改动

- `V1__init_schema.sql`：`consultant` 新增可空列 `user_id`（绑定教师账号）+ 索引（**只对全新机器生效**；老库需下方直接 SQL）。
- `V3__seed_teacher_users.sql`：新建 9 个教师账号（role=3，拼音邮箱，密码同 123456）并按「姓名+service_id」回填 `consultant.user_id`。

## 二、服务器执行步骤（按序）

### 1. 先给老库 `consultant` 加列（必须在发布新版本前执行）

```bash
# 进 cas-mysql 容器（若已部署则用该容器；也可直接连 13306 调试端口）
docker exec -it cas-mysql mysql -uroot -p -e "
ALTER TABLE cas_db.consultant ADD COLUMN user_id BIGINT NULL AFTER service_id,
  ADD KEY idx_consultant_user(user_id);"
```
> 忽略 FK（老库可不建外键）；若列已存在会报 Duplicate column，忽略即可。

### 2. 发布新代码并重启 cas-service

`V1` 内容变化会导致**既有库校验和(migration 1)不匹配**，首次启动会报
`Migration checksum mismatch for migration version 1` 并打印 `-> Resolved locally : <N>`。

修复（沿用既有做法）：
```bash
# 在 cas-mysql 容器里执行（把 <N> 换成启动日志里的 Resolved locally 值）
docker exec -it cas-mysql mysql -uroot -p -e "
UPDATE cas_db.flyway_schema_history SET checksum=<N> WHERE version='1';"
```

### 3. 重启 cas-service

重启后 Flyway 会自动应用 `V3`：创建教师账号并回填 `consultant.user_id`。

### 4. 验证

```bash
# 应有 9 个 role=3 教师账号
docker exec cas-mysql mysql -uroot -p cas_db -e "SELECT id,name,email FROM user WHERE role=3;"
# consultant 已回填：肖/周/刘(仓前心理) · 石/管(仓前辅导) · 姚/裘/孙(下沙心理) · 管(下沙辅导)
docker exec cas-mysql mysql -uroot -p cas_db -e "SELECT id,name,service_id,user_id FROM consultant;"
```

教师登录账号（均 `@campus.com` / 密码 `123456`）：
`xiao / zhou / liu / shi / guan（仓前辅导）/ yao / qiu / sun / guanxs（下沙辅导）`

## 三、回归自测（可选）

- 学生 `user@campus.com` 预约肖老师咨询 → 用 `xiao@campus.com` 登录教师端 `/teacher` 待我审核出现 → 通过/拒绝。
- 学生报名“活动预约”→ 状态直接=已通过、容量+1；开始前取消 → 名额释放。
- 教室 / 设备借用仍只在管理员端 `/admin/bookings` 审核。

## 四、注意事项

- 不要改历史 `V*.sql` 去适配老库（Flyway checksum），结构演进一律用上面的直接 SQL + 校验和修复。
- `V3` 幂等（`INSERT IGNORE`），多环境重复应用不重复建账号。
