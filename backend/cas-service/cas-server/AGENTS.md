# AGENTS.md — cas-server

Application entry point. Aggregates all module dependencies, holds `application.yml`, and configures component scanning. Contains **ZERO business code**.

## File List

```
cas-server/
├── pom.xml                                          ← depends on ALL modules
└── src/main/
    ├── java/com/laoliu/cas/server/
    │   └── CampusAppointmentApplication.java         ← @SpringBootApplication
    └── resources/
        ├── application.yml                           ← REAL CREDENTIALS — treat as secrets
        └── application.yml.example                   ← template without real values
```

## CampusAppointmentApplication.java

```java
@SpringBootApplication
@MapperScan("com.laoliu.cas.**.mapper")      // scans all modules' mappers
@ComponentScan("com.laoliu.cas")              // scans all modules' components
public class CampusAppointmentApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusAppointmentApplication.class, args);
    }
}
```

**Key points:**
- `@MapperScan` with wildcard `com.laoliu.cas.**.mapper` → picks up all MyBatis mappers from all modules automatically
- `@ComponentScan("com.laoliu.cas")` → picks up all `@Service`/`@Component`/`@Repository`/`@Controller` from all modules
- New packages under `com.laoliu.cas` are picked up WITHOUT any config change

## application.yml Configuration

### Server
- Port: **18080** (not 8080)

### Database
- MySQL: `jdbc:mysql://localhost:3306/cas_db`
- User: `root`
- MyBatis-Plus: `map-underscore-to-camel-case: true`, SQL stdout logging (StdOutImpl)

### Redis
- Host: `localhost:6379`, database 0

### Mail (163.com SMTP)
- Host: `smtp.163.com`, port 465, SSL
- Username: `dmregy@163.com` (real credential)

### JWT
- Base64-encoded secret
- Expiration: 86400000 ms (24 hours)

### File Upload
- Directory: `./uploads/`
- URL prefix: `/api/files/`
- Server address: `http://localhost:18080`

### Email Verification
- Code expiration: 300 seconds (5 min)
- Send frequency limit: 60 seconds

### External APIs
- Weather: cn.apihz.cn (real API key/ID in config)
- Aliyun OSS: placeholder credentials (`your-access-key-id`)
- Aliyun SMS: real credentials in config
- AI (Qwen): DashScope API

### Actuator
- Exposed: `health`, `info` only
- Show details: `when_authorized`

### Knife4j / Swagger
- Access: `http://localhost:18080/doc.html`
- Permit-all paths for swagger resources

### Logging
- `com.laoliu: debug`
- MyBatis SQL logging via `StdOutImpl` (should be dev-profile only)

## Dependency Aggregation

cas-server's pom.xml depends on:
- `cas-module-system`
- `cas-module-appointment`
- `cas-module-infra`
- `cas-thirdparty`
- All `cas-spring-boot-starter-*` modules
- `spring-boot-starter-actuator`
- `knife4j-openapi3-jakarta-spring-boot-starter`
- `mysql-connector-j` (runtime)

## ANTI-PATTERNS

- ❌ **NO business code** — no services, no entities, no controllers in this module
- ❌ **NO domain entities** — entities belong in their business modules
- ❌ **NO new credentials in application.yml** — use env vars, add to `.example` instead
