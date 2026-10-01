package com.educmind.analytics.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cau hinh ngưỡng sinh recommendation - Module05 FINAL muc 4.5. Gia tri tam [TBD].
 */
@Component
@ConfigurationProperties(prefix = "educmind.analytics.recommendation")
public class RecommendationProperties {

    /** Mastery duoi nguong nay: REVIEW_CONTENT; tu nguong nay den duoi advancedThreshold: PRACTICE_TARGETED. */
    private double masteryLow = 0.40;

    /** Mastery tu nguong nay tro len: ADVANCED_ACTIVITY. */
    private double advancedThreshold = 0.70;

    public double getMasteryLow() { return masteryLow; }
    public void setMasteryLow(double masteryLow) { this.masteryLow = masteryLow; }
    public double getAdvancedThreshold() { return advancedThreshold; }
    public void setAdvancedThreshold(double advancedThreshold) { this.advancedThreshold = advancedThreshold; }
}
