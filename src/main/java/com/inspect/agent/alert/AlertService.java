package com.inspect.agent.alert;

import com.alibaba.fastjson2.*;
import com.inspect.agent.dto.*;
import lombok.extern.slf4j.*;
import okhttp3.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

import java.io.*;
import java.util.*;

/**
 * 告警通知服务，通过钉钉 Webhook 推送巡检异常告警。
 */
@Slf4j
@Service
public class AlertService {

    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();

    /** 是否启用告警推送 */
    @Value("${alert.enabled:false}")
    private boolean alertEnabled;

    /** 钉钉机器人 Webhook 地址 */
    @Value("${alert.webhook:}")
    private String webhookUrl;

    /**
     * 推送异常告警到钉钉群。
     *
     * @param taskId   任务 ID
     * @param taskNo   任务编号
     * @param failures 失败用例列表
     */
    public void sendAnomalyAlert(Long taskId, String taskNo, List<InspectResult> failures) {
        if (!alertEnabled || webhookUrl == null || webhookUrl.isBlank() || failures.isEmpty()) {
            return;
        }

        StringBuilder content = new StringBuilder();
        content.append("## 接口巡检异常告警\n\n");
        content.append(String.format("**任务编号**: %s\n", taskNo));
        content.append(String.format("**异常接口数**: %d\n\n", failures.size()));

        for (InspectResult r : failures) {
            content.append(String.format("- **%s** (%s %s)\n", r.getCaseName(), r.getMethod(), r.getApiUrl()));
            content.append(String.format("  - 错误类型: %s\n", r.getErrorType()));
            content.append(String.format("  - 错误信息: %s\n", r.getErrorMessage()));
            if (r.getAiAnalysis() != null) {
                content.append(String.format("  - AI分析: %s\n",
                        r.getAiAnalysis().length() > 100
                                ? r.getAiAnalysis().substring(0, 100) + "..."
                                : r.getAiAnalysis()));
            }
            content.append("\n");
        }

        try {
            JSONObject body = new JSONObject();
            body.put("msgtype", "markdown");
            JSONObject markdown = new JSONObject();
            markdown.put("title", "接口巡检异常告警 - " + taskNo);
            markdown.put("text", content.toString());
            body.put("markdown", markdown);

            Request request = new Request.Builder()
                    .url(webhookUrl)
                    .post(RequestBody.create(body.toJSONString(), JSON_MEDIA))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                log.info("告警推送结果: status={}", response.code());
            }
        } catch (IOException e) {
            log.error("告警推送失败", e);
        }
    }
}