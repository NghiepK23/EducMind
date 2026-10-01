package com.educmind.analytics.controller;

import com.educmind.analytics.dto.MasteryResponse;
import com.educmind.analytics.service.StudentMasteryQueryService;
import com.educmind.common.envelope.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class StudentMasteryController {

    private final StudentMasteryQueryService queryService;

    public StudentMasteryController(StudentMasteryQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/api/v1/students/me/mastery")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<MasteryResponse>>> getMyMastery(
            Authentication authentication,
            @RequestParam(name = "lesson_id", required = false) UUID lessonId,
            @RequestParam(name = "course_id", required = false) UUID courseId) {
        UUID studentId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(queryService.findMine(studentId, lessonId, courseId)));
    }
}
