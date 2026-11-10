package com.educmind.assignment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * PATCH bài tập (US-T03, US-T11 AC-T11.2/11.3).
 * `scopeType` = COURSE | TOPIC | LESSON: chỉ khi có giá trị mới thay đổi phạm vi;
 * null = giữ nguyên phạm vi hiện tại.
 */
public class UpdateAssignmentRequest {

    @Size(min = 1, max = 200, message = "Tiêu đề phải từ 1 đến 200 ký tự")
    private String title;

    private String description;

    private OffsetDateTime dueDate;

    @DecimalMin(value = "0.01", message = "Điểm tối đa phải lớn hơn 0")
    @Digits(integer = 3, fraction = 2, message = "Điểm tối đa tối đa 3 chữ số phần nguyên, 2 chữ số thập phân")
    private BigDecimal maxScore;

    private Boolean allowLate;

    private OffsetDateTime lateCloseDate;

    private String scopeType;

    private UUID topicId;

    private UUID lessonId;

    /** null = giữ nguyên danh sách mục tiêu. */
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

    public Boolean getAllowLate() {
        return allowLate;
    }

    public void setAllowLate(Boolean allowLate) {
        this.allowLate = allowLate;
    }

    public OffsetDateTime getLateCloseDate() {
        return lateCloseDate;
    }

    public void setLateCloseDate(OffsetDateTime lateCloseDate) {
        this.lateCloseDate = lateCloseDate;
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

    public List<UUID> getObjectiveIds() {
        return objectiveIds;
    }

    public void setObjectiveIds(List<UUID> objectiveIds) {
        this.objectiveIds = objectiveIds;
    }
}
