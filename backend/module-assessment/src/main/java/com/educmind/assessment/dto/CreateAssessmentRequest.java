package com.educmind.assessment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** US-T06 (AC-T06.1 … AC-T06.6). */
public class CreateAssessmentRequest {

    public static class QuestionWeight {

        @NotNull(message = "questionId không được để trống")
        private UUID questionId;

        @NotNull(message = "Trọng số câu hỏi không được để trống")
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

    @NotBlank(message = "Tiêu đề bài kiểm tra không được để trống")
    @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
    private String title;

    @NotNull(message = "Thời lượng không được để trống")
    @Min(value = 1, message = "Thời lượng phải lớn hơn 0 phút")
    private Integer durationMinutes;

    @NotNull(message = "Số lần làm tối đa không được để trống")
    @Min(value = 1, message = "Số lần làm tối đa phải ≥ 1")
    private Integer maxAttempts;

    @NotNull(message = "Thời gian mở không được để trống")
    private OffsetDateTime openAt;

    @NotNull(message = "Thời gian đóng không được để trống")
    private OffsetDateTime closeAt;

    private UUID topicId;

    private UUID lessonId;

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
