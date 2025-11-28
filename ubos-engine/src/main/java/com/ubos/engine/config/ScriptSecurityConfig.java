package com.ubos.engine.config;

// 👇 必须要有这几个 Groovy 的 import
import groovy.transform.ThreadInterrupt;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
// 👆 缺的就是上面这几行

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ScriptSecurityConfig {

    @Bean
    public CompilerConfiguration secureCompilerConfig() {
        SecureASTCustomizer secure = new SecureASTCustomizer();

        // -----------------------------------------------------------
        // 🚫 免疫规则 1: 禁止反射
        // -----------------------------------------------------------
        secure.setDisallowedReceivers(List.of(
            "java.lang.System",
            "java.lang.Runtime",
            "java.lang.Class",
            "java.lang.ClassLoader",
            "java.lang.ProcessBuilder",
            "java.lang.Thread",
            "java.lang.ThreadGroup"
        ));

        // -----------------------------------------------------------
        // 🚫 免疫规则 2: 禁止底层网络与IO
        // -----------------------------------------------------------
        secure.setDisallowedImports(List.of(
            "java.io.File",
            "java.io.FileInputStream",
            "java.io.FileOutputStream",
            "java.nio.*",
            "java.net.Socket",
            "java.net.ServerSocket",
            "java.sql.*",
            "java.util.concurrent.*",
            "java.lang.reflect.*"
        ));

        // -----------------------------------------------------------
        // 🚫 免疫规则 3: 语法限制
        // -----------------------------------------------------------
        secure.setMethodDefinitionAllowed(false);

        // -----------------------------------------------------------
        // 💉 注入配置 (含超时熔断机制)
        // -----------------------------------------------------------
        CompilerConfiguration config = new CompilerConfiguration();
        config.addCompilationCustomizers(secure);

        // 【关键】注入中断检查，配合 Future.cancel() 实现超时杀线程
        config.addCompilationCustomizers(new ASTTransformationCustomizer(ThreadInterrupt.class));

        return config;
    }
}