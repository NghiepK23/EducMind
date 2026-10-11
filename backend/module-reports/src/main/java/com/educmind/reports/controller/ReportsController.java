package com.educmind.reports.controller;

import com.educmind.common.envelope.ApiResponse;
import com.educmind.reports.dto.ClassProgressReport;
import com.educmind.reports.dto.StudentProgressReport;
import com.educmind.reports.service.ClassProgressReportService;
import com.educmind.reports.service.StudentProgressReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class ReportsController {

    private final StudentProgressReportService studentProgressService;
    private final ClassProgressReportService classProgressService;

    public ReportsController(StudentProgressReportService studentProgressService,
                             ClassProgressReportService classProgressService) {
        this.studentProgressService = studentProgressService;
        this.classProgressService = classProgressService;
    }

    @GetMapping("/api/v1/reports/student-progress")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<StudentProgressReport>> studentProgress(
            Authentication authentication,
            @RequestParam(name = "student_id", required = false) UUID studentId,
            @RequestParam(name = "course_id", required = false) UUID courseId) {
        UUID actorId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(
                studentProgressService.generate(actorId, studentId, courseId)));
    }

    @GetMapping("/api/v1/courses/{courseId}/reports/class-progress")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ClassProgressReport>> classProgress(
            Authentication authentication,
            @PathVariable("courseId") UUID courseId,
            @RequestParam(name = "class_id", required = false) UUID classId) {
        UUID actorId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(
                classProgressService.generate(actorId, courseId, classId)));
    }
}
