package com.educmind.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Bảng learning_objectives (Spec v2, mục D — bảng 7).
 * Mã (code) chỉ duy nhất trong phạm vi một bài học: UNIQUE(lesson_id, code).
 * Đây là khóa tham chiếu cho questions / learning_evidence / mastery / knowledge_gaps.
 */
@Entity
@Table(name = "learning_objectives")
public class LearningObjective {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false)
    private String description;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected LearningObjective() {
    }

    public LearningObjective(UUID lessonId, String code, String description) {
        this.lessonId = lessonId;
        this.code = code;
        this.description = description;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    /** AC-T11.1: chỉ cho sửa mô tả (việc đổi mã do tầng service kiểm soát). */
    public void updateDescription(String description) {
        this.description = description;
    }

    /**
     * AC-T11.1: đổi mã chỉ được phép khi mục tiêu CHƯA có bằng chứng học tập.
     * Tầng service kiểm tra điều kiện rồi mới gọi hàm này.
     */
    public void updateCode(String code) {
        this.code = code;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLessonId() {
        return lessonId;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
