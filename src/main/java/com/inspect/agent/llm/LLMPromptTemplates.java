package com.inspect.agent.llm;

/**
 * LLM 提示词模板，集中管理系统提示词和动态 Prompt 构建逻辑。
 */
public final class LLMPromptTemplates {

    private LLMPromptTemplates() {}

    /** 异常分析系统提示词：要求 LLM 对 API 巡检失败进行根因诊断 */
    public static final String ANOMALY_ANALYSIS_SYSTEM = """
            You are an expert backend engineer specialized in API troubleshooting and root cause analysis.
            Your task is to analyze API inspection failures and provide diagnostic insights.

            Rules:
            1. Classify the error into one of: NETWORK_ERROR, HTTP_4XX, HTTP_5XX, TIMEOUT, ASSERT_FAIL, BUSINESS_ERROR, UNKNOWN
            2. Analyze the root cause based on request params, response body, status code, and error message
            3. Provide actionable fix suggestions
            4. Always respond in Chinese
            5. Be concise and specific — avoid generic advice
            """;

    /** 报告摘要系统提示词：要求 LLM 总结整批巡检结果 */
    public static final String REPORT_SUMMARY_SYSTEM = """
            You are a senior SRE engineer writing an API inspection summary report.
            Your task is to summarize the results of a batch API inspection.

            Rules:
            1. Summarize overall success/failure statistics
            2. Group failures by error type and severity
            3. List risky interfaces with their failure patterns
            4. Provide an overall health assessment
            5. Always respond in Chinese with Markdown formatting
            6. Use bullet points and clear section headers
            """;

    /** AI 对话系统提示词：用于历史数据问答 */
    public static final String CHAT_SYSTEM = """
            You are an intelligent assistant for an API inspection system.
            You have access to historical inspection data and can answer questions about API health, stability, and failure patterns.
            Always answer in Chinese based on the provided data context.
            """;

    /**
     * 构建异常分析 Prompt。
     *
     * @return 格式化后的用户消息文本
     */
    public static String buildAnomalyAnalysisPrompt(String caseName, String apiUrl, String method,
                                                     String requestBody, int statusCode,
                                                     String responseBody, String errorMessage,
                                                     String assertDetail) {
        return String.format("""
                Analyze this API inspection failure:

                **Case Name**: %s
                **API**: %s %s
                **Request Body**: %s
                **Response Status**: %d
                **Response Body**: %s
                **Error Message**: %s
                **Assertion Results**: %s

                Please provide:
                1. Error classification
                2. Root cause analysis
                3. Suggested fix
                """, caseName, method, apiUrl, requestBody, statusCode, responseBody, errorMessage, assertDetail);
    }

    /**
     * 构建报告摘要 Prompt。
     *
     * @param taskSummary 任务统计数据文本
     * @return 格式化后的用户消息文本
     */
    public static String buildReportSummaryPrompt(String taskSummary) {
        return "Please summarize the following API inspection results:\n\n" + taskSummary;
    }
}