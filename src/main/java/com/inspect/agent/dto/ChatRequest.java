package com.inspect.agent.dto;

import lombok.Data;

/**
 * AI 对话复盘请求。
 */
@Data
public class ChatRequest {

    /** 用户提问内容 */
    private String question;

    /** 关联的任务 ID，为空时查询最近 7 天数据 */
    private Long taskId;
}