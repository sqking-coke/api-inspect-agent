package com.inspect.agent.dto;

import lombok.Data;

@Data
public class ConfigUpdateRequest {
    private String cronExpression;
    private Boolean cronEnabled;
    private Integer defaultTimeout;
    private Integer defaultRetryCount;
    private Integer retryInterval;
    private Integer parallelThreads;
    private Boolean llmEnabled;
    private Boolean alertEnabled;
    private String alertWebhook;
}