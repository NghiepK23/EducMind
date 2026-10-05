package com.educmind.analytics.dto;

import java.util.List;
import java.util.UUID;

/**
 * GET /api/v1/schools/{schoolId}/analytics/overview (SCHOOL_ADMIN, spec muc 5.3 + PATCH 3).
 * Chi so lieu tong hop theo course. Tuyet doi khong co thong tin tung hoc sinh.
 * avgMastery = null khi course chua co dong mastery nao.
 */
public record SchoolAnalyticsOverviewResponse(UUID schoolId, List<Item> items) {

    public SchoolAnalyticsOverviewResponse {
        items = List.copyOf(items);
    }

    public record Item(UUID courseId, long studentCount, Double avgMastery, long openGapCount) {
    }
}
