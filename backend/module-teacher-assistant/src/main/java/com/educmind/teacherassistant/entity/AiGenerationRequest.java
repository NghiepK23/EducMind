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
import java.util.Map;
import java.util.UUID;

/** Bảng ai_generation_requests — V10__module_teacher_assistant.sql, Module 7 mục G.4. */
@Entity
@Table(name = "ai_generation_requests")
public class AiGenerationRequest {

    public static final String TYPE_LESSON_PLAN = "LESSON_PLAN";
    public static final String TYPE_QUESTION_SET = "QUESTION_SET";
    public static final String TYPE_ASSESSMENT_MATRIX = "ASSESSMENT_MATRIX";

    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "request_type", nullable = false, length = 30)
    private String requestType;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "lesson_id")
    private UUID lessonId;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "prompt_template", nullable = false, length = 50)
    private String promptTemplate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_params", columnDefinition = "jsonb")
    private Map<String, Object> inputParams;

    @Column(name = "ai_provider", nullable = false, length = 30)
    private String aiProvider;

    @Column(nullable = false, length = 20)
    private String status = STATUS_PROCESSING;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    protected AiGenerationRequest() {
    }

    public AiGenerationRequest(String requestType, UUID courseId, UUID lessonId, UUID requestedBy,
                               String promptTemplate, Map<String, Object> inputParams, String aiProvider) {
        this.requestType = requestType;
        this.courseId = courseId;
        this.lessonId = lessonId;
        this.requestedBy = requestedBy;
        this.promptTemplate = promptTemplate;
        this.inputParams = inputParams;
        this.aiProvider = aiProvider;
    }

    public void markCompleted() {
        this.status = STATUS_COMPLETED;
        this.completedAt = OffsetDateTime.now();
    }

    public void markFailed(String message) {
        this.status = STATUS_FAILED;
        this.errorMessage = message;
        this.completedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public String getRequestType() { return requestType; }
    public UUID getCourseId() { return courseId; }
    public UUID getLessonId() { return lessonId; }
    public UUID getRequestedBy() { return requestedBy; }
    public String getPromptTemplate() { return promptTemplate; }
    public Map<String, Object> getInputParams() { return inputParams; }
    public String getAiProvider() { return aiProvider; }
    public String getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
}
