package com.educmind.common.port;

import java.util.UUID;

/**
 * Port ghi bằng chứng học tập (bảng learning_evidence — Spec v2 mục F).
 *
 * Quy ước sourceId của Spec v2:
 *  - ASSIGNMENT → id của BÀI NỘP (assignment_submissions.id)
 *  - ASSESSMENT → id của BÀI KIỂM TRA (assessments.id)
 *
 * score là điểm chuẩn hoá 0..1 (điểm đạt / điểm tối đa).
 */
public interface LearningEvidencePort {

    enum SourceType {
        ASSIGNMENT,
        ASSESSMENT
    }

    /**
     * Ghi hoặc cập nhật bằng chứng cho (học sinh, mục tiêu, nguồn).
     * Idempotent: chấm lại chỉ cập nhật điểm, không tạo bản ghi trùng.
     */
    void record(UUID studentId, UUID objectiveId, SourceType sourceType, UUID sourceId, double score);
}
