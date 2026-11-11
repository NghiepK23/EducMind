package com.educmind.analytics.controller;

import com.educmind.analytics.dto.ClassMasteryResponse;
import com.educmind.analytics.dto.DismissRequest;
import com.educmind.analytics.dto.KnowledgeGapRow;
import com.educmind.analytics.dto.StudentMasteryDetailResponse;
import com.educmind.analytics.service.KnowledgeGapService;
import com.educmind.analytics.service.AnalyticsQueryService;
import com.educmind.common.envelope.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** US-T07, US-T08 — giáo viên xem phân tích theo lớp. */
@RestController
public class ClassAnalyticsController {

    private final AnalyticsQueryService queryService;
    private final KnowledgeGapService knowledgeGapService;

    public ClassAnalyticsController(AnalyticsQueryService queryService,
                                    KnowledgeGapService knowledgeGapService) {
        this.queryService = queryService;
        this.knowledgeGapService = knowledgeGapService;
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/api/v1/classes/{classId}/mastery")
    public ResponseEntity<ApiResponse<ClassMasteryResponse>> classMastery(
            Authentication authentication, @PathVariable UUID classId,
            @RequestParam(required = false) UUID topicId,
            @RequestParam(required = false) UUID lessonId) {
        return ResponseEntity.ok(ApiResponse.success(queryService.classMastery(
                Actor.id(authentication), classId, topicId, lessonId)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/api/v1/classes/{classId}/students/{studentId}/mastery")
    public ResponseEntity<ApiResponse<StudentMasteryDetailResponse>> studentMastery(
            Authentication authentication, @PathVariable UUID classId, @PathVariable UUID studentId,
            @RequestParam(required = false) UUID topicId,
            @RequestParam(required = false) UUID lessonId) {
        return ResponseEntity.ok(ApiResponse.success(queryService.studentMasteryForTeacher(
                Actor.id(authentication), classId, studentId, topicId, lessonId)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/api/v1/classes/{classId}/knowledge-gaps")
    public ResponseEntity<ApiResponse<List<KnowledgeGapRow>>> knowledgeGaps(
            Authentication authentication, @PathVariable UUID classId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID topicId,
            @RequestParam(required = false) UUID lessonId) {
        return ResponseEntity.ok(ApiResponse.success(knowledgeGapService.list(
                Actor.id(authentication), classId, status, topicId, lessonId)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/api/v1/knowledge-gaps/{id}/dismiss")
    public ResponseEntity<ApiResponse<KnowledgeGapRow>> dismiss(
            Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody DismissRequest request) {
        return ResponseEntity.ok(ApiResponse.success(knowledgeGapService.dismiss(
                Actor.id(authentication), id, request.getReason())));
    }
}
