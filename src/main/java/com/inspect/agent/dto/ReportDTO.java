package com.inspect.agent.dto;

import lombok.Data;
import java.util.List;

@Data
public class ReportDTO {

    private TaskInfo taskInfo;
    private String aiSummary;
    private String aiRiskItems;
    private List<AnomalyItem> anomalyList;
    private List<SuccessItem> successList;

    @Data
    public static class TaskInfo {
        private Long id;
        private String taskNo;
        private int totalCount;
        private int successCount;
        private int failCount;
        private long taskDuration;
        private int taskStatus;
        private String executeMode;
        private String createTime;
    }

    @Data
    public static class AnomalyItem {
        private Long caseId;
        private String caseName;
        private String apiUrl;
        private String errorType;
        private String errorDetail;
        private String aiAnalysis;
        private String aiSuggestion;
    }

    @Data
    public static class SuccessItem {
        private Long caseId;
        private String caseName;
        private String apiUrl;
        private int responseTime;
        private int statusCode;
    }
}