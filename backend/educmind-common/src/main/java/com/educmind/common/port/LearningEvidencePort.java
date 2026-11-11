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

    /**
     * Như {@link #record} nhưng chỉ ghi đè khi điểm mới cao hơn.
     * Dùng cho bài kiểm tra: làm lại nhiều lượt thì bằng chứng giữ kết quả tốt nhất (AC-T14.2).
     */
    void recordHighest(UUID studentId, UUID objectiveId, SourceType sourceType, UUID sourceId, double score);

    /**
     * true nếu mục tiêu đã có ít nhất một dòng bằng chứng học tập.
     * Dùng để chặn đổi mã mục tiêu (AC-T11.1).
     */
    boolean hasEvidenceForObjective(UUID objectiveId);
}
