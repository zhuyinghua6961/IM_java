package com.im.ai.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.im.ai.config.DeepSeekConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * DeepSeek API 客户端
 */
@Slf4j
@Component
public class DeepSeekClient {

    @Autowired
    private DeepSeekConfig deepSeekConfig;

    private final OkHttpClient client;

    public DeepSeekClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 简单对话调用
     */
    public String chat(String userMessage) {
        return chat(userMessage, null);
    }

    /**
     * 带系统提示词的对话调用
     */
    public String chat(String userMessage, String systemPrompt) {
        // 构建请求体
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", deepSeekConfig.getModel());

        // 构建消息列表
        JSONArray messages = new JSONArray();

        // 系统提示词
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);
        }

        // 用户消息
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        requestBody.put("messages", messages);
        requestBody.put("max_tokens", deepSeekConfig.getMaxTokens());
        requestBody.put("temperature", 0.7);

        // 发送请求
        return sendRequest(requestBody.toJSONString());
    }

    /**
     * 发送请求
     */
    private String sendRequest(String jsonBody) {
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(deepSeekConfig.getApiUrl())
                .addHeader("Authorization", "Bearer " + deepSeekConfig.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("DeepSeek API 调用失败, code: {}", response.code());
                return "抱歉，我现在有点忙，请稍后再试。";
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            return parseResponse(responseBody);

        } catch (IOException e) {
            log.error("DeepSeek API 调用异常", e);
            return "抱歉，服务器出了点问题，请稍后再试。";
        }
    }

    /**
     * 解析响应
     */
    private String parseResponse(String responseBody) {
        try {
            JSONObject jsonObject = JSON.parseObject(responseBody);
            JSONArray choices = jsonObject.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");
                if (message != null) {
                    return message.getString("content");
                }
            }
            return "抱歉，我没有理解您的问题。";
        } catch (Exception e) {
            log.error("解析 DeepSeek 响应失败", e);
            return "抱歉，出了点问题，请稍后再试。";
        }
    }
}
