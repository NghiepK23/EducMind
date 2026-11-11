package com.educmind.assessment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** PATCH bài kiểm tra (US-T06, US-T11 AC-T11.5, AC-T11.6). */
public class UpdateAssessmentRequest {

    public static class QuestionWeight {

        private UUID questionId;

        @DecimalMin(value = "0.01", message = "Trọng số phải lớn hơn 0")
        private BigDecimal weight;

        public UUID getQuestionId() {
            return questionId;
        }

        public void setQuestionId(UUID questionId) {
            this.questionId = questionId;
        }

        public BigDecimal getWeight() {
            return weight;
        }

        public void setWeight(BigDecimal weight) {
            this.weight = weight;
        }
    }

    @Size(min = 1, max = 200, message = "Tiêu đề phải từ 1 đến 200 ký tự")
    private String title;

    @Min(value = 1, message = "Thời lượng phải lớn hơn 0 phút")
    private Integer durationMinutes;

    @Min(value = 1, message = "Số lần làm tối đa phải ≥ 1")
    private Integer maxAttempts;

    private OffsetDateTime openAt;

    private OffsetDateTime closeAt;

    private String scopeType;

    private UUID topicId;

    private UUID lessonId;

    /** null = giữ nguyên danh sách câu hỏi; chỉ áp dụng khi bài kiểm tra chưa có lượt làm. */
    private List<QuestionWeight> questions;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public OffsetDateTime getOpenAt() {
        return openAt;
    }

    public void setOpenAt(OffsetDateTime openAt) {
        this.openAt = openAt;
    }

    public OffsetDateTime getCloseAt() {
        return closeAt;
    }

    public void setCloseAt(OffsetDateTime closeAt) {
        this.closeAt = closeAt;
    }

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public UUID getTopicId() {
        return topicId;
    }

    public void setTopicId(UUID topicId) {
        this.topicId = topicId;
    }

    public UUID getLessonId() {
        return lessonId;
    }

    public void setLessonId(UUID lessonId) {
        this.lessonId = lessonId;
    }

    public List<QuestionWeight> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionWeight> questions) {
        this.questions = questions;
    }
}
