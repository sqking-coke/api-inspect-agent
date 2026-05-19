package com.inspect.agent.dto;

import lombok.Data;
import java.util.List;

/**
 * 单个用例的巡检执行结果。
 */
@Data
public class InspectResult {

    /** 用例 ID */
    private Long caseId;

    /** 用例名称 */
    private String caseName;

    /** 接口 URL */
    private String apiUrl;

    /** HTTP 方法 */
    private String method;

    /** 是否通过（HTTP 成功且断言全部通过） */
    private boolean success;

    /** HTTP 状态码 */
    private int statusCode;

    /** 响应体 */
    private String responseBody;

    /** 响应耗时（毫秒） */
    private int responseTime;

    /** 错误信息 */
    private String errorMessage;

    /** 错误分类 */
    private String errorType;

    /** 实际重试次数 */
    private int retryCount;

    /** 断言结果列表 */
    private List<AssertItem> assertResults;

    /** AI 分析结论 */
    private String aiAnalysis;

    /** AI 修复建议 */
    private String aiSuggestion;

    /**
     * 单条断言结果。
     */
    @Data
    public static class AssertItem {

        /** 断言规则名称 */
        private String rule;

        /** 期望值 */
        private String expected;

        /** 实际值 */
        private String actual;

        /** 是否通过 */
        private boolean passed;
    }
}