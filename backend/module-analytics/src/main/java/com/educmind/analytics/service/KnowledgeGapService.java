package com.educmind.analytics.service;

import com.educmind.analytics.entity.KnowledgeGap;
import com.educmind.analytics.repository.KnowledgeGapRepository;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import com.educmind.common.event.KnowledgeGapOpenedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Mo / dong knowledge gap - Module05_LearningEvidence_Analytics_FINAL.md muc 4.4.
 * Khong co @Transactional rieng: chay chung transaction cua MasteryService.recompute,
 * nen mastery va gap luon nhat quan.
 */
@Service
public class KnowledgeGapService {

    private final KnowledgeGapRepository knowledgeGapRepository;
    private final GapProperties gapProperties;
    private final RecommendationService recommendationService;
    private final ApplicationEventPublisher eventPublisher;

    public KnowledgeGapService(KnowledgeGapRepository knowledgeGapRepository,
                               GapProperties gapProperties,
                               RecommendationService recommendationService,
                               ApplicationEventPublisher eventPublisher) {
        this.knowledgeGapRepository = knowledgeGapRepository;
        this.gapProperties = gapProperties;
        this.recommendationService = recommendationService;
        this.eventPublisher = eventPublisher;
    }

    public void evaluate(UUID studentId, UUID lessonId, UUID learningObjectiveId,
                         UUID courseId, double masteryScore, int evidenceCount) {

        Optional<KnowledgeGap> openGap = knowledgeGapRepository
                .findByStudentIdAndLessonIdAndLearningObjectiveIdAndStatus(
                        studentId, lessonId, learningObjectiveId, KnowledgeGap.STATUS_OPEN);

        if (openGap.isPresent()) {
            if (masteryScore >= gapProperties.getResolveThreshold()) {
                KnowledgeGap gap = openGap.get();
                gap.resolve(String.format(Locale.ROOT,
                        "Mastery %.4f >= resolve-threshold %.2f (%d evidence)",
                        masteryScore, gapProperties.getResolveThreshold(), evidenceCount));
                knowledgeGapRepository.save(gap);
            }
            recommendationService.generateAdvancedIfNeeded(studentId, lessonId,
                    learningObjectiveId, courseId, masteryScore);
            return;
        }

        if (masteryScore < gapProperties.getOpenThreshold()
                && evidenceCount >= gapProperties.getMinEvidence()) {
            KnowledgeGap gap = new KnowledgeGap(studentId, lessonId, learningObjectiveId, courseId,
                    String.format(Locale.ROOT,
                            "Mastery %.4f < open-threshold %.2f (%d evidence)",
                            masteryScore, gapProperties.getOpenThreshold(), evidenceCount));
            KnowledgeGap saved = knowledgeGapRepository.save(gap);
            eventPublisher.publishEvent(new KnowledgeGapOpenedEvent(saved.getId(), studentId,
                    courseId, lessonId, learningObjectiveId));
            recommendationService.generateForGap(saved, masteryScore);
        }
        recommendationService.generateAdvancedIfNeeded(studentId, lessonId,
                learningObjectiveId, courseId, masteryScore);
    }
}
