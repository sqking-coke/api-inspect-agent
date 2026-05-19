package com.inspect.agent.llm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.inspect.agent.config.LLMConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * LLM 客户端，通过 OpenAI 兼容 API 调用大模型。
 */
@Slf4j
@Component
public class LLMClient {

    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");
    private final LLMConfig config;
    private final OkHttpClient client;

    public LLMClient(LLMConfig config) {
        this.config = config;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(config.getConnectTimeout(), TimeUnit.SECONDS)
                .readTimeout(config.getReadTimeout(), TimeUnit.SECONDS)
                .build();
    }

    /**
     * 发送 Chat Completion 请求。
     *
     * @param systemPrompt 系统提示词
     * @param userMessage  用户消息
     * @return 模型回复文本，LLM 禁用或异常时返回 null
     */
    public String chat(String systemPrompt, String userMessage) {
        if (!config.isEnabled()) {
            log.info("LLM 已禁用，返回默认结果");
            return null;
        }
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.warn("LLM API Key 未配置");
            return null;
        }

        JSONObject body = new JSONObject();
        body.put("model", config.getModel());
        body.put("max_tokens", config.getMaxTokens());
        body.put("temperature", config.getTemperature());

        JSONArray messages = new JSONArray();
        JSONObject sysMsg = new JSONObject();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt);
        messages.add(sysMsg);

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        body.put("messages", messages);

        try {
            Request request = new Request.Builder()
                    .url(config.getApiUrl())
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(body.toJSONString(), JSON_MEDIA))
                    .build();

            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("LLM 调用失败: status={}", response.code());
                    return null;
                }
                String respBody = response.body() != null ? response.body().string() : "";
                JSONObject respJson = JSON.parseObject(respBody);
                return respJson.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
            }
        } catch (IOException e) {
            log.error("LLM 调用异常", e);
            return null;
        }
    }

    /**
     * 使用默认系统提示词发送消息。
     *
     * @param userMessage 用户消息
     * @return 模型回复文本
     */
    public String chat(String userMessage) {
        return chat("You are a helpful assistant.", userMessage);
    }
}