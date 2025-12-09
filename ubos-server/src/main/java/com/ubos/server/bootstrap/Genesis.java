package com.ubos.server.bootstrap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ubos.kernel.service.LcmKernelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 【创世程序】纯数据驱动版
 * 不依赖任何 Entity POJO，直接构建 Map/JSON 数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Genesis { // 移除 CommandLineRunner 接口，改为被 SystemBootstrapper 调用

    private final LcmKernelService kernelService;
    private final ObjectMapper objectMapper;

    private static final String BRANCH_MAIN = "master";
    private static final String AUTHOR_SYSTEM = "system.genesis";

    /**
     * 对外暴露的执行方法
     */
    public Mono<Void> execute() {
        log.info("🌌 [Genesis] Igniting with Pure Data...");

        return Mono.empty()
            .then(commitMetaType())
            .then(commitMoneyType())
            .then(commitDesktopSchema())
            .then(commitGreetingLogic())
            .then(commitAdminUser()) // 顺便把管理员也造出来
            .then();
    }

    // --- 1. 元类型定义 (Meta Type) ---
    private Mono<Void> commitMetaType() {
        // 直接用 Map，不要 new Entity()
        Map<String, Object> data = Map.of(
            "name", "Meta Type Definition",
            "fields", List.of(
                Map.of("name", "slug", "type", "string", "required", true),
                Map.of("name", "schema", "type", "json")
            )
        );
        return internalCommit("TYPE_DEF", "type.meta", data, "Genesis: Meta Type");
    }

    // --- 2. 金额类型 (Money Type) ---
    private Mono<Void> commitMoneyType() {
        Map<String, Object> data = Map.of(
            "name", "Currency Amount",
            "precision", 2,
            "currency", "USD",
            "validator_ref", "ubos://ctx/logic/validate.money"
        );
        return internalCommit("TYPE_DEF", "type.primitive.money", data, "Genesis: Money Type");
    }

    // --- 3. 默认桌面 UI (Desktop Schema) ---
    private Mono<Void> commitDesktopSchema() {
        Map<String, Object> data = Map.of(
            "wallpaper", "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?q=80&w=2070",
            "theme", "dark",
            "icons", List.of(
                // --- 图标 1: 我的钱包 ---
                Map.of(
                    "id", "app_wallet",
                    "title", "我的钱包",
                    "icon", "💰",
                    // 【修正点】: 必须补全 ui_schema，否则窗口打开是空的
                    "ui_schema", Map.of(
                        "type", "v-stack",
                        "children", List.of(
                            Map.of(
                                "type", "text",
                                "props", Map.of("content", "当前余额: $1024.00", "size", "24px", "color", "#4caf50")
                            ),
                            Map.of(
                                "type", "button",
                                "props", Map.of("label", "提现测试", "action", "logic.wallet.withdraw")
                            )
                        )
                    )
                ),
                // --- 图标 2: 应用市场 ---
                Map.of(
                    "id", "app_store",
                    "title", "应用市场",
                    "icon", "🛍️",
                    "ui_schema", Map.of(
                        "type", "v-stack",
                        "children", List.of(
                            Map.of("type", "text", "props", Map.of("content", "欢迎来到 Logrum Store"))
                        )
                    )
                )
            ),
            "start_menu", List.of("profile", "settings")
        );
        return internalCommit("VIEW", "schema.desktop.default", data, "Genesis: Desktop UI");
    }
    // --- 4. 问候逻辑 (Logic) ---
    private Mono<Void> commitGreetingLogic() {
        String code = """
            if (ctx.hour < 12) return "Good Morning, " + ctx.name
            else return "Good Afternoon, " + ctx.name
        """;

        // 逻辑的内容通常直接就是代码字符串，或者是包含 meta 的 map
        Map<String, Object> data = Map.of(
            "engine", "groovy",
            "content", code
        );
        return internalCommit("LOGIC", "logic.sys.greeting", data, "Genesis: Greeting Logic");
    }

    // --- 5. 默认管理员 (Admin User) ---
    private Mono<Void> commitAdminUser() {
        Map<String, Object> data = Map.of(
            "email", "admin@logrum.org",
            "password", "admin123", // MVP明文
            "name", "System Administrator",
            "role", "ADMIN",
            "avatar", "https://api.dicebear.com/7.x/bottts/svg?seed=admin"
        );
        // 这里 slug 是 user_admin
        return internalCommit("USER", "user_admin", data, "Genesis: Admin User");
    }

    // --- 核心提交方法 ---
    private Mono<Void> internalCommit(String type, String slug, Map<String, Object> data, String msg) {
        try {
            // 1. 把 Map 转成 JSON 字符串
            String jsonContent = objectMapper.writeValueAsString(data);

            // 2. 调用内核服务 (您提供的 LcmKernelService)
            return kernelService.commit(
                type,
                slug,
                BRANCH_MAIN,
                jsonContent,
                AUTHOR_SYSTEM,
                msg
            ).then();

        } catch (JsonProcessingException e) {
            return Mono.error(new RuntimeException("Genesis JSON Error", e));
        }
    }
}