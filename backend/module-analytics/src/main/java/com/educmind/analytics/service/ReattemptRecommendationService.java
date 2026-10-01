package com.educmind.analytics.service;

import com.educmind.analytics.entity.KnowledgeGap;
import com.educmind.analytics.entity.Recommendation;
import com.educmind.analytics.repository.KnowledgeGapRepository;
import com.educmind.analytics.repository.RecommendationRepository;
import com.educmind.common.port.LearningEvidencePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Sinh recommendation REATTEMPT_ASSESSMENT (Module05 FINAL muc 4.5, nhanh 2).
 * Dieu kien: (a) diem quy doi ca attempt < resolve-threshold, (b) attemptNumber < maxAttempts,
 * (c) co knowledge_gap OPEN khop (student, lesson, objective) cua cac nhom trong attempt.
 * Chong trung: moi gap OPEN toi da 1 REATTEMPT_ASSESSMENT ACTIVE.
 * target_resource_id = NULL (hanh dong la lam lai bai, khong phai doc tai lieu).
 * Goi sau khi cac recompute mastery cua attempt xong, trong afterCommit,
 * nen can transaction rieng (REQUIRES_NEW).
 */
@Service
public class ReattemptRecommendationService {

    private final KnowledgeGapRepository gapRepository;
    private final RecommendationRepository recommendationRepository;
    private final GapProperties gapProperties;

    public ReattemptRecommendationService(KnowledgeGapRepository gapRepository,
                                          RecommendationRepository recommendationRepository,
                                          GapProperties gapProperties) {
        this.gapRepository = gapRepository;
        this.recommendationRepository = recommendationRepository;
        this.gapProperties = gapProperties;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateIfNeeded(UUID studentId, UUID courseId, int attemptNumber, int maxAttempts,
                                 List<LearningEvidencePort.ObjectiveGroupScore> groups) {
        if (attemptNumber >= maxAttempts) {
            return;
        }
        BigDecimal totalAwarded = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (LearningEvidencePort.ObjectiveGroupScore g : groups) {
            if (g.totalWeight() == null || g.totalWeight().signum() <= 0) {
                continue;
            }
            BigDecimal awarded = g.totalAwarded() != null ? g.totalAwarded() : BigDecimal.ZERO;
            totalAwarded = totalAwarded.add(awarded);
            totalWeight = totalWeight.add(g.totalWeight());
        }
        if (totalWeight.signum() <= 0) {
            return;
        }
        double attemptScore = totalAwarded.divide(totalWeight, 4, java.math.RoundingMode.HALF_UP).doubleValue();
        if (attemptScore >= gapProperties.getResolveThreshold()) {
            return;
        }
        for (LearningEvidencePort.ObjectiveGroupScore g : groups) {
            if (g.lessonId() == null || g.learningObjectiveId() == null) {
                continue;
            }
            Optional<KnowledgeGap> gap = gapRepository
                    .findByStudentIdAndLessonIdAndLearningObjectiveIdAndStatus(
                            studentId, g.lessonId(), g.learningObjectiveId(), KnowledgeGap.STATUS_OPEN);
            if (gap.isEmpty()) {
                continue;
            }
            UUID gapId = gap.get().getId();
            if (recommendationRepository.existsByKnowledgeGapIdAndTypeAndStatus(
                    gapId, Recommendation.TYPE_REATTEMPT_ASSESSMENT, Recommendation.STATUS_ACTIVE)) {
                continue;
            }
            recommendationRepository.save(new Recommendation(studentId, g.lessonId(),
                    g.learningObjectiveId(), courseId, gapId,
                    Recommendation.TYPE_REATTEMPT_ASSESSMENT, null));
        }
    }
}
