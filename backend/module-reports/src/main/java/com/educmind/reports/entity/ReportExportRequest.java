package com.educmind.reports.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/** Bảng report_export_requests — V9__module_reports_export.sql, Module 10 mục K.4.1. */
@Entity
@Table(name = "report_export_requests")
public class ReportExportRequest {

    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "report_type", nullable = false, length = 30)
    private String reportType;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scope_params", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> scopeParams;

    @Column(nullable = false, length = 10)
    private String format;

    @Column(nullable = false, length = 20)
    private String status = STATUS_PROCESSING;

    @Column(name = "file_storage_key", length = 500)
    private String fileStorageKey;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    protected ReportExportRequest() {
    }

    public ReportExportRequest(String reportType, UUID requestedBy,
                               Map<String, Object> scopeParams, String format) {
        this.reportType = reportType;
        this.requestedBy = requestedBy;
        this.scopeParams = scopeParams;
        this.format = format;
    }

    public void markCompleted(String storageKey) {
        this.status = STATUS_COMPLETED;
        this.fileStorageKey = storageKey;
        this.completedAt = OffsetDateTime.now();
    }

    public void markFailed(String message) {
        this.status = STATUS_FAILED;
        this.errorMessage = message;
        this.completedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public String getReportType() { return reportType; }
    public UUID getRequestedBy() { return requestedBy; }
    public Map<String, Object> getScopeParams() { return scopeParams; }
    public String getFormat() { return format; }
    public String getStatus() { return status; }
    public String getFileStorageKey() { return fileStorageKey; }
    public String getErrorMessage() { return errorMessage; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
}
