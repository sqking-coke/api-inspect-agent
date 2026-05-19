package com.inspect.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * LLM 大模型配置，映射 application.yml 中 llm 前缀的属性。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "llm")
public class LLMConfig {

    /** 是否启用 LLM，默认 true */
    private boolean enabled = true;

    /** LLM API 地址（OpenAI 兼容格式） */
    private String apiUrl;

    /** LLM API Key */
    private String apiKey;

    /** 模型名称 */
    private String model;

    /** 最大 Token 数，默认 4096 */
    private int maxTokens = 4096;

    /** 温度参数（0~1），默认 0.3 */
    private double temperature = 0.3;

    /** 连接超时（秒），默认 30 */
    private int connectTimeout = 30;

    /** 读取超时（秒），默认 120 */
    private int readTimeout = 120;
}