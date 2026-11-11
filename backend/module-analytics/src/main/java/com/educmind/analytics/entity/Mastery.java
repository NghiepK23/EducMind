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
 * Bảng mastery (Spec v2, mục F — bảng 19; US-H01, US-S05).
 *
 * Mỗi dòng = mức nắm vững của MỘT học sinh với MỘT mục tiêu, tính lại mỗi khi có
 * bằng chứng mới. Không có dữ liệu đánh giá ⇒ không có dòng (API trả "Chưa có dữ liệu").
 */
@Entity
@Table(name = "mastery")
public class Mastery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "objective_id", nullable = false)
    private UUID objectiveId;

    @Column(name = "mastery_score", nullable = false, precision = 4, scale = 3)
    private BigDecimal masteryScore;

    @Column(name = "evidence_count", nullable = false)
    private int evidenceCount;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Mastery() {
    }

    public Mastery(UUID studentId, UUID objectiveId, BigDecimal masteryScore, int evidenceCount) {
        this.studentId = studentId;
        this.objectiveId = objectiveId;
        this.masteryScore = masteryScore;
        this.evidenceCount = evidenceCount;
        this.updatedAt = OffsetDateTime.now();
    }

    public void update(BigDecimal masteryScore, int evidenceCount) {
        this.masteryScore = masteryScore;
        this.evidenceCount = evidenceCount;
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public UUID getObjectiveId() {
        return objectiveId;
    }

    public BigDecimal getMasteryScore() {
        return masteryScore;
    }

    public int getEvidenceCount() {
        return evidenceCount;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
