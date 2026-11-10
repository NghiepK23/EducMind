package com.educmind.assessment.entity;

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

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Bảng questions (Spec v2 — bảng 12; US-T05).
 * options/correct_answer là JSONB: mảng chuỗi phương án / mảng chỉ số đáp án đúng.
 */
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private QuestionType type;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private DifficultyLevel difficulty;

    @Column(name = "bloom_level", length = 30)
    private String bloomLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String options;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "correct_answer", columnDefinition = "jsonb")
    private String correctAnswer;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private QuestionSource source;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Question() {
    }

    public Question(UUID courseId, String content, QuestionType type, DifficultyLevel difficulty,
                    String bloomLevel, String options, String correctAnswer, UUID createdBy) {
        this.courseId = courseId;
        this.content = content;
        this.type = type;
        this.difficulty = difficulty;
        this.bloomLevel = bloomLevel;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.source = QuestionSource.MANUAL;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    /** AC-T11.4: chỉ cho sửa nội dung khi câu hỏi chưa được dùng trong bài kiểm tra có lượt làm. */
    public void update(String content, QuestionType type, DifficultyLevel difficulty, String bloomLevel,
                       String options, String correctAnswer, UUID actorId) {
        this.content = content;
        this.type = type;
        this.difficulty = difficulty;
        this.bloomLevel = bloomLevel;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.updatedBy = actorId;
    }

    /** AC-T11.4: vẫn cho sửa độ khó / mức Bloom khi câu hỏi đã được dùng. */
    public void updateMetadata(DifficultyLevel difficulty, String bloomLevel, UUID actorId) {
        this.difficulty = difficulty;
        this.bloomLevel = bloomLevel;
        this.updatedBy = actorId;
    }

    public void markDeleted(UUID actorId) {
        this.deletedAt = OffsetDateTime.now();
        this.updatedBy = actorId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public String getContent() {
        return content;
    }

    public QuestionType getType() {
        return type;
    }

    public DifficultyLevel getDifficulty() {
        return difficulty;
    }

    public String getBloomLevel() {
        return bloomLevel;
    }

    public String getOptions() {
        return options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public QuestionSource getSource() {
        return source;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }
}
