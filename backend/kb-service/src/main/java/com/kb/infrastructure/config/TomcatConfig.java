package com.kb.infrastructure.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tomcat 配置 — 允许 URL 中包含中文等非 ASCII 字符
 *
 * @author forever-king
 */
@Configuration
public class TomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            connector.setURIEncoding("UTF-8");
            connector.setProperty("relaxedQueryChars", "[]|{}^\\`\"<>");
        });
    }
}
