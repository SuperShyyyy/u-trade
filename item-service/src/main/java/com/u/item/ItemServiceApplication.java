package com.u.item;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.sql.Connection;

@MapperScan("com.u.item.mapper")
@EnableDiscoveryClient
// Feign 扫描 UserClient 所在包
@EnableFeignClients(basePackages = "com.u.api.client.user")
@SpringBootApplication(scanBasePackages = {"com.u.item", "com.u.common", "com.u.api"})
public class ItemServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ItemServiceApplication.class, args);
    }
    @Bean
    public ApplicationRunner debugDataSource(DataSource dataSource) {
        return args -> {
            try (Connection conn = dataSource.getConnection()) {
                System.out.println("\n🔍 🔍 🔍 数据源调试信息 🔍 🔍 🔍");
                System.out.println("✅ 实际数据库名: " + conn.getCatalog());
                System.out.println("✅ JDBC URL: " + conn.getMetaData().getURL());
                System.out.println("✅ 当前用户: " + conn.getMetaData().getUserName());
                System.out.println("🔍 🔍 🔍 🔍 🔍 🔍 🔍 🔍 🔍 🔍 🔍 \n");
            } catch (Exception e) {
                System.err.println("获取数据源信息失败: " + e.getMessage());
            }
        };
    }
}
