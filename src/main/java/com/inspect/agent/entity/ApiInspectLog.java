package com.inspect.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("api_inspect_log")
public class ApiInspectLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;
    private Long caseId;
    private String caseName;
    private String apiUrl;
    private String method;
    private String requestHeader;
    private String requestBody;
    private String responseHeader;
    private String responseBody;
    private Integer statusCode;
    private Integer responseTime;
    private Integer success;
    private String errorMessage;
    private String errorType;
    private String assertDetail;
    private Integer retryCount;
    private String aiAnalysis;
    private String aiSuggestion;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}