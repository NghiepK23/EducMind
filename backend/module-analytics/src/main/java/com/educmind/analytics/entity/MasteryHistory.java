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
 * Entity ứng với bảng `mastery_history` — V7__module_analytics_tables.sql,
 * Module05_LearningEvidence_Analytics_FINAL.md mục 3.2.
 *
 * Append-only: mỗi lần mastery được tính lại thì thêm 1 dòng snapshot, không sửa dòng cũ.
 * triggerEvidenceId là evidence đã gây ra lần tính lại này (NOT NULL, có FK ở DB).
 */
@Entity
@Table(name = "mastery_history")
public class MasteryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "mastery_id", nullable = false)
    private UUID masteryId;

    @Column(name = "mastery_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal masteryScore;

    @Column(name = "snapshot_at", nullable = false)
    private OffsetDateTime snapshotAt;

    @Column(name = "trigger_evidence_id", nullable = false)
    private UUID triggerEvidenceId;

    protected MasteryHistory() {
    }

    public MasteryHistory(UUID masteryId, BigDecimal masteryScore, UUID triggerEvidenceId) {
        this.masteryId = masteryId;
        this.masteryScore = masteryScore;
        this.triggerEvidenceId = triggerEvidenceId;
        this.snapshotAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getMasteryId() {
        return masteryId;
    }

    public BigDecimal getMasteryScore() {
        return masteryScore;
    }

    public OffsetDateTime getSnapshotAt() {
        return snapshotAt;
    }

    public UUID getTriggerEvidenceId() {
        return triggerEvidenceId;
    }
}
