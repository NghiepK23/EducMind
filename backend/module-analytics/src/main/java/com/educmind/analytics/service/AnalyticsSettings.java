package com.educmind.analytics.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Ngưỡng đánh giá của Phase 3 (AC-S05.1, AC-T08.2). Cấu hình được qua application.properties:
 *
 * <pre>
 * educmind.analytics.mastered-threshold=0.8   // ≥ ngưỡng này ⇒ "Đã đạt"
 * educmind.analytics.improving-threshold=0.5  // ≥ ngưỡng này (và dưới ngưỡng đạt) ⇒ "Đang tiến bộ"
 * educmind.analytics.gap-threshold=0.5        // dưới ngưỡng này ⇒ lỗ hổng kiến thức
 * educmind.analytics.gap-min-evidence=2       // số lần đánh giá tối thiểu mới kết luận lỗ hổng
 * </pre>
 */
@Component
public class AnalyticsSettings {

    private final double masteredThreshold;
    private final double improvingThreshold;
    private final double gapThreshold;
    private final int gapMinEvidence;

    public AnalyticsSettings(
            @Value("${educmind.analytics.mastered-threshold:0.8}") double masteredThreshold,
            @Value("${educmind.analytics.improving-threshold:0.5}") double improvingThreshold,
            @Value("${educmind.analytics.gap-threshold:0.5}") double gapThreshold,
            @Value("${educmind.analytics.gap-min-evidence:2}") int gapMinEvidence) {
        this.masteredThreshold = masteredThreshold;
        this.improvingThreshold = improvingThreshold;
        this.gapThreshold = gapThreshold;
        this.gapMinEvidence = gapMinEvidence;
    }

    public double masteredThreshold() {
        return masteredThreshold;
    }

    public double improvingThreshold() {
        return improvingThreshold;
    }

    public double gapThreshold() {
        return gapThreshold;
    }

    public int gapMinEvidence() {
        return gapMinEvidence;
    }
}
