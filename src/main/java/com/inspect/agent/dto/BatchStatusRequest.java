package com.inspect.agent.dto;

import lombok.Data;
import java.util.List;

/**
 * 批量更新用例状态请求。
 */
@Data
public class BatchStatusRequest {

    /** 目标用例 ID 列表 */
    private List<Long> ids;

    /** 目标状态（0=禁用，1=启用） */
    private Integer status;
}