package com.educmind.analytics.controller;

import com.educmind.analytics.dto.MasteryMatrixResponse;
import com.educmind.analytics.dto.MasteryResponse;
import com.educmind.analytics.dto.KnowledgeGapResponse;
import com.educmind.analytics.service.TeacherKnowledgeGapQueryService;
import com.educmind.analytics.service.TeacherMasteryMatrixQueryService;
import com.educmind.analytics.service.TeacherStudentMasteryQueryService;
import com.educmind.common.envelope.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API phia giao vien cua Module 5 (spec muc 5.2).
 * Phan biet owner / dong giang day xu ly o service (isOwnerOrCoTeacher), @PreAuthorize chi chan role.
 */
@RestController
public class TeacherAnalyticsController {

    private final TeacherStudentMasteryQueryService studentMasteryQueryService;
    private final TeacherKnowledgeGapQueryService knowledgeGapQueryService;
    private final TeacherMasteryMatrixQueryService masteryMatrixQueryService;

    public TeacherAnalyticsController(TeacherStudentMasteryQueryService studentMasteryQueryService,
                                      TeacherKnowledgeGapQueryService knowledgeGapQueryService,
                                      TeacherMasteryMatrixQueryService masteryMatrixQueryService) {
        this.studentMasteryQueryService = studentMasteryQueryService;
        this.knowledgeGapQueryService = knowledgeGapQueryService;
        this.masteryMatrixQueryService = masteryMatrixQueryService;
    }

    @GetMapping("/api/v1/students/{studentId}/mastery")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<MasteryResponse>>> getStudentMastery(
            Authentication authentication,
            @PathVariable UUID studentId,
            @RequestParam(name = "course_id", required = false) UUID courseId) {
        UUID actorId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(
                studentMasteryQueryService.find(actorId, studentId, courseId)));
    }

    @GetMapping("/api/v1/courses/{courseId}/analytics/knowledge-gaps")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<KnowledgeGapResponse>>> getCourseKnowledgeGaps(
            Authentication authentication,
            @PathVariable UUID courseId,
            @RequestParam(name = "class_id", required = false) UUID classId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        UUID actorId = UUID.fromString(authentication.getName());
        TeacherKnowledgeGapQueryService.Result r =
                knowledgeGapQueryService.find(actorId, courseId, classId, status, page, size);
        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("page", r.page());
        pagination.put("size", r.size());
        pagination.put("total_items", r.totalItems());
        pagination.put("total_pages", r.totalPages());
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("pagination", pagination);
        return ResponseEntity.ok(ApiResponse.success(r.items(), meta));
    }

    @GetMapping("/api/v1/lessons/{lessonId}/analytics/mastery-matrix")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<MasteryMatrixResponse>> getMasteryMatrix(
            Authentication authentication,
            @PathVariable UUID lessonId,
            @RequestParam(name = "class_id", required = false) UUID classId) {
        UUID actorId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(
                masteryMatrixQueryService.find(actorId, lessonId, classId)));
    }
}
