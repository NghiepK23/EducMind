package com.educmind.analytics.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình tính mastery — Module05_LearningEvidence_Analytics_FINAL.md mục 4.3 / 10.
 * Các giá trị mặc định dưới đây đều là giá trị tạm [TBD], cần hiệu chỉnh bằng dữ liệu thực tế.
 * Đọc từ application.properties, tiền tố educmind.analytics.mastery.
 */
@Component
@ConfigurationProperties(prefix = "educmind.analytics.mastery")
public class MasteryProperties {

    /** Sau bấy nhiêu ngày thì trọng số của 1 evidence giảm còn một nửa. */
    private double halfLifeDays = 30;

    /** Từ số evidence này trở lên thì confidence_level = MEDIUM. */
    private int confidenceMediumMinEvidence = 3;

    /** Từ số evidence này trở lên thì confidence_level = HIGH. */
    private int confidenceHighMinEvidence = 5;

    public double getHalfLifeDays() {
        return halfLifeDays;
    }

    public void setHalfLifeDays(double halfLifeDays) {
        this.halfLifeDays = halfLifeDays;
    }

    public int getConfidenceMediumMinEvidence() {
        return confidenceMediumMinEvidence;
    }

    public void setConfidenceMediumMinEvidence(int confidenceMediumMinEvidence) {
        this.confidenceMediumMinEvidence = confidenceMediumMinEvidence;
    }

    public int getConfidenceHighMinEvidence() {
        return confidenceHighMinEvidence;
    }

    public void setConfidenceHighMinEvidence(int confidenceHighMinEvidence) {
        this.confidenceHighMinEvidence = confidenceHighMinEvidence;
    }
}
