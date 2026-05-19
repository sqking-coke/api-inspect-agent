package com.inspect.agent.dto;

import lombok.Data;
import java.util.List;

/**
 * 巡检任务启动请求。
 */
@Data
public class TaskStartRequest {

    /** 执行模式（SERIAL/PARALLEL），为空默认 PARALLEL */
    private String executeMode;

    /** 指定要执行的用例 ID 列表，为空执行全部启用用例 */
    private List<Long> caseIds;
}