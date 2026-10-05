package com.educmind.analytics.controller;

import com.educmind.analytics.dto.SchoolAnalyticsOverviewResponse;
import com.educmind.analytics.service.SchoolAnalyticsOverviewQueryService;
import com.educmind.common.envelope.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * API phia SCHOOL_ADMIN cua Module 5 (spec muc 5.3 + PATCH 3): chi xem so lieu tong hop, read-only.
 * @PreAuthorize chi chan role; viec schoolId path phai trung school cua nguoi goi xu ly o service.
 */
@RestController
public class SchoolAnalyticsController {

    private final SchoolAnalyticsOverviewQueryService overviewQueryService;

    public SchoolAnalyticsController(SchoolAnalyticsOverviewQueryService overviewQueryService) {
        this.overviewQueryService = overviewQueryService;
    }

    @GetMapping("/api/v1/schools/{schoolId}/analytics/overview")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolAnalyticsOverviewResponse>> getSchoolOverview(
            Authentication authentication,
            @PathVariable UUID schoolId) {
        UUID actorId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(
                overviewQueryService.find(actorId, schoolId)));
    }
}
