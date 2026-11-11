package com.educmind.analytics.service;

/** Trạng thái mức nắm vững trả cho UI (AC-S05.1, AC-T07.2, AC-H01.3). */
public enum MasteryLevel {
    /** Đã đạt */
    MASTERED,
    /** Đang tiến bộ */
    IMPROVING,
    /** Cần cải thiện */
    NEEDS_IMPROVEMENT,
    /** Chưa có dữ liệu — KHÔNG kết luận là chưa đạt (AC-H01.3) */
    NO_DATA;

    public static MasteryLevel of(Double score, AnalyticsSettings settings) {
        if (score == null) {
            return NO_DATA;
        }
        if (score >= settings.masteredThreshold()) {
            return MASTERED;
        }
        if (score >= settings.improvingThreshold()) {
            return IMPROVING;
        }
        return NEEDS_IMPROVEMENT;
    }

    public boolean mastered() {
        return this == MASTERED;
    }
}
