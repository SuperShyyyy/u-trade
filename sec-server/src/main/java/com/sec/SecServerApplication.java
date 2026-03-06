package com.sec;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@MapperScan("com.sec.mapper")
@SpringBootApplication
public class SecServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SecServerApplication.class, args);
    }
}