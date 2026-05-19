package com.inspect.agent.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String question;
    private Long taskId;
}