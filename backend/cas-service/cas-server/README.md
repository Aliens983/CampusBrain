# README.md - cas-server

## 模块职责

cas-server 是应用启动入口模块，聚合所有业务模块的依赖，配置应用启动参数。

## 核心功能

- **启动入口**: Spring Boot应用启动类
- **依赖聚合**: 聚合所有业务模块
- **配置管理**: 统一管理应用配置

## 目录结构

```
cas-server/
├── src/main/java/com/laoliu/cas/server/
│   └── CampusAppointmentApplication.java  # 启动类
├── src/main/resources/
│   └── application.yml                   # 配置文件
└── pom.xml                              # 聚合依赖
```

## 启动说明

### 编译

```bash
mvn clean package -DskipTests
```

### 运行

```bash
java -jar cas-server/target/cas-server-1.0.0.jar
```

### 访问

启动后访问 http://localhost:8080

## 配置说明

主要配置项：

| 配置项 | 说明 |
|--------|------|
| server.port | 服务端口 |
| spring.datasource | 数据库连接配置 |
| spring.data.redis | Redis连接配置 |
| jwt.secret | JWT密钥 |
| jwt.expiration | Token过期时间 |
| file.upload.dir | 文件上传目录 |

## 依赖关系

```
cas-server
    │
    ├──> cas-module-system
    ├──> cas-module-infra
    ├──> cas-module-appointment
    ├──> cas-thirdparty-aliyun
    │
    ├──> cas-spring-boot-starter-web
    ├──> cas-spring-boot-starter-security
    ├──> cas-spring-boot-starter-mybatis
    ├──> cas-spring-boot-starter-redis
    └──> cas-spring-boot-starter-mq
```

## 注意事项

1. server 模块不编写任何业务代码
2. 所有业务逻辑在对应的业务模块中实现
3. 配置文件敏感信息建议使用环境变量或配置中心
