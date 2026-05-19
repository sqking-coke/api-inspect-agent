package com.inspect.agent.dto;

import lombok.Data;
import java.util.List;

@Data
public class TaskStartRequest {
    private String executeMode;
    private List<Long> caseIds;
}