package com.sec;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.sec.mapper")
@SpringBootApplication
@EnableScheduling
public class SecServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SecServerApplication.class, args);
    }
}