package com.educmind.analytics.controller;

import com.educmind.analytics.dto.ObjectiveDetailRow;
import com.educmind.analytics.dto.RecommendationRow;
import com.educmind.analytics.service.AnalyticsQueryService;
import com.educmind.analytics.service.RecommendationService;
import com.educmind.common.envelope.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** US-S05, US-S06 — học sinh xem kết quả theo mục tiêu và gợi ý học tập. */
@RestController
public class StudentAnalyticsController {

    private final AnalyticsQueryService queryService;
    private final RecommendationService recommendationService;

    public StudentAnalyticsController(AnalyticsQueryService queryService,
                                      RecommendationService recommendationService) {
        this.queryService = queryService;
        this.recommendationService = recommendationService;
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/api/v1/students/me/mastery")
    public ResponseEntity<ApiResponse<List<ObjectiveDetailRow>>> myMastery(
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                queryService.myMastery(Actor.id(authentication))));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/api/v1/students/me/recommendations")
    public ResponseEntity<ApiResponse<List<RecommendationRow>>> myRecommendations(
            Authentication authentication,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(recommendationService.myRecommendations(
                Actor.id(authentication), status)));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/api/v1/recommendations/{id}/accept")
    public ResponseEntity<ApiResponse<RecommendationRow>> accept(
            Authentication authentication, @PathVariable java.util.UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                recommendationService.accept(Actor.id(authentication), id)));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/api/v1/recommendations/{id}/dismiss")
    public ResponseEntity<ApiResponse<RecommendationRow>> dismiss(
            Authentication authentication, @PathVariable java.util.UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                recommendationService.dismiss(Actor.id(authentication), id)));
    }
}
