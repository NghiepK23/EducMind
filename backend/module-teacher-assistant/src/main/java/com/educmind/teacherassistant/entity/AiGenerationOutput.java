package com.educmind.teacherassistant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bảng ai_generation_outputs — nội dung AI sinh ra + trạng thái duyệt của giáo viên.
 * Cột retrieved_chunk_ids (UUID[]) lưu id các đoạn tài liệu (Module 6) đã đưa vào prompt; NULL với output cũ.
 */
@Entity
@Table(name = "ai_generation_outputs")
public class AiGenerationOutput {

    public static final String REVIEW_PENDING = "PENDING_REVIEW";
    public static final String REVIEW_APPROVED = "APPROVED";
    public static final String REVIEW_REJECTED = "REJECTED";
    public static final String REVIEW_EDITED_AND_APPROVED = "EDITED_AND_APPROVED";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> content;

    @Column(name = "review_status", nullable = false, length = 20)
    private String reviewStatus = REVIEW_PENDING;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "review_notes")
    private String reviewNotes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "edited_content", columnDefinition = "jsonb")
    private Map<String, Object> editedContent;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "retrieved_chunk_ids", columnDefinition = "uuid[]")
    private List<UUID> retrievedChunkIds;

    @Column(name = "applied_at")
    private OffsetDateTime appliedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected AiGenerationOutput() {
    }

    public AiGenerationOutput(UUID requestId, Map<String, Object> content) {
        this.requestId = requestId;
        this.content = content;
    }

    public AiGenerationOutput(UUID requestId, Map<String, Object> content, List<UUID> retrievedChunkIds) {
        this(requestId, content);
        this.retrievedChunkIds = retrievedChunkIds;
    }

    public void approve(UUID reviewerId, String notes) {
        this.reviewStatus = REVIEW_APPROVED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = OffsetDateTime.now();
        this.reviewNotes = notes;
    }

    public void editAndApprove(UUID reviewerId, String notes, Map<String, Object> edited) {
        this.reviewStatus = REVIEW_EDITED_AND_APPROVED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = OffsetDateTime.now();
        this.reviewNotes = notes;
        this.editedContent = edited;
    }

    public void reject(UUID reviewerId, String notes) {
        this.reviewStatus = REVIEW_REJECTED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = OffsetDateTime.now();
        this.reviewNotes = notes;
    }

    public void markApplied() {
        this.appliedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getRequestId() { return requestId; }
    public Map<String, Object> getContent() { return content; }
    public List<UUID> getRetrievedChunkIds() { return retrievedChunkIds; }
    public String getReviewStatus() { return reviewStatus; }
    public UUID getReviewedBy() { return reviewedBy; }
    public OffsetDateTime getReviewedAt() { return reviewedAt; }
    public String getReviewNotes() { return reviewNotes; }
    public Map<String, Object> getEditedContent() { return editedContent; }
    public OffsetDateTime getAppliedAt() { return appliedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
