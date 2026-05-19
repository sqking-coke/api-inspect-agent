package com.inspect.agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

/**
 * 通用重试工具，支持固定间隔的重试策略。
 * <p>重试次数包含首次调用（即 maxRetries=2 表示最多执行 3 次）。
 */
@Slf4j
@Component
public class RetryTool {

    /**
     * 带重试执行任务。
     *
     * @param task           待执行的任务
     * @param maxRetries     最大重试次数（不含首次）
     * @param retryIntervalMs 重试间隔（毫秒）
     * @param <T>            返回值类型
     * @return 包含成功标志、数据、实际尝试次数和异常的重试结果
     */
    public <T> RetryResult<T> executeWithRetry(Callable<T> task, int maxRetries, int retryIntervalMs) {
        int attempts = 0;
        Exception lastException = null;

        while (attempts <= maxRetries) {
            try {
                T result = task.call();
                return new RetryResult<>(true, result, attempts, null);
            } catch (Exception e) {
                lastException = e;
                attempts++;
                if (attempts <= maxRetries) {
                    log.info("第{}次重试，等待{}ms", attempts, retryIntervalMs);
                    try {
                        Thread.sleep(retryIntervalMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return new RetryResult<>(false, null, attempts, ie);
                    }
                }
            }
        }
        return new RetryResult<>(false, null, maxRetries, lastException);
    }

    /**
     * 重试执行结果。
     */
    public record RetryResult<T>(boolean success, T data, int attempts, Exception error) {}
}