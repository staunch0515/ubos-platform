package com.ubos.engine.config;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ScriptSecurityConfig {

    @Bean
    public CompilerConfiguration secureCompilerConfig() {
        SecureASTCustomizer secure = new SecureASTCustomizer();

        // 【免疫系统规则】

        // 1. 禁止调用危险的 Java 系统类
        secure.setDisallowedReceivers(List.of(
            "java.lang.System",
            "java.lang.Runtime",
            "java.lang.ProcessBuilder",
            "java.io.File"
        ));

        // 2. 禁止创建线程 (防止资源耗尽攻击)
        secure.setDisallowedImports(List.of(
            "java.lang.Thread",
            "java.util.concurrent.*"
        ));

        // 3. (可选) 限制循环深度等...

        CompilerConfiguration config = new CompilerConfiguration();
        config.addCompilationCustomizers(secure);
        return config;
    }
}