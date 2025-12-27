package com.im.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DeepSeek API 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekConfig {

    /**
     * API Key
     */
    private String apiKey;

    /**
     * API 地址
     */
    private String apiUrl = "https://api.deepseek.com/chat/completions";

    /**
     * 模型名称
     */
    private String model = "deepseek-chat";

    /**
     * 超时时间(秒)
     */
    private int timeout = 30;

    /**
     * 最大返回长度
     */
    private int maxTokens = 512;
}
