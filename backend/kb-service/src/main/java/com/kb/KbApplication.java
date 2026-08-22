package com.kb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Knowledge Base Platform — Main Application Entry
 *
 * <p>An enterprise-grade intelligent Q&A platform powered by RAG
 * (Retrieval-Augmented Generation), enabling semantic search and
 * precise answers over private enterprise documents.</p>
 * @author forever-king
 */
@SpringBootApplication
@EnableAsync
@EnableFeignClients(basePackages = "com.kb.infrastructure.client")
@ComponentScan(basePackages = {"com.kb", "com.laoliu.auth"})
public class KbApplication {

    public static void main(String[] args) {
        SpringApplication.run(KbApplication.class, args);
    }
}
