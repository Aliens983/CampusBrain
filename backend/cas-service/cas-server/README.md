# cas-server — 启动入口模块

cas-service 的 Spring Boot 启动模块，聚合全部业务模块依赖并承载应用配置。**不编写任何业务代码**。

## 目录结构
```
cas-server/
├── src/main/java/com/laoliu/cas/server/
│   ├── CampusAppointmentApplication.java   # 启动类（@SpringBootApplication + @MapperScan + @ComponentScan）
│   └── controller/                         # 仅演示控制器（无业务）
│       ├── ConfigDemoController.java       # GET /config-demo/greeting（Nacos 配置热更新演示）
│       └── SentinelDemoController.java     # GET /sentinel-demo/limited（限流演示）
├── src/main/resources/
│   ├── application.yml                     # 端口 18080、context-path /api/v1、Nacos/RabbitMQ/DS/Redis、Flyway 等
│   └── db/migration/
│       ├── V1__init_schema.sql             # 全部建表 + 两校区种子数据
│       └── V2__seed_initial_users.sql      # 初始账号 admin@campus.com / user@campus.com
└── pom.xml
```

## 关键配置（application.yml）
| 配置项 | 值 | 说明 |
|---|---|---|
| `server.port` | `18080` | 服务端口 |
| `server.servlet.context-path` | `/api/v1` | 统一前缀（经网关透传） |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/cas_db?...&allowPublicKeyRetrieval=true` | 库 `cas_db`，密码取 `MYSQL_ROOT_PASSWORD` |
| `spring.data.redis` | localhost:6379 | 验证码/限频 |
| `spring.config.import` | `optional:nacos:cas-service.yaml` | Nacos 配置中心 |
| `spring.flyway` | enabled + `locations=classpath:db/migration` | 启动自动迁移建表 |
| `spring.cloud.sentinel.datasource.flow.nacos` | `cas-sentinel-flow-rules` | Sentinel 动态流控规则源 |
| `jwt.*` / `auth.internal-sign-secret` | 环境变量注入 | 密钥不入库 |

## 启动（本地，backend 目录下）
```bash
./scripts/run-local.sh cas        # 自动加载 .env + mvn package + java -jar
# 或
mvn -DskipTests -pl cas-service/cas-server -am package
java -jar cas-server/target/cas-server-1.0.0.jar
```

## 依赖聚合
`cas-server` → `cas-module-system` / `cas-module-appointment` / `cas-module-infra` / `cas-thirdparty` / `cas-framework`（全部 starter）。聚合出唯一可运行 jar；演示控制器仅用于 Nacos/Sentinel 功能演示。
