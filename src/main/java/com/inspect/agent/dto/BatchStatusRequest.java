package com.inspect.agent.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchStatusRequest {
    private List<Long> ids;
    private Integer status;
}