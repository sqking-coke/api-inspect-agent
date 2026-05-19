package com.inspect.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("api_inspect_case")
public class ApiInspectCase {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String caseName;
    private String apiUrl;
    private String method;
    private String requestHeader;
    private String requestBody;
    private String queryParams;
    private Integer timeout;
    private Integer retryCount;
    private Integer retryInterval;
    private String assertRule;
    private String groupName;
    private String description;
    private Integer priority;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}