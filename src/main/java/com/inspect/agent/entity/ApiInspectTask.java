package com.inspect.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 巡检任务实体，记录一次巡检批次的汇总信息。
 */
@Data
@TableName("api_inspect_task")
public class ApiInspectTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务编号，格式 INSPECT-yyyyMMdd-NNN */
    private String taskNo;

    /** 总用例数 */
    private Integer totalCount;

    /** 成功数 */
    private Integer successCount;

    /** 失败数 */
    private Integer failCount;

    /** 跳过数 */
    private Integer skipCount;

    /** 任务总耗时（毫秒） */
    private Long taskDuration;

    /** 执行模式（SERIAL/PARALLEL） */
    private String executeMode;

    /** AI 生成的巡检摘要 */
    private String aiSummary;

    /** AI 识别的风险项 */
    private String aiRiskItems;

    /** 错误率 */
    private BigDecimal errorRate;

    /** 平均响应时间（毫秒） */
    private Integer avgResponseTime;

    /** 任务状态（0=执行中，1=已完成，2=已停止） */
    private Integer taskStatus;

    /** 触发类型（1=手动，2=定时） */
    private Integer triggerType;

    /** 异常信息（任务级别） */
    private String errorMessage;

    /** 任务开始时间 */
    private LocalDateTime startTime;

    /** 任务结束时间 */
    private LocalDateTime endTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}