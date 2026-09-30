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
 * Entity ứng với bảng `questions` — V4__module_assignment_assessment.sql,
 * Module04_Assignment_Assessment_FINAL.md mục E.4/E.5.
 *
 * {@link #courseId} là cross-module reference tới module-course (bảng courses).
 * {@link #lessonId} là cross-module reference tới module-course (bảng lessons) —
 * NULLABLE: NULL nghĩa là câu hỏi tổng hợp/ôn tập toàn course, không gắn 1 lesson
 * cụ thể (Module04_PATCH_lesson_id.md).
 * {@link #createdBy} là cross-module reference sang module-auth (bảng users).
 *
 * [ĐÃ SỬA — V5__module_analytics_lo_id_fix.sql] learningObjectiveId (UUID) thay cho
 * learningObjectiveCode (String tự do) cũ — tham chiếu đúng "id" của 1 phần tử trong
 * lessons.learning_objectives (JSONB, module-course), không phải chuỗi tự gõ không
 * liên kết gì. Ràng buộc cũ giữ nguyên tinh thần: learningObjectiveId != null BẮT
 * BUỘC kéo theo lessonId != null, và phải thực sự thuộc đúng lesson đó (validate ở
 * QuestionService qua LessonAccessPort, KHÔNG phải DB constraint vì objective nằm
 * trong JSONB của bảng khác, không có FK).
 *
 * {@link #options}/{@link #correctAnswer}: JSONB nhưng hình dạng dữ liệu thay đổi
 * tùy {@link #questionType} (options là mảng lựa chọn cho SINGLE/MULTIPLE_CHOICE;
 * correctAnswer có thể là mảng string ["A"] hoặc object {"expected_keywords":[...]}
 * cho SHORT_ANSWER) — dùng kiểu Object chung thay vì 1 class Java cố định hình dạng,
 * khác cách Lesson.learningObjectives dùng List&lt;LearningObjective&gt; cố định.
 */
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Column(name = "lesson_id")
    private UUID lessonId;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 30)
    private QuestionType questionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "cognitive_level", length = 30)
    private CognitiveLevel cognitiveLevel;

    @Column(name = "learning_objective_id")
    private UUID learningObjectiveId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Object options;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "correct_answer", columnDefinition = "jsonb")
    private Object correctAnswer;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionSource source = QuestionSource.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    private ReviewStatus reviewStatus = ReviewStatus.APPROVED;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Question() {
    }

    public Question(UUID courseId, String questionText, QuestionType questionType,
                     Difficulty difficulty, UUID createdBy) {
        this.courseId = courseId;
        this.questionText = questionText;
        this.questionType = questionType;
        this.difficulty = difficulty;
        this.createdBy = createdBy;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * Duyệt câu hỏi — E.6, PATCH /questions/{id}/review. Sau PATCH_4Decisions PATCH 1,
     * chỉ còn ý nghĩa thực tế cho source=MANUAL (nếu giáo viên chủ động lưu PENDING).
     */
    public void approve() {
        this.reviewStatus = ReviewStatus.APPROVED;
    }

    /** Từ chối câu hỏi — cùng ngữ cảnh với {@link #approve()}. */
    public void reject() {
        this.reviewStatus = ReviewStatus.REJECTED;
    }

    // ------------------------------------------------------------------
    // Getters & setters
    // ------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public UUID getLessonId() {
        return lessonId;
    }

    public void setLessonId(UUID lessonId) {
        this.lessonId = lessonId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public CognitiveLevel getCognitiveLevel() {
        return cognitiveLevel;
    }

    public void setCognitiveLevel(CognitiveLevel cognitiveLevel) {
        this.cognitiveLevel = cognitiveLevel;
    }

    public UUID getLearningObjectiveId() {
        return learningObjectiveId;
    }

    public void setLearningObjectiveId(UUID learningObjectiveId) {
        this.learningObjectiveId = learningObjectiveId;
    }

    public Object getOptions() {
        return options;
    }

    public void setOptions(Object options) {
        this.options = options;
    }

    public Object getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(Object correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public QuestionSource getSource() {
        return source;
    }

    public void setSource(QuestionSource source) {
        this.source = source;
    }

    public ReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
