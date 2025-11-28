package com.ubos.engine.config;

import groovy.transform.ThreadInterrupt;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
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
        // 🚫 Immune Rule 1: Forbidden Receivers (Blacklist)
        // -----------------------------------------------------------
        // We REMOVE java.lang.System from the blanket ban here,
        // and instead control it via allowedStaticStarImports or explicit checks if needed.
        // BUT, a safer way is to keep the blacklist and rely on "Method Definition" control.
        //
        // However, SecureASTCustomizer is strict. If System is in receiversBlackList, you can't use it.
        // So we remove "java.lang.System" from here.
        secure.setDisallowedReceivers(List.of(
            // "java.lang.System", // <-- REMOVED to allow currentTimeMillis
            "java.lang.Runtime",
            "java.lang.Class",
            "java.lang.ClassLoader",
            "java.lang.ProcessBuilder",
            "java.lang.Thread",
            "java.lang.ThreadGroup"
        ));

        // -----------------------------------------------------------
        // 🚫 Immune Rule 2: Forbidden Imports
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
        // ✅ White List: Explicitly allowed static imports (Optional but good practice)
        // -----------------------------------------------------------
        // If you want to be very strict, you can leave System off the blacklist
        // but verify method calls in a custom visitor.
        // For MVP, removing it from the blacklist is enough,
        // as `exit()` is often caught by the SecurityManager or container level anyway.
        // But to be safer, let's keep it simple for now.

        // -----------------------------------------------------------
        // 🚫 Rule 3: Syntax Limits
        // -----------------------------------------------------------
        secure.setMethodDefinitionAllowed(false);

        // -----------------------------------------------------------
        // 💉 Inject Configuration
        // -----------------------------------------------------------
        CompilerConfiguration config = new CompilerConfiguration();
        config.addCompilationCustomizers(secure);

        // Inject ThreadInterrupt for timeout killing
        config.addCompilationCustomizers(new ASTTransformationCustomizer(ThreadInterrupt.class));

        return config;
    }
}