package com.educmind.analytics.controller;

import com.educmind.analytics.dto.MasteryResponse;
import com.educmind.analytics.service.TeacherStudentMasteryQueryService;
import com.educmind.common.envelope.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * API phia giao vien cua Module 5 (spec muc 5.2).
 * Phan biet owner / dong giang day xu ly o service (isOwnerOrCoTeacher), @PreAuthorize chi chan role.
 */
@RestController
public class TeacherAnalyticsController {

    private final TeacherStudentMasteryQueryService studentMasteryQueryService;

    public TeacherAnalyticsController(TeacherStudentMasteryQueryService studentMasteryQueryService) {
        this.studentMasteryQueryService = studentMasteryQueryService;
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
}
