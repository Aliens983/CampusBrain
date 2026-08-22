package com.kb.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / SpringDoc OpenAPI 配置
 * <p>
 * 访问地址：
 * <ul>
 *   <li>Swagger UI：<a href="http://localhost:8080/swagger-ui.html">/swagger-ui.html</a></li>
 *   <li>Knife4j 增强 UI：<a href="http://localhost:8080/doc.html">/doc.html</a></li>
 *   <li>OpenAPI JSON：<a href="http://localhost:8080/v3/api-docs">/v3/api-docs</a></li>
 * </ul>
 * </p>
 * @author forever-king
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("知识库智能问答平台 API")
                        .description("Enterprise Intelligent Knowledge Base Q&A Platform — "
                                + "基于 RAG 架构的企业级知识库问答系统。\n\n"
                                + "## 认证方式\n"
                                + "所有业务 API 需在 Header 中携带 `Authorization: Bearer {accessToken}`。\n"
                                + "开发环境可使用 `Authorization: Bearer test1` Mock 登录。\n\n"
                                + "## 双 Token 机制\n"
                                + "- **Access Token**：2 小时有效期，用于业务请求\n"
                                + "- **Refresh Token**：7 天有效期，用于刷新 Access Token")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("forever-king")
                                .url("https://github.com/forever-king"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .schemaRequirement("Bearer", new SecurityScheme()
                        .name("Bearer")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("UUID")
                        .description("输入 Access Token（不含 'Bearer ' 前缀）"));
    }
}
