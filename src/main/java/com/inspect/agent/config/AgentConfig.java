package com.inspect.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "agent.inspect")
public class AgentConfig {
    private int defaultTimeout = 5000;
    private int defaultRetryCount = 2;
    private int retryInterval = 1000;
    private int parallelThreads = 10;
    private String cronExpression = "0 0 3 * * ?";
    private boolean cronEnabled = false;
    private int logRetentionDays = 90;
}