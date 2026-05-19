package com.inspect.agent.analysis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inspect.agent.entity.ApiInspectLog;
import com.inspect.agent.llm.LLMClient;
import com.inspect.agent.llm.LLMPromptTemplates;
import com.inspect.agent.mapper.ApiInspectLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 异常分析服务，提供批量 AI 分析和 AI 对话复盘功能。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExceptionAnalysisService {

    private final LLMClient llmClient;
    private final ApiInspectLogMapper logMapper;

    /**
     * 对指定任务中尚未分析的失败日志进行批量 AI 分析，结果回写日志表。
     *
     * @param taskId 任务 ID
     */
    public void batchAnalyze(Long taskId) {
        List<ApiInspectLog> failLogs = logMapper.selectList(
                new LambdaQueryWrapper<ApiInspectLog>()
                        .eq(ApiInspectLog::getTaskId, taskId)
                        .eq(ApiInspectLog::getSuccess, 0)
                        .isNull(ApiInspectLog::getAiAnalysis)
        );

        if (failLogs.isEmpty()) return;

        for (ApiInspectLog logEntry : failLogs) {
            try {
                String prompt = LLMPromptTemplates.buildAnomalyAnalysisPrompt(
                        logEntry.getCaseName(), logEntry.getApiUrl(), logEntry.getMethod(),
                        logEntry.getRequestBody(), logEntry.getStatusCode(),
                        logEntry.getResponseBody(), logEntry.getErrorMessage(),
                        logEntry.getAssertDetail()
                );
                String analysis = llmClient.chat(LLMPromptTemplates.ANOMALY_ANALYSIS_SYSTEM, prompt);
                if (analysis != null) {
                    logEntry.setAiAnalysis(analysis);
                    logMapper.updateById(logEntry);
                }
            } catch (Exception e) {
                log.warn("批量 AI 分析失败: logId={}", logEntry.getId(), e);
            }
        }
    }

    /**
     * AI 对话复盘，根据任务日志回答用户问题。
     *
     * @param taskId   任务 ID，为空时查询最近 7 天数据
     * @param question 用户问题
     * @return LLM 回复文本
     */
    public String chat(Long taskId, String question) {
        List<ApiInspectLog> logs;
        if (taskId != null) {
            logs = logMapper.selectList(
                    new LambdaQueryWrapper<ApiInspectLog>()
                            .eq(ApiInspectLog::getTaskId, taskId)
                            .last("LIMIT 50")
            );
        } else {
            logs = logMapper.selectList(
                    new LambdaQueryWrapper<ApiInspectLog>()
                            .ge(ApiInspectLog::getCreateTime,
                                    java.time.LocalDateTime.now().minusDays(7))
                            .last("LIMIT 50")
            );
        }

        StringBuilder context = new StringBuilder();
        context.append("## 巡检日志数据\n\n");
        for (ApiInspectLog logEntry : logs) {
            context.append(String.format("- [%s] %s %s → %d (%dms) %s\n",
                    logEntry.getSuccess() == 1 ? "OK" : "FAIL",
                    logEntry.getMethod(), logEntry.getApiUrl(),
                    logEntry.getStatusCode(), logEntry.getResponseTime(),
                    logEntry.getErrorType() != null ? logEntry.getErrorType() : ""));
        }

        String fullPrompt = "Data:\n" + context + "\n\nQuestion: " + question;
        return llmClient.chat(LLMPromptTemplates.CHAT_SYSTEM, fullPrompt);
    }
}