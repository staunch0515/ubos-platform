package com.ubos.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.scheduling.annotation.EnableScheduling; // 👈 之前缺的就是这行

@SpringBootApplication
// 扫描所有模块组件 (Kernel, Engine, Common)
@ComponentScan(basePackages = "com.ubos")
// 扫描数据库仓库
@EnableR2dbcRepositories(basePackages = "com.ubos.kernel.repository")
// 开启定时任务调度器 (生物钟)
@EnableScheduling
public class UbosApplication {

    public static void main(String[] args) {
        SpringApplication.run(UbosApplication.class, args);
        System.out.println("🚀 UBOS (Universal Business OS) Started Successfully!");
    }
}