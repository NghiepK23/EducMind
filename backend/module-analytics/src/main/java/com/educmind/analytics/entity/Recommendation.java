package com.educmind.analytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Bảng recommendations (Spec v2, mục F — bảng 21; US-S06, US-H01).
 *
 * DB bảo đảm UNIQUE (student_id, type, objective_id, related_assessment_id) WHERE status = 'ACTIVE'
 * nên tầng ứng dụng luôn tìm bản ghi ACTIVE cùng loại trước khi tạo mới (AC-S06.5).
 * Trigger V7 tự sinh thông báo NEW_RECOMMENDATION khi INSERT (AC-G03.1).
 */
@Entity
@Table(name = "recommendations")
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "objective_id")
    private UUID objectiveId;

    @Column(name = "related_assessment_id")
    private UUID relatedAssessmentId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private RecommendationType type;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(name = "target_entity_type", length = 50)
    private String targetEntityType;

    @Column(name = "target_entity_id")
    private UUID targetEntityId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private RecommendationStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;

    protected Recommendation() {
    }

    public Recommendation(UUID studentId, UUID objectiveId, RecommendationType type, String reason,
                          String targetEntityType, UUID targetEntityId) {
        this.studentId = studentId;
        this.objectiveId = objectiveId;
        this.type = type;
        this.reason = reason;
        this.targetEntityType = targetEntityType;
        this.targetEntityId = targetEntityId;
        this.status = RecommendationStatus.ACTIVE;
        this.createdAt = OffsetDateTime.now();
    }

    public void updateContent(String reason, String targetEntityType, UUID targetEntityId) {
        this.reason = reason;
        this.targetEntityType = targetEntityType;
        this.targetEntityId = targetEntityId;
    }

    public void accept(OffsetDateTime at) {
        this.status = RecommendationStatus.ACCEPTED;
        this.respondedAt = at;
    }

    public void dismiss(OffsetDateTime at) {
        this.status = RecommendationStatus.DISMISSED;
        this.respondedAt = at;
    }

    public void expire(OffsetDateTime at) {
        this.status = RecommendationStatus.EXPIRED;
        this.respondedAt = at;
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

    public UUID getRelatedAssessmentId() {
        return relatedAssessmentId;
    }

    public RecommendationType getType() {
        return type;
    }

    public String getReason() {
        return reason;
    }

    public String getTargetEntityType() {
        return targetEntityType;
    }

    public UUID getTargetEntityId() {
        return targetEntityId;
    }

    public RecommendationStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getRespondedAt() {
        return respondedAt;
    }
}
