package com.inspect.agent.dto;

import lombok.Data;
import java.util.List;

/**
 * 巡检报告 DTO，包含任务概览、AI 分析、异常和成功列表。
 */
@Data
public class ReportDTO {

    /** 任务基本信息 */
    private TaskInfo taskInfo;

    /** AI 生成的巡检摘要 */
    private String aiSummary;

    /** AI 识别的风险项 */
    private String aiRiskItems;

    /** 异常接口列表 */
    private List<AnomalyItem> anomalyList;

    /** 正常接口列表 */
    private List<SuccessItem> successList;

    /**
     * 任务概览信息。
     */
    @Data
    public static class TaskInfo {
        private Long id;
        /** 任务编号 */
        private String taskNo;
        /** 总用例数 */
        private int totalCount;
        /** 成功数 */
        private int successCount;
        /** 失败数 */
        private int failCount;
        /** 耗时（毫秒） */
        private long taskDuration;
        /** 任务状态（0=执行中，1=已完成，2=已停止） */
        private int taskStatus;
        /** 执行模式 */
        private String executeMode;
        /** 创建时间（格式化字符串） */
        private String createTime;
    }

    /**
     * 异常接口项。
     */
    @Data
    public static class AnomalyItem {
        private Long caseId;
        private String caseName;
        private String apiUrl;
        /** 错误分类 */
        private String errorType;
        /** 错误详情 */
        private String errorDetail;
        /** AI 分析 */
        private String aiAnalysis;
        /** AI 修复建议 */
        private String aiSuggestion;
    }

    /**
     * 正常接口项。
     */
    @Data
    public static class SuccessItem {
        private Long caseId;
        private String caseName;
        private String apiUrl;
        /** 响应耗时（毫秒） */
        private int responseTime;
        /** HTTP 状态码 */
        private int statusCode;
    }
}