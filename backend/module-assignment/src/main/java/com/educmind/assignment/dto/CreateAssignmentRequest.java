package com.educmind.assignment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** US-T03 (AC-T03.1, AC-T03.2, AC-T03.5, AC-T03.6). */
public class CreateAssignmentRequest {

    @NotBlank(message = "Tiêu đề bài tập không được để trống")
    @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
    private String title;

    @NotBlank(message = "Đề bài không được để trống")
    private String description;

    @NotNull(message = "Hạn nộp không được để trống")
    private OffsetDateTime dueDate;

    @NotNull(message = "Điểm tối đa không được để trống")
    @DecimalMin(value = "0.01", message = "Điểm tối đa phải lớn hơn 0")
    @Digits(integer = 3, fraction = 2, message = "Điểm tối đa tối đa 3 chữ số phần nguyên, 2 chữ số thập phân")
    private BigDecimal maxScore;

    private boolean allowLate;

    private OffsetDateTime lateCloseDate;

    /** Phạm vi: chỉ chọn tối đa một trong hai (AC-T03.5). */
    private UUID topicId;

    private UUID lessonId;

    private List<UUID> objectiveIds;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OffsetDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(OffsetDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public boolean isAllowLate() {
        return allowLate;
    }

    public void setAllowLate(boolean allowLate) {
        this.allowLate = allowLate;
    }

    public OffsetDateTime getLateCloseDate() {
        return lateCloseDate;
    }

    public void setLateCloseDate(OffsetDateTime lateCloseDate) {
        this.lateCloseDate = lateCloseDate;
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

    public List<UUID> getObjectiveIds() {
        return objectiveIds;
    }

    public void setObjectiveIds(List<UUID> objectiveIds) {
        this.objectiveIds = objectiveIds;
    }
}
