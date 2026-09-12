# AGENTS.md — cas-server

启动入口模块：聚合全部模块依赖、承载 application.yml 与 Flyway 脚本。**零业务代码**（仅两个功能演示控制器）。权威说明见上层 `../CLAUDE.md`。

## 文件清单

```
cas-server/
├── pom.xml                                        依赖全部业务模块 + starters
└── src/main/
    ├── java/com/laoliu/cas/server/
    │   ├── CampusAppointmentApplication.java      @SpringBootApplication
    │   │                                          @MapperScan("com.laoliu.cas.**.mapper")
    │   │                                          @ComponentScan("com.laoliu.cas")
    │   ├── DbResetConfig.java                     仅 APP_DB_RESET_ON_STARTUP=true 时 clean+migrate（compose 演示）
    │   └── controller/
    │       ├── ConfigDemoController.java          GET /config-demo/greeting（Nacos 热更新演示）
    │       └── SentinelDemoController.java        GET /sentinel-demo/limited（限流演示）
    └── resources/
        ├── application.yml                        全部 ${ENV_VAR}（无明文密钥，仅本地示例默认值）
        └── db/migration/
            ├── V1__init_schema.sql                建表 + 两校区/分类/轮播图种子
            ├── V2__seed_initial_users.sql         admin@ / user@campus.com
            ├── V3__seed_teacher_users.sql         教师账号 + 咨询师回填
            ├── V4__service_category.sql           service_category + 4 类
            └── V5__consult_chat.sql               咨询沟通两表
```

> 已无 `application.yml.example`；历史「yml 内含真实凭据 / SQL 输出 StdOutImpl / 日志 debug」均不成立：
> 当前密钥全部环境变量化，MyBatis 不打印 SQL，日志级别 info。

## 关键配置

| 项 | 值 |
|---|---|
| server.port / context-path | 18080 / `/api/v1` |
| datasource | `jdbc:mysql://localhost:3306/cas_db`，账号 `${DB_USERNAME:root}` / `${MYSQL_ROOT_PASSWORD}` |
| redis | localhost:6379 db0 |
| spring.application.name | cas-service |
| spring.config.import | `optional:nacos:cas-service.yaml` |
| nacos / sentinel | `${NACOS_ADDR:localhost:8848}`；Sentinel 规则源 data-id `cas-sentinel-flow-rules` |
| rabbitmq | `${RABBITMQ_HOST:localhost}:${RABBITMQ_PORT:5672}`，admin/admin123 |
| flyway | enabled，locations=classpath:db/migration，validate-on-migrate=false，clean-disabled=false |
| jwt / internal-sign | `${JWT_SECRET:本地默认}` / `${INTERNAL_SIGN_SECRET:本地默认}`（生产必覆盖） |
| file.upload | dir=uploads，url-prefix=/uploads，server-address=http://localhost:18080 |

## 红线

- ❌ 不写业务代码 / 不放领域实体；新密钥一律环境变量注入并登记 `backend/.env.example`。
- ❌ 不改写历史 V*.sql（Flyway checksum）；新结构增量加 V 文件或对存量库直接执行 SQL。
