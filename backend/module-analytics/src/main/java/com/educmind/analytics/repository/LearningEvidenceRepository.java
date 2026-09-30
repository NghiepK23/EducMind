package com.educmind.analytics.repository;

import com.educmind.analytics.entity.LearningEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LearningEvidenceRepository extends JpaRepository<LearningEvidence, UUID> {

    /** Toàn bộ evidence của 1 nguồn (1 submission hoặc 1 attempt) — dùng cho onAssignmentGraded. */
    List<LearningEvidence> findBySourceTypeAndSourceId(String sourceType, UUID sourceId);

    /** Idempotency: nguồn này đã sinh evidence chưa. */
    boolean existsBySourceTypeAndSourceId(String sourceType, UUID sourceId);

    /** Evidence của 1 học sinh theo 1 objective — dùng khi tính mastery (bước sau). */
    List<LearningEvidence> findByStudentIdAndLessonIdAndLearningObjectiveId(
            UUID studentId, UUID lessonId, UUID learningObjectiveId);
}
