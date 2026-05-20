package com.inspect.agent.report;

import com.baomidou.mybatisplus.core.conditions.query.*;
import com.inspect.agent.common.*;
import com.inspect.agent.dto.*;
import com.inspect.agent.entity.*;
import com.inspect.agent.llm.*;
import com.inspect.agent.mapper.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.stereotype.*;

import java.time.format.*;
import java.util.*;

/**
 * 巡检报告服务，负责生成巡检报告和 AI 摘要。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ApiInspectTaskMapper taskMapper;
    private final ApiInspectLogMapper logMapper;
    private final LLMClient llmClient;

    /**
     * 获取任务的完整巡检报告，包含任务概览、成功/异常接口列表和 AI 分析。
     *
     * @param taskId 任务 ID
     * @return 报告 DTO
     * @throws BusinessException 任务不存在时抛出（code=404）
     */
    public ReportDTO getReport(Long taskId) {
        ApiInspectTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }

        List<ApiInspectLog> logs = logMapper.selectList(
                new LambdaQueryWrapper<ApiInspectLog>().eq(ApiInspectLog::getTaskId, taskId));

        List<ReportDTO.SuccessItem> successes = logs.stream()
                .filter(l -> l.getSuccess() == 1)
                .map(l -> {
                    ReportDTO.SuccessItem item = new ReportDTO.SuccessItem();
                    item.setCaseId(l.getCaseId());
                    item.setCaseName(l.getCaseName());
                    item.setApiUrl(l.getApiUrl());
                    item.setResponseTime(l.getResponseTime());
                    item.setStatusCode(l.getStatusCode());
                    return item;
                }).toList();

        List<ReportDTO.AnomalyItem> anomalies = logs.stream()
                .filter(l -> l.getSuccess() == 0)
                .map(l -> {
                    ReportDTO.AnomalyItem item = new ReportDTO.AnomalyItem();
                    item.setCaseId(l.getCaseId());
                    item.setCaseName(l.getCaseName());
                    item.setApiUrl(l.getApiUrl());
                    item.setErrorType(l.getErrorType());
                    item.setErrorDetail(l.getErrorMessage());
                    item.setAiAnalysis(l.getAiAnalysis());
                    item.setAiSuggestion(l.getAiSuggestion());
                    return item;
                }).toList();

        ReportDTO.TaskInfo info = new ReportDTO.TaskInfo();
        info.setId(task.getId());
        info.setTaskNo(task.getTaskNo());
        info.setTotalCount(task.getTotalCount());
        info.setSuccessCount(task.getSuccessCount());
        info.setFailCount(task.getFailCount());
        info.setTaskDuration(task.getTaskDuration());
        info.setTaskStatus(task.getTaskStatus());
        info.setExecuteMode(task.getExecuteMode());
        info.setCreateTime(task.getCreateTime() != null
                ? task.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");

        ReportDTO dto = new ReportDTO();
        dto.setTaskInfo(info);
        dto.setSuccessList(successes);
        dto.setAnomalyList(anomalies);
        dto.setAiSummary(task.getAiSummary());
        dto.setAiRiskItems(task.getAiRiskItems());

        return dto;
    }

    /**
     * 调用 LLM 生成巡检总结摘要并回写到任务表。
     *
     * @param taskId 任务 ID
     * @return AI 生成的摘要文本，LLM 异常时返回 null
     */
    public String generateAiSummary(Long taskId) {
        ApiInspectTask task = taskMapper.selectById(taskId);
        if (task == null) return null;

        List<ApiInspectLog> failLogs = logMapper.selectList(
                new LambdaQueryWrapper<ApiInspectLog>()
                        .eq(ApiInspectLog::getTaskId, taskId)
                        .eq(ApiInspectLog::getSuccess, 0));

        StringBuilder summary = new StringBuilder();
        summary.append(String.format("总用例数: %d, 成功: %d, 失败: %d, 耗时: %dms\n\n",
                task.getTotalCount(), task.getSuccessCount(), task.getFailCount(), task.getTaskDuration()));

        if (!failLogs.isEmpty()) {
            summary.append("## 失败接口详情\n\n");
            for (ApiInspectLog logEntry : failLogs) {
                summary.append(String.format("- **%s** (%s %s): status=%d, type=%s, error=%s\n",
                        logEntry.getCaseName(), logEntry.getMethod(), logEntry.getApiUrl(),
                        logEntry.getStatusCode(), logEntry.getErrorType(), logEntry.getErrorMessage()));
            }
        }

        String aiSummary = llmClient.chat(
                LLMPromptTemplates.REPORT_SUMMARY_SYSTEM,
                LLMPromptTemplates.buildReportSummaryPrompt(summary.toString())
        );

        if (aiSummary != null) {
            task.setAiSummary(aiSummary);
            taskMapper.updateById(task);
        }

        return aiSummary;
    }
}