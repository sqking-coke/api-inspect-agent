package com.inspect.agent.controller;

import com.inspect.agent.common.Result;
import com.inspect.agent.dto.ConfigUpdateRequest;
import com.inspect.agent.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 系统配置管理接口。
 */
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    /** 获取当前系统配置（数据库配置优先，无记录时使用默认值） */
    @GetMapping("/config")
    public Result<Map<String, String>> getConfig() {
        return Result.ok(Map.of(
                "cronExpression", nvl(configService.getValue("cron_expression"), "0 0 3 * * ?"),
                "cronEnabled", nvl(configService.getValue("cron_enabled"), "false"),
                "defaultTimeout", nvl(configService.getValue("default_timeout"), "5000"),
                "defaultRetryCount", nvl(configService.getValue("default_retry_count"), "2"),
                "retryInterval", nvl(configService.getValue("retry_interval"), "1000"),
                "parallelThreads", nvl(configService.getValue("parallel_threads"), "10"),
                "llmEnabled", nvl(configService.getValue("llm_enabled"), "true"),
                "alertEnabled", nvl(configService.getValue("alert_enabled"), "false"),
                "alertWebhook", nvl(configService.getValue("alert_webhook"), "")
        ));
    }

    /** 更新系统配置（仅更新非 null 字段） */
    @PutMapping("/config")
    public Result<Void> updateConfig(@RequestBody ConfigUpdateRequest request) {
        if (request.getCronExpression() != null)
            configService.setValue("cron_expression", request.getCronExpression());
        if (request.getCronEnabled() != null)
            configService.setValue("cron_enabled", String.valueOf(request.getCronEnabled()));
        if (request.getDefaultTimeout() != null)
            configService.setValue("default_timeout", String.valueOf(request.getDefaultTimeout()));
        if (request.getDefaultRetryCount() != null)
            configService.setValue("default_retry_count", String.valueOf(request.getDefaultRetryCount()));
        if (request.getRetryInterval() != null)
            configService.setValue("retry_interval", String.valueOf(request.getRetryInterval()));
        if (request.getParallelThreads() != null)
            configService.setValue("parallel_threads", String.valueOf(request.getParallelThreads()));
        if (request.getLlmEnabled() != null)
            configService.setValue("llm_enabled", String.valueOf(request.getLlmEnabled()));
        if (request.getAlertEnabled() != null)
            configService.setValue("alert_enabled", String.valueOf(request.getAlertEnabled()));
        if (request.getAlertWebhook() != null)
            configService.setValue("alert_webhook", request.getAlertWebhook());
        return Result.ok("配置已更新", null);
    }

    /** 返回非空值，val 为 null 或空白时返回默认值 def */
    private String nvl(String val, String def) {
        return val != null && !val.isBlank() ? val : def;
    }
}