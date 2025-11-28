package com.ubos.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@SpringBootApplication
// 扫描所有模块的组件 (Common, Kernel, Engine)
@ComponentScan(basePackages = "com.ubos")
// 扫描 Kernel 里的 R2DBC Repository
@EnableR2dbcRepositories(basePackages = "com.ubos.kernel.repository")
public class UbosApplication {

    public static void main(String[] args) {
        SpringApplication.run(UbosApplication.class, args);
        System.out.println("🚀 UBOS (Universal Business OS) Started Successfully!");
    }
}