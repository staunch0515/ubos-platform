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

        // -----------------------------------------------------------
        // 🚫 免疫规则 1: 禁止反射 (防止绕过沙箱)
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
        // 禁止定义新方法 (防止脚本变得过于复杂)
        secure.setMethodDefinitionAllowed(false);

        // 注意：标准 Groovy SecureASTCustomizer 不支持 setTypeDefinitionAllowed
        // 我们主要靠禁止 ClassLoader 和反射来防止类定义带来的危害

        // -----------------------------------------------------------
        // 💉 注入配置
        // -----------------------------------------------------------
        CompilerConfiguration config = new CompilerConfiguration();
        config.addCompilationCustomizers(secure);
        return config;
    }
}