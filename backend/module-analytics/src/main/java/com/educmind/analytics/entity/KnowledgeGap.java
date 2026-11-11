package com.educmind.analytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Bảng knowledge_gaps (Spec v2, mục F — bảng 20; US-T08).
 *
 * Mỗi (học sinh, mục tiêu) dùng lại MỘT dòng: mở lại khi kết quả đi xuống, đánh dấu
 * RESOLVED khi đạt ngưỡng, DISMISSED khi giáo viên bỏ qua kèm lý do.
 * Trigger V7 (fn_block_reopen_dismissed_gap) chặn mở lại nếu chưa có bằng chứng mới hơn.
 */
@Entity
@Table(name = "knowledge_gaps")
public class KnowledgeGap {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "objective_id", nullable = false)
    private UUID objectiveId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private GapStatus status;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(name = "opened_at", nullable = false)
    private OffsetDateTime openedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "dismissed_by")
    private UUID dismissedBy;

    @Column(name = "dismissed_at")
    private OffsetDateTime dismissedAt;

    @Column(name = "dismiss_reason", columnDefinition = "text")
    private String dismissReason;

    protected KnowledgeGap() {
    }

    public KnowledgeGap(UUID studentId, UUID objectiveId, String reason) {
        this.studentId = studentId;
        this.objectiveId = objectiveId;
        this.status = GapStatus.OPEN;
        this.reason = reason;
        this.openedAt = OffsetDateTime.now();
    }

    public void updateReason(String reason) {
        this.reason = reason;
    }

    /** Mở lại sau khi đã RESOLVED (hoặc DISMISSED nhưng đã có bằng chứng mới). */
    public void reopen(String reason) {
        this.status = GapStatus.OPEN;
        this.reason = reason;
        this.openedAt = OffsetDateTime.now();
        this.resolvedAt = null;
        this.dismissedBy = null;
        this.dismissedAt = null;
        this.dismissReason = null;
    }

    public void resolve(OffsetDateTime at) {
        this.status = GapStatus.RESOLVED;
        this.resolvedAt = at;
    }

    public void dismiss(UUID teacherId, OffsetDateTime at, String reason) {
        this.status = GapStatus.DISMISSED;
        this.dismissedBy = teacherId;
        this.dismissedAt = at;
        this.dismissReason = reason;
        this.resolvedAt = null;
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

    public GapStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public OffsetDateTime getOpenedAt() {
        return openedAt;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public UUID getDismissedBy() {
        return dismissedBy;
    }

    public OffsetDateTime getDismissedAt() {
        return dismissedAt;
    }

    public String getDismissReason() {
        return dismissReason;
    }
}
