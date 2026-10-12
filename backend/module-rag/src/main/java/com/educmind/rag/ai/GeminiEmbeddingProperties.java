package com.educmind.rag.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình gọi Gemini embedding, tiền tố educmind.ai.embedding.
 * apiKey không ghi trực tiếp trong file: application.properties chỉ trỏ tới biến môi trường GEMINI_API_KEY.
 */
@Component
@ConfigurationProperties(prefix = "educmind.ai.embedding")
public class GeminiEmbeddingProperties {

    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";

    private String model = "gemini-embedding-001";

    private String apiKey = "";

    /** Số chiều vector. Phải khớp VECTOR(n) của bảng document_chunks (migration V11). */
    private int dimensions = 768;

    private int timeoutSeconds = 60;

    /** Số đoạn chữ gửi trong một lời gọi. Giữ nhỏ để tránh vượt hạn mức bậc miễn phí. */
    private int batchSize = 16;

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

    public int getDimensions() {
        return dimensions;
    }

    public void setDimensions(int dimensions) {
        this.dimensions = dimensions;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
