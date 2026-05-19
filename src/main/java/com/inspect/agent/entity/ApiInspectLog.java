package com.inspect.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 巡检日志实体，记录每个用例每次执行的完整详情。
 */
@Data
@TableName("api_inspect_log")
public class ApiInspectLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的任务 ID */
    private Long taskId;

    /** 关联的用例 ID */
    private Long caseId;

    /** 用例名称（冗余，便于查询） */
    private String caseName;

    /** 接口 URL */
    private String apiUrl;

    /** HTTP 方法 */
    private String method;

    /** 请求头（JSON） */
    private String requestHeader;

    /** 请求体（JSON） */
    private String requestBody;

    /** 响应头（JSON） */
    private String responseHeader;

    /** 响应体，长文本会被截断 */
    private String responseBody;

    /** HTTP 状态码 */
    private Integer statusCode;

    /** 响应耗时（毫秒） */
    private Integer responseTime;

    /** 执行结果（0=失败，1=成功） */
    private Integer success;

    /** 错误信息 */
    private String errorMessage;

    /** 错误分类（TIMEOUT/NETWORK_ERROR/HTTP_4XX/HTTP_5XX/ASSERT_FAIL） */
    private String errorType;

    /** 断言详情（JSON） */
    private String assertDetail;

    /** 实际重试次数 */
    private Integer retryCount;

    /** AI 分析结论 */
    private String aiAnalysis;

    /** AI 修复建议 */
    private String aiSuggestion;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}