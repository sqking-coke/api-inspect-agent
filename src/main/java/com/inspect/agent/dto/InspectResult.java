package com.inspect.agent.dto;

import lombok.Data;
import java.util.List;

@Data
public class InspectResult {

    private Long caseId;
    private String caseName;
    private String apiUrl;
    private String method;
    private boolean success;
    private int statusCode;
    private String responseBody;
    private int responseTime;
    private String errorMessage;
    private String errorType;
    private int retryCount;
    private List<AssertItem> assertResults;
    private String aiAnalysis;
    private String aiSuggestion;

    @Data
    public static class AssertItem {
        private String rule;
        private String expected;
        private String actual;
        private boolean passed;
    }
}