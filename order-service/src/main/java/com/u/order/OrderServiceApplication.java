package com.u.order;

import jakarta.annotation.PostConstruct;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.u.order.mapper")
@SpringBootApplication(scanBasePackages = {"com.u.order", "com.u.common", "com.u.api"})
@EnableScheduling
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.u.api.client")
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
    @Autowired
    private Environment env;
    @PostConstruct
    public void debug() {
        System.out.println("rabbitmq.host = " +
                env.getProperty("spring.rabbitmq.host"));
    }
}
