package com.educmind.reports.controller;

import com.educmind.common.envelope.ApiResponse;
import com.educmind.reports.entity.ReportExportRequest;
import com.educmind.reports.service.ReportExportService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
public class ReportExportController {

    public record ExportStatusResponse(UUID id, String reportType, String format, String status,
                                       String errorMessage, OffsetDateTime createdAt,
                                       OffsetDateTime completedAt, String downloadUrl) {
        static ExportStatusResponse from(ReportExportRequest r) {
            String url = ReportExportRequest.STATUS_COMPLETED.equals(r.getStatus())
                    ? "/api/v1/reports/export/" + r.getId() + "/download" : null;
            return new ExportStatusResponse(r.getId(), r.getReportType(), r.getFormat(),
                    r.getStatus(), r.getErrorMessage(), r.getCreatedAt(), r.getCompletedAt(), url);
        }
    }

    private final ReportExportService service;

    public ReportExportController(ReportExportService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/reports/export")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ExportStatusResponse>> create(
            Authentication authentication, @RequestBody Map<String, Object> body) {
        UUID actorId = UUID.fromString(authentication.getName());
        ReportExportRequest req = service.create(actorId, body);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(ExportStatusResponse.from(req)));
    }

    @GetMapping("/api/v1/reports/export/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ExportStatusResponse>> status(
            Authentication authentication, @PathVariable("id") UUID id) {
        UUID actorId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(
                ExportStatusResponse.from(service.find(actorId, id))));
    }

    @GetMapping("/api/v1/reports/export/{id}/download")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<FileSystemResource> download(
            Authentication authentication, @PathVariable("id") UUID id) {
        UUID actorId = UUID.fromString(authentication.getName());
        ReportExportRequest req = service.find(actorId, id);
        FileSystemResource file = service.file(actorId, id);
        boolean pdf = "PDF".equals(req.getFormat());
        String ext = pdf ? "pdf" : "csv";
        MediaType type = pdf ? MediaType.APPLICATION_PDF
                : MediaType.parseMediaType("text/csv; charset=UTF-8");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report-" + id + "." + ext + "\"")
                .contentType(type)
                .body(file);
    }
}
