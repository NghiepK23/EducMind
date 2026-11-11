package com.educmind.assignment.dto;

import com.educmind.assignment.entity.AssignmentSubmission;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * AC-S04.1: bài chưa chấm trả `pendingGrade = true` (UI hiển thị "Chờ chấm"),
 * bài đã chấm trả điểm + nhận xét.
 */
public record SubmissionResponse(UUID id, UUID assignmentId, UUID studentId, String status,
                                 String textContent, String fileUrl, BigDecimal score, String feedback,
                                 boolean pendingGrade, boolean edited, OffsetDateTime editedAt,
                                 OffsetDateTime submittedAt, OffsetDateTime gradedAt,
                                 String downloadUrl) {

    public static SubmissionResponse from(AssignmentSubmission s) {
        return new SubmissionResponse(s.getId(), s.getAssignmentId(), s.getStudentId(),
                s.getStatus().name(), s.getTextContent(), s.getFileUrl(), s.getScore(), s.getFeedback(),
                !s.isGraded(), s.isEdited(), s.getEditedAt(), s.getSubmittedAt(), s.getGradedAt(),
                s.getFileUrl() == null ? null : "/api/v1/submissions/" + s.getId() + "/file");
    }
}
