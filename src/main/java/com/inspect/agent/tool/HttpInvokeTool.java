package com.inspect.agent.tool;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP 调用工具，封装 OkHttp 执行 API 巡检请求。
 */
@Slf4j
@Component
public class HttpInvokeTool {

    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client;

    public HttpInvokeTool() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 执行一次 HTTP 调用。
     *
     * @param url        请求 URL
     * @param method     HTTP 方法（GET/POST/PUT/DELETE）
     * @param headersJson 请求头（JSON 格式），可为空
     * @param bodyJson   请求体（JSON 格式），非 POST/PUT 时忽略
     * @param timeoutMs  读超时（毫秒）
     * @return 包含成功标志、状态码、响应体、耗时和错误信息的结果对象
     */
    public HttpResponse execute(String url, String method, String headersJson,
                                 String bodyJson, int timeoutMs) {
        long start = System.currentTimeMillis();
        try {
            OkHttpClient timeoutClient = client.newBuilder()
                    .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .build();

            Request.Builder builder = new Request.Builder().url(url);

            if (headersJson != null && !headersJson.isBlank()) {
                Map<String, String> headers = JSON.parseObject(headersJson,
                        new com.alibaba.fastjson2.TypeReference<Map<String, String>>() {});
                if (headers != null) {
                    headers.forEach(builder::addHeader);
                }
            }

            RequestBody body = null;
            if (bodyJson != null && !bodyJson.isBlank()
                    && ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method))) {
                body = RequestBody.create(bodyJson, JSON_MEDIA);
            }

            String m = method != null ? method.toUpperCase() : "GET";
            switch (m) {
                case "POST" -> builder.post(body != null ? body : RequestBody.create("", JSON_MEDIA));
                case "PUT" -> builder.put(body != null ? body : RequestBody.create("", JSON_MEDIA));
                case "DELETE" -> builder.delete(body != null ? body : RequestBody.create("", JSON_MEDIA));
                default -> builder.get();
            }

            try (okhttp3.Response response = timeoutClient.newCall(builder.build()).execute()) {
                String respBody = response.body() != null ? response.body().string() : "";
                int elapsed = (int) (System.currentTimeMillis() - start);
                return new HttpResponse(true, response.code(), respBody, elapsed, null);
            }
        } catch (java.net.SocketTimeoutException e) {
            int elapsed = (int) (System.currentTimeMillis() - start);
            return new HttpResponse(false, 0, null, elapsed, "TIMEOUT: " + e.getMessage());
        } catch (IOException e) {
            int elapsed = (int) (System.currentTimeMillis() - start);
            return new HttpResponse(false, 0, null, elapsed, "NETWORK_ERROR: " + e.getMessage());
        } catch (Exception e) {
            int elapsed = (int) (System.currentTimeMillis() - start);
            log.error("HTTP调用异常 url={}", url, e);
            return new HttpResponse(false, 0, null, elapsed, "UNKNOWN: " + e.getMessage());
        }
    }

    /**
     * HTTP 响应结果。
     */
    public record HttpResponse(boolean success, int statusCode, String body, int elapsedMs, String error) {}
}