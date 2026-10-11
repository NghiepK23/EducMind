package com.educmind.reports.controller;

import com.educmind.common.envelope.ApiResponse;
import com.educmind.reports.dto.AssessmentResultReport;
import com.educmind.reports.service.AssessmentResultReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class AssessmentReportController {

    private final AssessmentResultReportService service;

    public AssessmentReportController(AssessmentResultReportService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/reports/assessment-result")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentResultReport>> assessmentResult(
            Authentication authentication,
            @RequestParam(name = "assessment_id", required = false) UUID assessmentId) {
        UUID actorId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(service.generate(actorId, assessmentId)));
    }
}
