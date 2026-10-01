package com.educmind.analytics.controller;

import com.educmind.analytics.dto.KnowledgeGapResponse;
import com.educmind.analytics.dto.RecommendationResponse;
import com.educmind.analytics.service.StudentLearningQueryService;
import com.educmind.common.envelope.ApiResponse;
import com.educmind.common.pagination.PaginationSupport;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class StudentLearningController {

    private final StudentLearningQueryService queryService;

    public StudentLearningController(StudentLearningQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/api/v1/students/me/knowledge-gaps")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<KnowledgeGapResponse>>> getMyGaps(
            Authentication authentication,
            @RequestParam(name = "course_id", required = false) UUID courseId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        UUID studentId = UUID.fromString(authentication.getName());
        Page<KnowledgeGapResponse> result = queryService.findMyGaps(studentId, courseId, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(result.getContent(),
                PaginationSupport.meta(result.getNumber() + 1, result.getSize(), result.getTotalElements())));
    }

    @GetMapping("/api/v1/students/me/recommendations")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> getMyRecommendations(
            Authentication authentication,
            @RequestParam(name = "course_id", required = false) UUID courseId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        UUID studentId = UUID.fromString(authentication.getName());
        Page<RecommendationResponse> result =
                queryService.findMyRecommendations(studentId, courseId, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(result.getContent(),
                PaginationSupport.meta(result.getNumber() + 1, result.getSize(), result.getTotalElements())));
    }
}
