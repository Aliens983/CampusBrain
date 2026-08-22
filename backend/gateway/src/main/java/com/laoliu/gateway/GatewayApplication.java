package com.laoliu.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 统一入口网关。
 *
 * @author forever-king
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.laoliu.gateway", "com.laoliu.auth"})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
