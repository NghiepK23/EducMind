package com.educmind.analytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity ứng với bảng `mastery` — V7__module_analytics_tables.sql,
 * Module05_LearningEvidence_Analytics_FINAL.md mục 3.2 / 4.3.
 *
 * Mỗi dòng ứng với 1 (studentId, lessonId, learningObjectiveId) — unique ở DB.
 * Không có FK cứng tới objective vì nó nằm trong JSONB của lessons.
 * Được tính lại (upsert) mỗi khi có evidence mới cho đúng khóa đó.
 */
@Entity
@Table(name = "mastery")
public class Mastery {

    public static final String CONFIDENCE_LOW = "LOW";
    public static final String CONFIDENCE_MEDIUM = "MEDIUM";
    public static final String CONFIDENCE_HIGH = "HIGH";
    public static final String DEFAULT_METHOD = "RULE_V1_WEIGHTED_AVG_DECAY";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(name = "learning_objective_id", nullable = false)
    private UUID learningObjectiveId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "mastery_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal masteryScore;

    @Column(name = "evidence_count", nullable = false)
    private int evidenceCount;

    @Column(name = "confidence_level", nullable = false, length = 10)
    private String confidenceLevel = CONFIDENCE_LOW;

    @Column(name = "last_evidence_at")
    private OffsetDateTime lastEvidenceAt;

    @Column(name = "calculation_method", nullable = false, length = 50)
    private String calculationMethod = DEFAULT_METHOD;

    @Column(name = "computed_at", nullable = false)
    private OffsetDateTime computedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Mastery() {
    }

    public Mastery(UUID studentId, UUID lessonId, UUID learningObjectiveId, UUID courseId,
                   BigDecimal masteryScore, int evidenceCount, String confidenceLevel,
                   OffsetDateTime lastEvidenceAt) {
        this.studentId = studentId;
        this.lessonId = lessonId;
        this.learningObjectiveId = learningObjectiveId;
        this.courseId = courseId;
        this.masteryScore = masteryScore;
        this.evidenceCount = evidenceCount;
        this.confidenceLevel = confidenceLevel;
        this.lastEvidenceAt = lastEvidenceAt;
        OffsetDateTime now = OffsetDateTime.now();
        this.computedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Cập nhật kết quả sau mỗi lần tính lại mastery (upsert vào dòng đã tồn tại). */
    public void applyRecomputation(BigDecimal masteryScore, int evidenceCount,
                                   String confidenceLevel, OffsetDateTime lastEvidenceAt) {
        this.masteryScore = masteryScore;
        this.evidenceCount = evidenceCount;
        this.confidenceLevel = confidenceLevel;
        this.lastEvidenceAt = lastEvidenceAt;
        OffsetDateTime now = OffsetDateTime.now();
        this.computedAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public UUID getLessonId() {
        return lessonId;
    }

    public UUID getLearningObjectiveId() {
        return learningObjectiveId;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public BigDecimal getMasteryScore() {
        return masteryScore;
    }

    public int getEvidenceCount() {
        return evidenceCount;
    }

    public String getConfidenceLevel() {
        return confidenceLevel;
    }

    public OffsetDateTime getLastEvidenceAt() {
        return lastEvidenceAt;
    }

    public String getCalculationMethod() {
        return calculationMethod;
    }

    public OffsetDateTime getComputedAt() {
        return computedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
