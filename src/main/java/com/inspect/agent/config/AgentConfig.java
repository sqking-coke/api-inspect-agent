package com.inspect.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 巡检配置，映射 application.yml 中 agent.inspect 前缀的属性。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "agent.inspect")
public class AgentConfig {

    /** HTTP 调用默认超时（毫秒），默认 5000 */
    private int defaultTimeout = 5000;

    /** 失败默认重试次数，默认 2 */
    private int defaultRetryCount = 2;

    /** 重试间隔（毫秒），默认 1000 */
    private int retryInterval = 1000;

    /** 并行执行线程数，默认 10 */
    private int parallelThreads = 10;

    /** 定时巡检 Cron 表达式，默认每天凌晨 3:00 */
    private String cronExpression = "0 0 3 * * ?";

    /** 是否启用定时巡检，默认 false */
    private boolean cronEnabled = false;

    /** 日志保留天数，默认 90 */
    private int logRetentionDays = 90;
}