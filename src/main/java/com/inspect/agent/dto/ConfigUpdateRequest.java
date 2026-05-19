package com.inspect.agent.dto;

import lombok.Data;

/**
 * 系统配置更新请求，所有字段可选，仅更新非 null 的字段。
 */
@Data
public class ConfigUpdateRequest {

    /** 定时巡检 Cron 表达式 */
    private String cronExpression;

    /** 是否启用定时巡检 */
    private Boolean cronEnabled;

    /** HTTP 调用默认超时（毫秒） */
    private Integer defaultTimeout;

    /** 默认重试次数 */
    private Integer defaultRetryCount;

    /** 重试间隔（毫秒） */
    private Integer retryInterval;

    /** 并行执行线程数 */
    private Integer parallelThreads;

    /** 是否启用 LLM */
    private Boolean llmEnabled;

    /** 是否启用告警 */
    private Boolean alertEnabled;

    /** 告警 Webhook 地址 */
    private String alertWebhook;
}