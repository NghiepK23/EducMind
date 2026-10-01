package com.educmind.analytics.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình mở/đóng knowledge gap — Module05_LearningEvidence_Analytics_FINAL.md mục 4.4 / 10.
 * Các giá trị mặc định dưới đây đều là giá trị tạm [TBD], cần hiệu chỉnh bằng dữ liệu thực tế.
 * Đọc từ application.properties, tiền tố educmind.analytics.gap.
 */
@Component
@ConfigurationProperties(prefix = "educmind.analytics.gap")
public class GapProperties {

    /** Mastery dưới ngưỡng này (và đủ evidence) thì mở gap. */
    private double openThreshold = 0.50;

    /** Mastery từ ngưỡng này trở lên thì gap OPEN tự đóng (RESOLVED). */
    private double resolveThreshold = 0.65;

    /** Số evidence tối thiểu để được mở gap (spec đã chốt là 2). */
    private int minEvidence = 2;

    public double getOpenThreshold() {
        return openThreshold;
    }

    public void setOpenThreshold(double openThreshold) {
        this.openThreshold = openThreshold;
    }

    public double getResolveThreshold() {
        return resolveThreshold;
    }

    public void setResolveThreshold(double resolveThreshold) {
        this.resolveThreshold = resolveThreshold;
    }

    public int getMinEvidence() {
        return minEvidence;
    }

    public void setMinEvidence(int minEvidence) {
        this.minEvidence = minEvidence;
    }
}
