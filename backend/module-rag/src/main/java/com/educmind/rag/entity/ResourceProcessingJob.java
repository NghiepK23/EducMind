package com.educmind.rag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Nhật ký xử lý một giai đoạn của một tài liệu (phục vụ theo dõi tiến trình và truy vết lỗi).
 * resourceId là UUID thuần, không dùng @ManyToOne, theo quy ước giữa các module.
 */
@Entity
@Table(name = "resource_processing_jobs")
public class ResourceProcessingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "resource_id", nullable = false, updatable = false)
    private UUID resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 30)
    private JobStage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected ResourceProcessingJob() {
    }

    public ResourceProcessingJob(UUID resourceId, JobStage stage) {
        this.resourceId = resourceId;
        this.stage = stage;
        this.status = JobStatus.PENDING;
        this.attemptCount = 0;
        this.createdAt = OffsetDateTime.now();
    }

    /** Bắt đầu một lần chạy (tăng số lần thử). */
    public void markRunning() {
        this.status = JobStatus.RUNNING;
        this.attemptCount++;
        this.startedAt = OffsetDateTime.now();
        this.finishedAt = null;
        this.errorMessage = null;
    }

    public void markSucceeded() {
        this.status = JobStatus.SUCCEEDED;
        this.finishedAt = OffsetDateTime.now();
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.finishedAt = OffsetDateTime.now();
        this.errorMessage = errorMessage;
    }

    public UUID getId() {
        return id;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public JobStage getStage() {
        return stage;
    }

    public JobStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
