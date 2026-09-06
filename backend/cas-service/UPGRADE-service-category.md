# 服务器升级指引：服务分类落库（service_category + services.category_id）

> 适用：**已有数据的服务器库**（`/opt/campusbrain` 上 `cas-mysql` 容器的 `cas_db`）。
> 全新机器无需此文档——V1 直接建出 `services.category_id`（不存 `category` 串），V4 只补分类字典。
> 迁移约定：老库结构演进一律**直接 SQL**，**不改写历史 V*.sql**，避免 Flyway checksum 冲突。

## 背景
- `service_category` 固定 4 类：`1 教师咨询 · 2 设备借用 · 3 教室空间 · 4 活动报名`。
- `services` 不再存 `category` 编码串，改为 `category_id`（**代码级外键**，不建 DB 外键）。
- `V4__service_category.sql` 仅建表 + `INSERT IGNORE` 种子（幂等，老库执行也安全）。

## 服务器执行步骤（按序）

### 1. 停旧服务 → 给老库改结构（必须在换新版前，且旧版运行中不要执行，避免 `category` 列消失导致旧代码查询报错）

```bash
# 停掉 cas-service 容器后执行
docker exec cas-mysql mysql -uroot -p -e "
ALTER TABLE cas_db.services
  ADD COLUMN category_id INT NULL COMMENT '业务分类ID（引用 service_category.id）' AFTER image_url,
  ADD KEY idx_services_category (category_id);

UPDATE cas_db.services s
  LEFT JOIN (
    SELECT 1 AS id, 'teacher' AS code
    UNION ALL SELECT 2, 'equipment'
    UNION ALL SELECT 3, 'space'
    UNION ALL SELECT 4, 'activity'
  ) c ON c.code = s.category
SET s.category_id = c.id;

ALTER TABLE cas_db.services DROP COLUMN category;"
```
> `category_id` 已存在会报 Duplicate column，忽略即可；老库若还有非四类的历史值会留 `NULL`（当前无此类数据）。

### 2. 发布新代码并重启 cas-service

V1 内容已改为新机器建 `category_id`，会导致**既有库 v1 校验和不匹配**，启动日志打印
`Migration checksum mismatch for migration version 1` + `-> Resolved locally : <N>`。

修复（沿用既有做法，把 `<N>` 换成日志里的 Resolved locally 值）：
```bash
docker exec cas-mysql mysql -uroot -p -e "
UPDATE cas_db.flyway_schema_history SET checksum=<N> WHERE version='1';"
```

### 3. 再次重启 cas-service

重启后 Flyway 自动应用 `V4`：建 `service_category`（已存在则跳过）+ 补 4 行种子（幂等）。
> 若此前曾误跑过“带 ALTER 的 V4”中间版，先删历史行让新版纯新增 V4 重放：
> `DELETE FROM cas_db.flyway_schema_history WHERE version='4';`

### 4. 验证

```bash
docker exec cas-mysql mysql -uroot -p cas_db -e "SELECT id,code,name FROM service_category ORDER BY id;"
docker exec cas-mysql mysql -uroot -p cas_db -e "SELECT service_id,service_name,category_id FROM services ORDER BY service_id;"
# 应无 category 列、category_id 已回填（1教师咨询/2设备借用/3教室空间/4活动报名）
docker exec cas-mysql mysql -uroot -p cas_db -e "SHOW COLUMNS FROM services LIKE 'category%';"
```

## 回归自测（可选）
- 管理员端「新增服务」下拉应显示 4 类（来自 `GET /app/service-categories`），创建后列表“业务类别”为中文名。
- 学生预约“活动报名”仍免审直通、可开始前取消；教师端“待我审核”只见咨询档期，不受影响。
