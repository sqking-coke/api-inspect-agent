package com.inspect.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 巡检用例实体，定义一条 API 巡检规则。
 */
@Data
@TableName("api_inspect_case")
public class ApiInspectCase {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用例名称 */
    private String caseName;

    /** 接口 URL */
    private String apiUrl;

    /** HTTP 方法（GET/POST/PUT/DELETE） */
    private String method;

    /** 请求头（JSON 格式） */
    private String requestHeader;

    /** 请求体（JSON 格式） */
    private String requestBody;

    /** URL 查询参数 */
    private String queryParams;

    /** 超时时间（毫秒），为空则使用全局默认值 */
    private Integer timeout;

    /** 重试次数，为空则使用全局默认值 */
    private Integer retryCount;

    /** 重试间隔（毫秒），为空则使用全局默认值 */
    private Integer retryInterval;

    /** 断言规则（JSON 格式） */
    private String assertRule;

    /** 用例分组 */
    private String groupName;

    /** 用例说明 */
    private String description;

    /** 优先级 */
    private Integer priority;

    /** 状态（0=禁用，1=启用） */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}