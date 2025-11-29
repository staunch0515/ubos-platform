package com.ubos.kernel.util;

import io.r2dbc.spi.R2dbcException;
import org.springframework.dao.TransientDataAccessResourceException;
import reactor.util.retry.Retry;

import java.time.Duration;

public final class ReactiveRetry {

    private ReactiveRetry() {} // Utility class

    /**
     * 定义标准的 UBOS 数据库瞬时错误重试策略：
     * 最大重试 3 次，初始间隔 1 秒，使用指数退避。
     */
    public static Retry databaseTransientErrors() {
        return Retry.backoff(3, Duration.ofSeconds(1))
            .filter(ReactiveRetry::isTransientError) // 只对可重试的错误进行重试
            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                new RuntimeException("UBOS Database resilience exhausted after " +
                                     retrySignal.totalRetries() + " attempts.", retrySignal.failure())
            );
    }

    /**
     * 判断异常是否为瞬时错误 (Transient Error)
     * 只有瞬时错误才重试。致命错误（如 SQL 语法错）应立即失败。
     */
    private static boolean isTransientError(Throwable throwable) {
        // 1. Spring Data 封装的瞬时资源访问错误 (如连接中断)
        if (throwable instanceof TransientDataAccessResourceException) {
            return true;
        }
        // 2. R2DBC 驱动抛出的底层连接错误 (通常为 R2dbcException)
        if (throwable instanceof R2dbcException) {
            return true;
        }
        // 3. 如果错误信息包含连接重置 (Reactor Netty 常见)
        if (throwable.getMessage() != null && throwable.getMessage().contains("Connection reset")) {
            return true;
        }

        // 其他异常（如 ValidationException, NullPointerException）属于致命错误，不重试
        return false;
    }
}