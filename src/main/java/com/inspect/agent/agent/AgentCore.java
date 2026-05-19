package com.inspect.agent.agent;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inspect.agent.common.BusinessException;
import com.inspect.agent.config.AgentConfig;
import com.inspect.agent.dto.InspectResult;
import com.inspect.agent.entity.ApiInspectCase;
import com.inspect.agent.entity.ApiInspectLog;
import com.inspect.agent.entity.ApiInspectTask;
import com.inspect.agent.llm.LLMClient;
import com.inspect.agent.llm.LLMPromptTemplates;
import com.inspect.agent.mapper.ApiInspectCaseMapper;
import com.inspect.agent.mapper.ApiInspectLogMapper;
import com.inspect.agent.mapper.ApiInspectTaskMapper;
import com.inspect.agent.tool.AssertTool;
import com.inspect.agent.tool.HttpInvokeTool;
import com.inspect.agent.tool.RetryTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 巡检 Agent 核心引擎，负责用例加载、任务调度、断言校验和 AI 分析。
 *
 * <p>支持两种执行模式：
 * <ul>
 *   <li><b>SERIAL</b> — 串行逐个执行用例</li>
 *   <li><b>PARALLEL</b> — 线程池并行执行用例（默认）</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentCore {

    private final ApiInspectCaseMapper caseMapper;
    private final ApiInspectTaskMapper taskMapper;
    private final ApiInspectLogMapper logMapper;
    private final HttpInvokeTool httpInvokeTool;
    private final AssertTool assertTool;
    private final RetryTool retryTool;
    private final LLMClient llmClient;
    private final AgentConfig agentConfig;
    private final ThreadPoolExecutor inspectExecutor;

    /** 当前正在执行的巡检上下文，同一时间只允许一个巡检任务 */
    private volatile AgentContext currentContext;

    /** 任务编号自增序号（每天从 1 开始） */
    private final AtomicInteger taskSeq = new AtomicInteger(1);

    /**
     * 启动巡检任务，完整闭环：加载用例 → 创建上下文 → 执行（串行/并行） → 保存日志 → 更新任务状态。
     *
     * @param executeMode 执行模式（SERIAL / PARALLEL），为 null 时默认 PARALLEL
     * @param caseIds     指定用例 ID 列表，为空时加载所有启用用例
     * @param triggerType 触发类型（1=手动，2=定时）
     * @return 巡检上下文，含任务编号、执行统计等信息
     * @throws BusinessException 当已有运行中的任务时抛出（code=409）
     */
    public AgentContext startInspect(String executeMode, List<Long> caseIds, int triggerType) {
        if (currentContext != null && !currentContext.isFinished() && !currentContext.isStopped()) {
            throw new BusinessException(409, "已有巡检任务正在运行中: " + currentContext.getTaskNo());
        }

        // 1. 加载用例
        List<ApiInspectCase> cases = loadCases(caseIds);
        if (cases.isEmpty()) {
            throw new BusinessException(400, "没有可执行的用例");
        }

        // 2. 创建任务上下文
        String taskNo = generateTaskNo();
        AgentContext ctx = new AgentContext();
        ctx.setTaskNo(taskNo);
        ctx.setExecuteMode(executeMode != null ? executeMode.toUpperCase() : "PARALLEL");
        ctx.setTotalCount(cases.size());
        ctx.setStartTime(LocalDateTime.now());

        // 3. 创建任务记录
        ApiInspectTask task = new ApiInspectTask();
        task.setTaskNo(taskNo);
        task.setTotalCount(cases.size());
        task.setExecuteMode(ctx.getExecuteMode());
        task.setTaskStatus(0);
        task.setTriggerType(triggerType);
        task.setStartTime(ctx.getStartTime());
        taskMapper.insert(task);
        ctx.setTaskId(task.getId());

        currentContext = ctx;
        log.info("巡检任务启动: taskNo={}, total={}, mode={}", taskNo, cases.size(), ctx.getExecuteMode());

        // 4. 执行巡检
        if ("SERIAL".equalsIgnoreCase(ctx.getExecuteMode())) {
            executeSerial(ctx, cases);
        } else {
            executeParallel(ctx, cases);
        }

        // 5. 完成任务记录
        ctx.setEndTime(LocalDateTime.now());
        task.setSuccessCount(ctx.getSuccessCount());
        task.setFailCount(ctx.getFailCount());
        task.setTaskDuration(ctx.getDurationMs());
        task.setTaskStatus(ctx.isStopped() ? 2 : 1);
        task.setEndTime(ctx.getEndTime());
        taskMapper.updateById(task);

        log.info("巡检任务完成: taskNo={}, success={}, fail={}, duration={}ms",
                taskNo, ctx.getSuccessCount(), ctx.getFailCount(), ctx.getDurationMs());

        return ctx;
    }

    /**
     * 加载待执行的用例列表，仅返回启用状态（status=1）的用例。
     *
     * @param caseIds 指定用例 ID 集合，为空时加载全部启用用例
     */
    private List<ApiInspectCase> loadCases(List<Long> caseIds) {
        if (caseIds != null && !caseIds.isEmpty()) {
            return caseMapper.selectBatchIds(caseIds).stream()
                    .filter(c -> c.getStatus() == 1)
                    .toList();
        }
        return caseMapper.selectList(
                new LambdaQueryWrapper<ApiInspectCase>().eq(ApiInspectCase::getStatus, 1));
    }

    /** 串行执行用例，按列表顺序逐个处理，遇停止标记提前退出 */
    private void executeSerial(AgentContext ctx, List<ApiInspectCase> cases) {
        for (ApiInspectCase c : cases) {
            if (ctx.isStopped()) break;
            InspectResult result = executeOneCase(ctx.getTaskId(), c);
            ctx.addResult(result);
            saveLog(ctx.getTaskId(), c, result);
        }
    }

    /** 并行执行用例，使用线程池并发处理，整体超时 5 分钟 */
    private void executeParallel(AgentContext ctx, List<ApiInspectCase> cases) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (ApiInspectCase c : cases) {
            futures.add(CompletableFuture.runAsync(() -> {
                if (ctx.isStopped()) return;
                InspectResult result = executeOneCase(ctx.getTaskId(), c);
                ctx.addResult(result);
                saveLog(ctx.getTaskId(), c, result);
            }, inspectExecutor));
        }
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(5, TimeUnit.MINUTES);
        } catch (java.util.concurrent.TimeoutException e) {
            log.warn("并行巡检超时，部分任务未完成");
        } catch (Exception e) {
            log.error("并行巡检异常", e);
        }
    }

    /**
     * 执行单个用例，完整流程：HTTP 调用 → 重试 → 断言校验 → 错误分类 → AI 分析（仅失败时）。
     *
     * @param taskId 关联的任务 ID，手动测试时可为 null
     * @param c      待执行的用例实体
     * @return 巡检结果，含状态码、响应体、断言详情、AI 分析等
     */
    public InspectResult executeOneCase(Long taskId, ApiInspectCase c) {
        int timeout = c.getTimeout() != null && c.getTimeout() > 0
                ? c.getTimeout() : agentConfig.getDefaultTimeout();
        int maxRetries = c.getRetryCount() != null
                ? c.getRetryCount() : agentConfig.getDefaultRetryCount();
        int retryInterval = c.getRetryInterval() != null && c.getRetryInterval() > 0
                ? c.getRetryInterval() : agentConfig.getRetryInterval();

        RetryTool.RetryResult<HttpInvokeTool.HttpResponse> retryResult = retryTool.executeWithRetry(() -> {
            HttpInvokeTool.HttpResponse resp = httpInvokeTool.execute(
                    c.getApiUrl(), c.getMethod(), c.getRequestHeader(), c.getRequestBody(), timeout);
            if (resp.success() && (resp.statusCode() == 502 || resp.statusCode() == 503)) {
                throw new RuntimeException("Retryable status: " + resp.statusCode());
            }
            return resp;
        }, maxRetries, retryInterval);

        HttpInvokeTool.HttpResponse finalResp = retryResult.data();
        InspectResult result = new InspectResult();
        result.setCaseId(c.getId());
        result.setCaseName(c.getCaseName());
        result.setApiUrl(c.getApiUrl());
        result.setMethod(c.getMethod());
        result.setRetryCount(retryResult.attempts());

        if (!retryResult.success() || finalResp == null) {
            result.setSuccess(false);
            result.setStatusCode(0);
            result.setResponseTime(0);
            result.setErrorMessage(retryResult.error() != null
                    ? retryResult.error().getMessage() : "Unknown error");
            result.setErrorType("UNKNOWN");
        } else {
            result.setStatusCode(finalResp.statusCode());
            result.setResponseBody(finalResp.body());
            result.setResponseTime(finalResp.elapsedMs());
            result.setErrorMessage(finalResp.error());

            // 断言校验
            List<InspectResult.AssertItem> assertResults = assertTool.assertResponse(
                    c.getAssertRule(), finalResp.statusCode(), finalResp.body(), finalResp.elapsedMs());
            result.setAssertResults(assertResults);

            boolean assertPassed = assertResults.stream().allMatch(InspectResult.AssertItem::isPassed);
            boolean httpOk = finalResp.success() && finalResp.statusCode() >= 200 && finalResp.statusCode() < 400;
            result.setSuccess(httpOk && assertPassed);

            if (!result.isSuccess()) {
                result.setErrorType(classifyError(finalResp));
            }
        }

        // AI 智能分析（仅失败时）
        if (!result.isSuccess()) {
            analyzeWithAI(result, c);
        }

        return result;
    }

    /** 根据 HTTP 响应或异常信息归类错误类型：TIMEOUT / NETWORK_ERROR / HTTP_4XX / HTTP_5XX / ASSERT_FAIL */
    private String classifyError(HttpInvokeTool.HttpResponse resp) {
        if (resp.error() != null) {
            if (resp.error().startsWith("TIMEOUT")) return "TIMEOUT";
            if (resp.error().startsWith("NETWORK_ERROR")) return "NETWORK_ERROR";
        }
        int sc = resp.statusCode();
        if (sc >= 400 && sc < 500) return "HTTP_4XX";
        if (sc >= 500) return "HTTP_5XX";
        return "ASSERT_FAIL";
    }

    /** 调用 LLM 对失败用例进行根因分析，并将结果写入 result */
    private void analyzeWithAI(InspectResult result, ApiInspectCase c) {
        try {
            String prompt = LLMPromptTemplates.buildAnomalyAnalysisPrompt(
                    c.getCaseName(), c.getApiUrl(), c.getMethod(),
                    c.getRequestBody(), result.getStatusCode(),
                    result.getResponseBody(), result.getErrorMessage(),
                    JSON.toJSONString(result.getAssertResults())
            );
            String aiResponse = llmClient.chat(LLMPromptTemplates.ANOMALY_ANALYSIS_SYSTEM, prompt);
            if (aiResponse != null) {
                result.setAiAnalysis(aiResponse);
                result.setAiSuggestion(extractSuggestion(aiResponse));
            }
        } catch (Exception e) {
            log.warn("AI 分析失败: caseId={}", c.getId(), e);
        }
    }

    /** 从 AI 分析结论中截取"修复建议"部分 */
    private String extractSuggestion(String aiResponse) {
        int idx = aiResponse.indexOf("修复建议");
        if (idx == -1) idx = aiResponse.indexOf("Suggested fix");
        if (idx == -1) idx = aiResponse.indexOf("建议");
        if (idx != -1) {
            return aiResponse.substring(idx).trim();
        }
        return aiResponse;
    }

    /** 将单条用例的执行结果持久化到巡检日志表 */
    private void saveLog(Long taskId, ApiInspectCase c, InspectResult result) {
        ApiInspectLog logEntry = new ApiInspectLog();
        logEntry.setTaskId(taskId);
        logEntry.setCaseId(c.getId());
        logEntry.setCaseName(c.getCaseName());
        logEntry.setApiUrl(c.getApiUrl());
        logEntry.setMethod(c.getMethod());
        logEntry.setRequestHeader(c.getRequestHeader());
        logEntry.setRequestBody(c.getRequestBody());
        logEntry.setStatusCode(result.getStatusCode());
        logEntry.setResponseBody(truncate(result.getResponseBody(), 8192));
        logEntry.setResponseTime(result.getResponseTime());
        logEntry.setSuccess(result.isSuccess() ? 1 : 0);
        logEntry.setErrorMessage(result.getErrorMessage());
        logEntry.setErrorType(result.getErrorType());
        logEntry.setAssertDetail(JSON.toJSONString(result.getAssertResults()));
        logEntry.setRetryCount(result.getRetryCount());
        logEntry.setAiAnalysis(result.getAiAnalysis());
        logEntry.setAiSuggestion(result.getAiSuggestion());
        logMapper.insert(logEntry);
    }

    /** 截断字符串到指定长度，超出部分附加 truncation 标记 */
    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...[truncated]";
    }

    /** 停止当前正在执行的巡检任务，设置停止标记后各执行线程会在下次检查时退出 */
    public void stopCurrentTask() {
        if (currentContext != null) {
            currentContext.setStopped(true);
            log.info("巡检任务已停止: {}", currentContext.getTaskNo());
        }
    }

    /** @return 当前正在执行的巡检上下文，可能为 null */
    public AgentContext getCurrentContext() {
        return currentContext;
    }

    private String generateTaskNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("INSPECT-%s-%03d", date, taskSeq.getAndIncrement());
    }
}