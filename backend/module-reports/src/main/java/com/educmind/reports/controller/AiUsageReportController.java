package com.educmind.reports.controller;

import com.educmind.common.envelope.ApiResponse;
import com.educmind.common.port.AiUsagePort.AiUsageSummary;
import com.educmind.reports.service.AiUsageReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
public class AiUsageReportController {

    private final AiUsageReportService service;

    public AiUsageReportController(AiUsageReportService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/schools/{schoolId}/reports/ai-usage")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AiUsageSummary>> aiUsage(
            Authentication authentication,
            @PathVariable("schoolId") UUID schoolId,
            @RequestParam(name = "period_start", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime periodStart,
            @RequestParam(name = "period_end", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime periodEnd) {
        UUID actorId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(
                service.generate(actorId, schoolId, periodStart, periodEnd)));
    }
}
