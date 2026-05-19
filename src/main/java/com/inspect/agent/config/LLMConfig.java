package com.inspect.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "llm")
public class LLMConfig {
    private boolean enabled = true;
    private String apiUrl = "https://api.openai.com/v1/chat/completions";
    private String apiKey = "";
    private String model = "gpt-4o";
    private int maxTokens = 4096;
    private double temperature = 0.3;
    private int connectTimeout = 30;
    private int readTimeout = 120;
}