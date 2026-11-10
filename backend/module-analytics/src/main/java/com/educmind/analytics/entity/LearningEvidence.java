package com.educmind.analytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Bảng learning_evidence (Spec v2, mục F — bảng 18).
 *
 * Mỗi dòng = một mục tiêu học tập mà học sinh đạt được sau một bài nộp/bài kiểm tra.
 * DB có UNIQUE (student_id, objective_id, source_type, source_id) và trigger kiểm tra
 * nguồn có thật → tầng ứng dụng chỉ cần ghi/ghi đè điểm.
 */
@Entity
@Table(name = "learning_evidence")
public class LearningEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "objective_id", nullable = false)
    private UUID objectiveId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "source_type", nullable = false)
    private EvidenceSource sourceType;

    /** ASSIGNMENT → id bài nộp; ASSESSMENT → id bài kiểm tra. */
    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal score;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected LearningEvidence() {
    }

    public LearningEvidence(UUID studentId, UUID objectiveId, EvidenceSource sourceType,
                            UUID sourceId, BigDecimal score) {
        this.studentId = studentId;
        this.objectiveId = objectiveId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.score = score;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    /** Chấm lại: cập nhật điểm của chính bằng chứng cũ (AC-T04.4). */
    public void updateScore(BigDecimal newScore) {
        this.score = newScore;
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public UUID getObjectiveId() {
        return objectiveId;
    }

    public EvidenceSource getSourceType() {
        return sourceType;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public BigDecimal getScore() {
        return score;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
