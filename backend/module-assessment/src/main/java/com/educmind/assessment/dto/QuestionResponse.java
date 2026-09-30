package com.educmind.assessment.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class QuestionResponse {

    private final UUID id;
    private final UUID courseId;
    private final UUID lessonId;
    private final String questionText;
    private final String questionType;
    private final String difficulty;
    private final String cognitiveLevel;
    private final UUID learningObjectiveId;
    private final Object options;
    private final Object correctAnswer;
    private final String explanation;
    private final String source;
    private final String reviewStatus;
    private final UUID createdBy;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    private final OffsetDateTime deletedAt;

    public QuestionResponse(UUID id, UUID courseId, UUID lessonId, String questionText, String questionType,
                             String difficulty, String cognitiveLevel, UUID learningObjectiveId,
                             Object options, Object correctAnswer, String explanation, String source,
                             String reviewStatus, UUID createdBy, OffsetDateTime createdAt,
                             OffsetDateTime updatedAt, OffsetDateTime deletedAt) {
        this.id = id;
        this.courseId = courseId;
        this.lessonId = lessonId;
        this.questionText = questionText;
        this.questionType = questionType;
        this.difficulty = difficulty;
        this.cognitiveLevel = cognitiveLevel;
        this.learningObjectiveId = learningObjectiveId;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.source = source;
        this.reviewStatus = reviewStatus;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public UUID getLessonId() {
        return lessonId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getQuestionType() {
        return questionType;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getCognitiveLevel() {
        return cognitiveLevel;
    }

    public UUID getLearningObjectiveId() {
        return learningObjectiveId;
    }

    public Object getOptions() {
        return options;
    }

    public Object getCorrectAnswer() {
        return correctAnswer;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getSource() {
        return source;
    }

    public String getReviewStatus() {
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
}
