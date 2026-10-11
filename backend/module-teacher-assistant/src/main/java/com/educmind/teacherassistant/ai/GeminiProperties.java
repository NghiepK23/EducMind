package com.educmind.teacherassistant.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình gọi Gemini, tiền tố educmind.ai.gemini.
 * apiKey không ghi trực tiếp trong file: application.properties chỉ trỏ tới biến môi trường GEMINI_API_KEY.
 */
@Component
@ConfigurationProperties(prefix = "educmind.ai.gemini")
public class GeminiProperties {

    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";

    private String model = "gemini-3.8-flash";

    private String apiKey = "";

    private int timeoutSeconds = 60;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
