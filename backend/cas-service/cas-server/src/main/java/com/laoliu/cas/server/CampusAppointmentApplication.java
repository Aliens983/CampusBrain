package com.laoliu.cas.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author forever-king
 */
@SpringBootApplication
@MapperScan({"com.laoliu.cas.**.mapper"})
@ComponentScan(basePackages = {"com.laoliu.cas"})
public class CampusAppointmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusAppointmentApplication.class, args);
    }
}
