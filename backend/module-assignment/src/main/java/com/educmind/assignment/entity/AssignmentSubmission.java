package com.educmind.assignment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Bảng assignment_submissions (Spec v2 — bảng 10; US-S02, US-S07, US-T04).
 *
 * Trigger DB `trg_restrict_submission_edit` (V7 — J.3) là nguồn sự thật cho các quy tắc:
 * không sửa/xoá khi đã chấm hoặc quá hạn, nộp lại = khôi phục dòng đã xoá mềm,
 * tự tính SUBMITTED/LATE và is_edited/edited_at.
 */
@Entity
@Table(name = "assignment_submissions")
public class AssignmentSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "assignment_id", nullable = false)
    private UUID assignmentId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "text_content", columnDefinition = "text")
    private String textContent;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private SubmissionStatus status;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(columnDefinition = "text")
    private String feedback;

    @Column(name = "graded_by")
    private UUID gradedBy;

    @Column(name = "graded_at")
    private OffsetDateTime gradedAt;

    @Column(name = "is_edited", nullable = false)
    private boolean edited;

    @Column(name = "edited_at")
    private OffsetDateTime editedAt;

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected AssignmentSubmission() {
    }

    public AssignmentSubmission(UUID assignmentId, UUID studentId, String textContent,
                                String fileUrl, SubmissionStatus status) {
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        this.textContent = textContent;
        this.fileUrl = fileUrl;
        this.status = status;
    }

    @PrePersist
    void onCreate() {
        if (submittedAt == null) {
            submittedAt = OffsetDateTime.now();
        }
    }

    /** Học sinh sửa bài (AC-S07.1/2). Trigger tính lại status + is_edited + edited_at. */
    public void editContent(String textContent, String fileUrl) {
        this.textContent = textContent;
        this.fileUrl = fileUrl;
    }

    /** Nộp lại sau khi đã xoá mềm (AC-S07.4) — trigger DB reset điểm/nhận xét/trạng thái. */
    public void restore(String textContent, String fileUrl) {
        this.deletedAt = null;
        this.textContent = textContent;
        this.fileUrl = fileUrl;
        this.score = null;
        this.feedback = null;
        this.gradedBy = null;
        this.gradedAt = null;
        this.edited = false;
        this.editedAt = null;
        this.submittedAt = OffsetDateTime.now();
    }

    /** Xoá mềm bài nộp (AC-S07.4) — trigger xoá luôn bằng chứng học tập liên quan. */
    public void markDeleted() {
        this.deletedAt = OffsetDateTime.now();
    }

    /** Giáo viên chấm / chấm lại (AC-T04.2, AC-T04.4). */
    public void grade(BigDecimal score, String feedback, UUID teacherId, OffsetDateTime gradedAt) {
        this.score = score;
        this.feedback = feedback;
        this.gradedBy = teacherId;
        this.gradedAt = gradedAt;
    }

    public boolean isGraded() {
        return gradedAt != null;
    }

    public boolean hasContent() {
        return (textContent != null && !textContent.isBlank()) || (fileUrl != null && !fileUrl.isBlank());
    }

    public UUID getId() {
        return id;
    }

    public UUID getAssignmentId() {
        return assignmentId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public String getTextContent() {
        return textContent;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public BigDecimal getScore() {
        return score;
    }

    public String getFeedback() {
        return feedback;
    }

    public UUID getGradedBy() {
        return gradedBy;
    }

    public OffsetDateTime getGradedAt() {
        return gradedAt;
    }

    public boolean isEdited() {
        return edited;
    }

    public OffsetDateTime getEditedAt() {
        return editedAt;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }
}
