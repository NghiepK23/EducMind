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
 * Entity ứng với bảng `learning_evidence` — V7__module_analytics_tables.sql,
 * Module05_LearningEvidence_Analytics_FINAL.md mục 3.1 / 4.1 / 4.2.
 *
 * Khóa objective là (lessonId, learningObjectiveId). Cả hai cùng null = evidence "chung"
 * (không tính vào mastery per-objective). learningObjectiveId khác null thì lessonId bắt buộc
 * khác null (CHECK ở DB). Không có FK cứng tới objective vì nó nằm trong JSONB của lessons.
 *
 * Append-only, trừ đúng 1 ngoại lệ: {@link #updateScore(BigDecimal, BigDecimal)} khi giáo viên
 * chấm assignment sau khi evidence đã được tạo (spec mục 4.1).
 */
@Entity
@Table(name = "learning_evidence")
public class LearningEvidence {

    public static final String SOURCE_ASSIGNMENT = "ASSIGNMENT";
    public static final String SOURCE_ASSESSMENT = "ASSESSMENT";
    public static final String SOURCE_REASSESSMENT = "REASSESSMENT";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "lesson_id")
    private UUID lessonId;

    @Column(name = "learning_objective_id")
    private UUID learningObjectiveId;

    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal score;

    @Column(name = "raw_score", precision = 6, scale = 2)
    private BigDecimal rawScore;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal weight = new BigDecimal("1.00");

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected LearningEvidence() {
    }

    public LearningEvidence(UUID studentId, UUID classId, UUID courseId,
                            UUID lessonId, UUID learningObjectiveId,
                            String sourceType, UUID sourceId,
                            BigDecimal score, BigDecimal rawScore,
                            OffsetDateTime occurredAt) {
        this.studentId = studentId;
        this.classId = classId;
        this.courseId = courseId;
        this.lessonId = lessonId;
        this.learningObjectiveId = learningObjectiveId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.score = score;
        this.rawScore = rawScore;
        this.occurredAt = occurredAt;
        this.createdAt = OffsetDateTime.now();
    }

    /** Ngoại lệ append-only duy nhất (spec 4.1): cập nhật điểm sau khi giáo viên chấm assignment. */
    public void updateScore(BigDecimal score, BigDecimal rawScore) {
        this.score = score;
        this.rawScore = rawScore;
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public UUID getClassId() {
        return classId;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public UUID getLessonId() {
        return lessonId;
    }

    public UUID getLearningObjectiveId() {
        return learningObjectiveId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public BigDecimal getScore() {
        return score;
    }

    public BigDecimal getRawScore() {
        return rawScore;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
