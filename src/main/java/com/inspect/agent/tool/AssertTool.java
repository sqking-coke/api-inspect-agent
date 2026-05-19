package com.inspect.agent.tool;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.inspect.agent.dto.InspectResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 断言工具，根据 JSON 格式的断言规则对 HTTP 响应进行校验。
 *
 * <p>支持的断言类型：
 * <ul>
 *   <li>statusCode — HTTP 状态码</li>
 *   <li>maxResponseTime — 最大响应时间</li>
 *   <li>bodyContains / bodyNotContains — 响应体关键字</li>
 *   <li>responseCode + responseCodePath — JSONPath 定位的业务状态码</li>
 *   <li>notNullPath / notEmptyPath — JSONPath 非空校验</li>
 * </ul>
 */
@Slf4j
@Component
public class AssertTool {

    /**
     * 对单个 HTTP 响应执行断言校验。
     *
     * @param assertRuleJson 断言规则 JSON
     * @param statusCode     HTTP 状态码
     * @param responseBody   响应体
     * @param responseTime   实际响应耗时（毫秒）
     * @return 断言结果列表，规则为空时返回空列表
     */
    public List<InspectResult.AssertItem> assertResponse(String assertRuleJson,
                                                          int statusCode, String responseBody, int responseTime) {
        List<InspectResult.AssertItem> results = new ArrayList<>();
        if (assertRuleJson == null || assertRuleJson.isBlank()) {
            return results;
        }

        try {
            JSONObject rule = JSON.parseObject(assertRuleJson);

            if (rule.containsKey("statusCode")) {
                int expected = rule.getIntValue("statusCode");
                results.add(assertItem("statusCode", String.valueOf(expected),
                        String.valueOf(statusCode), statusCode == expected));
            }

            if (rule.containsKey("maxResponseTime")) {
                int maxMs = rule.getIntValue("maxResponseTime");
                results.add(assertItem("maxResponseTime", "<=" + maxMs + "ms",
                        responseTime + "ms", responseTime <= maxMs));
            }

            if (rule.containsKey("bodyContains") && responseBody != null) {
                String keyword = rule.getString("bodyContains");
                results.add(assertItem("bodyContains", keyword,
                        "found=" + responseBody.contains(keyword), responseBody.contains(keyword)));
            }

            if (rule.containsKey("bodyNotContains") && responseBody != null) {
                String keyword = rule.getString("bodyNotContains");
                results.add(assertItem("bodyNotContains", "not contains: " + keyword,
                        "found=" + responseBody.contains(keyword), !responseBody.contains(keyword)));
            }

            // JSONPath 断言仅在响应体为合法 JSON 时生效
            if (responseBody != null && !responseBody.isBlank()) {
                try {
                    JSONObject respJson = JSON.parseObject(responseBody);

                    if (rule.containsKey("responseCode") && rule.containsKey("responseCodePath")) {
                        int expectedCode = rule.getIntValue("responseCode");
                        String path = rule.getString("responseCodePath");
                        Object actual = getByPath(respJson, path);
                        boolean pass = actual != null
                                && Integer.parseInt(actual.toString()) == expectedCode;
                        results.add(assertItem("responseCode(" + path + ")", String.valueOf(expectedCode),
                                String.valueOf(actual), pass));
                    }

                    if (rule.containsKey("notNullPath")) {
                        String path = rule.getString("notNullPath");
                        Object val = getByPath(respJson, path);
                        results.add(assertItem("notNull(" + path + ")", "not null",
                                val == null ? "null" : "exists", val != null));
                    }

                    if (rule.containsKey("notEmptyPath")) {
                        String path = rule.getString("notEmptyPath");
                        Object val = getByPath(respJson, path);
                        boolean pass = val != null && !val.toString().isEmpty();
                        results.add(assertItem("notEmpty(" + path + ")", "not empty",
                                pass ? "not empty" : "empty/null", pass));
                    }
                } catch (Exception ignored) {
                    // 非 JSON 响应体，跳过 JSONPath 断言
                }
            }
        } catch (Exception e) {
            log.warn("断言规则解析失败: {}", assertRuleJson, e);
        }
        return results;
    }

    /** 构建单条断言结果对象 */
    private InspectResult.AssertItem assertItem(String rule, String expected, String actual, boolean passed) {
        InspectResult.AssertItem item = new InspectResult.AssertItem();
        item.setRule(rule);
        item.setExpected(expected);
        item.setActual(actual);
        item.setPassed(passed);
        return item;
    }

    /** 按点号分隔的简单 JSONPath 取值，支持 $.a.b 格式 */
    private Object getByPath(JSONObject json, String path) {
        String[] parts = path.replace("$.", "").split("\\.");
        Object current = json;
        for (String part : parts) {
            if (current instanceof JSONObject jo) {
                current = jo.get(part);
            } else {
                return null;
            }
        }
        return current;
    }
}