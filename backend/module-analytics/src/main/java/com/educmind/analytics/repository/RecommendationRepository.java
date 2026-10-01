package com.educmind.analytics.repository;

import com.educmind.analytics.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

    /** Đã có recommendation theo trạng thái này cho gap chưa — chống sinh trùng cho loại gắn gap. */
    boolean existsByKnowledgeGapIdAndStatus(UUID knowledgeGapId, String status);

    /** Chống sinh trùng cho loại không gắn gap (ADVANCED_ACTIVITY). */
    boolean existsByStudentIdAndLessonIdAndLearningObjectiveIdAndTypeAndStatus(
            UUID studentId, UUID lessonId, UUID learningObjectiveId, String type, String status);
}
