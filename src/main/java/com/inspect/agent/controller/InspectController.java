package com.inspect.agent.controller;

import com.baomidou.mybatisplus.core.conditions.query.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.*;
import com.inspect.agent.agent.*;
import com.inspect.agent.analysis.*;
import com.inspect.agent.common.*;
import com.inspect.agent.dto.*;
import com.inspect.agent.entity.*;
import com.inspect.agent.mapper.*;
import com.inspect.agent.report.*;
import com.inspect.agent.service.*;
import lombok.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 巡检核心接口，提供任务控制、报告查看、日志查询和 AI 对话功能。
 */
@RestController
@RequestMapping("/agent/inspect")
@RequiredArgsConstructor
public class InspectController {

    private final AgentCore agentCore;
    private final InspectTaskService taskService;
    private final CaseService caseService;
    private final ReportService reportService;
    private final ExceptionAnalysisService analysisService;
    private final ApiInspectLogMapper logMapper;

    /** 启动巡检任务，异步生成 AI 摘要和批量分析 */
    @PostMapping("/start")
    public Result<Map<String, Object>> start(@RequestBody TaskStartRequest request) {
        AgentContext ctx = agentCore.startInspect(request.getExecuteMode(), request.getCaseIds(), 1);
        new Thread(() -> {
            reportService.generateAiSummary(ctx.getTaskId());
            analysisService.batchAnalyze(ctx.getTaskId());
        }).start();
        return Result.ok("巡检任务已启动", Map.of(
                "taskId", ctx.getTaskId(),
                "taskNo", ctx.getTaskNo(),
                "totalCount", ctx.getTotalCount()
        ));
    }

    /** 停止当前正在执行的巡检任务 */
    @PostMapping("/stop")
    public Result<Map<String, Object>> stop(@RequestBody Map<String, Long> body) {
        agentCore.stopCurrentTask();
        AgentContext ctx = agentCore.getCurrentContext();
        return Result.ok("巡检任务已停止", Map.of(
                "taskId", body.get("taskId"),
                "completedCount", ctx != null ? ctx.getCompletedCount() : 0,
                "totalCount", ctx != null ? ctx.getTotalCount() : 0
        ));
    }

    /** 分页查询巡检任务列表 */
    @GetMapping("/task/list")
    public Result<Page<ApiInspectTask>> taskList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.ok(taskService.pageQuery(page, size, status, startDate, endDate));
    }

    /** 获取任务巡检报告（含 AI 摘要） */
    @GetMapping("/report/{taskId}")
    public Result<ReportDTO> report(@PathVariable Long taskId) {
        return Result.ok(reportService.getReport(taskId));
    }

    /** 查询任务下的巡检日志，可按用例 ID 和成功/失败筛选 */
    @GetMapping("/log/{taskId}")
    public Result<List<ApiInspectLog>> log(
            @PathVariable Long taskId,
            @RequestParam(required = false) Long caseId,
            @RequestParam(required = false) Integer success) {
        var wrapper = new LambdaQueryWrapper<ApiInspectLog>()
                .eq(ApiInspectLog::getTaskId, taskId);
        if (caseId != null) wrapper.eq(ApiInspectLog::getCaseId, caseId);
        if (success != null) wrapper.eq(ApiInspectLog::getSuccess, success);
        wrapper.orderByAsc(ApiInspectLog::getCreateTime);
        return Result.ok(logMapper.selectList(wrapper));
    }

    /** AI 对话复盘，基于历史巡检日志回答用户问题 */
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        String answer = analysisService.chat(request.getTaskId(), request.getQuestion());
        return Result.ok(Map.of("question", request.getQuestion(), "answer", answer));
    }

    /** 单用例调试：立即执行指定用例并返回结果（不记录日志） */
    @PostMapping("/case/test")
    public Result<InspectResult> testCase(@RequestBody Map<String, Long> body) {
        Long caseId = body.get("caseId");
        ApiInspectCase c = caseService.getById(caseId);
        if (c == null) return Result.fail(404, "用例不存在");
        InspectResult result = agentCore.executeOneCase(null, c);
        return Result.ok(result);
    }
}