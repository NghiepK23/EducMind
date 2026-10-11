package com.educmind.teacherassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình Module 7 (AI Teacher Assistant).
 * Đọc từ application.properties, tiền tố educmind.ai.teacher-assistant.
 */
@Component
@ConfigurationProperties(prefix = "educmind.ai.teacher-assistant")
public class TeacherAssistantProperties {

    /** Số yêu cầu generate tối đa của 1 giáo viên trong 24 giờ gần nhất (PATCH 4: 28, gộp cả 3 loại). */
    private int dailyLimit = 28;

    public int getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }
}
