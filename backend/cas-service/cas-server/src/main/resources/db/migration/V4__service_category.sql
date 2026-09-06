-- ---------------------------------------------------------------------------
-- V4 服务业务分类字典（固定 4 类，不可在管理端增删改）
-- 仅新增一张分类表 + 种子，不改动任何既有表结构。
--   · 全新机器：services.category_id 已由 V1 建好并直接种入数值 id，本脚本仅补分类字典；
--   · 已有库：先按升级指引直接执行 SQL 演进（ALTER 加 category_id + 回填 + DROP category），
--     本脚本同样只会建表/补种子（IF NOT EXISTS / INSERT IGNORE 幂等），安全可重复执行。
-- services.category_id 为代码级外键（不建 DB 外键），读写由后端按 id 回填编码与中文名。
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS service_category (
    id         INT         NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    code       VARCHAR(20) NOT NULL COMMENT '分类编码: teacher/equipment/space/activity',
    name       VARCHAR(20) NOT NULL COMMENT '分类中文名',
    sort       INT         NOT NULL DEFAULT 0 COMMENT '展示排序',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '服务业务分类（固定4类种子）';

INSERT IGNORE INTO service_category (id, code, name, sort) VALUES
    (1, 'teacher',   '教师咨询', 1),
    (2, 'equipment', '设备借用', 2),
    (3, 'space',     '教室空间', 3),
    (4, 'activity',  '活动报名', 4);
