package com.inspect.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("api_inspect_task")
public class ApiInspectTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskNo;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private Integer skipCount;
    private Long taskDuration;
    private String executeMode;
    private String aiSummary;
    private String aiRiskItems;
    private BigDecimal errorRate;
    private Integer avgResponseTime;
    private Integer taskStatus;
    private Integer triggerType;
    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}