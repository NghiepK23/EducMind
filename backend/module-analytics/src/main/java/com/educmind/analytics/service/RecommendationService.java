package com.educmind.analytics.service;

import com.educmind.analytics.entity.KnowledgeGap;
import com.educmind.analytics.entity.Recommendation;
import com.educmind.analytics.repository.RecommendationRepository;
import com.educmind.common.port.LessonAccessPort;
import com.educmind.common.event.RecommendationCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Sinh recommendation nhanh mastery (Module05 FINAL muc 4.5, nhanh 1).
 * Khong co @Transactional rieng: chay chung transaction cua MasteryService.recompute.
 * target_resource_id: resource moi nhat cua lesson, NULL neu khong co ([TBD] theo spec).
 * Khong sinh trung: chi tao khi chua co recommendation ACTIVE cung gap / cung loai.
 */
@Service
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final RecommendationProperties properties;
    private final LessonAccessPort lessonAccessPort;
    private final ApplicationEventPublisher eventPublisher;

    public RecommendationService(RecommendationRepository recommendationRepository,
                                 RecommendationProperties properties,
                                 LessonAccessPort lessonAccessPort,
                                 ApplicationEventPublisher eventPublisher) {
        this.recommendationRepository = recommendationRepository;
        this.properties = properties;
        this.lessonAccessPort = lessonAccessPort;
        this.eventPublisher = eventPublisher;
    }

    /** Goi ngay sau khi 1 gap vua duoc mo. */
    public void generateForGap(KnowledgeGap gap, double masteryScore) {
        if (recommendationRepository.existsByKnowledgeGapIdAndStatus(
                gap.getId(), Recommendation.STATUS_ACTIVE)) {
            return;
        }
        String type = masteryScore < properties.getMasteryLow()
                ? Recommendation.TYPE_REVIEW_CONTENT
                : Recommendation.TYPE_PRACTICE_TARGETED;
        save(gap.getStudentId(), gap.getLessonId(), gap.getLearningObjectiveId(),
                gap.getCourseId(), gap.getId(), type);
    }

    /** Goi moi lan mastery duoc tinh lai; chi sinh khi mastery >= advanced-threshold. */
    public void generateAdvancedIfNeeded(UUID studentId, UUID lessonId, UUID learningObjectiveId,
                                         UUID courseId, double masteryScore) {
        if (masteryScore < properties.getAdvancedThreshold()) {
            return;
        }
        if (recommendationRepository
                .existsByStudentIdAndLessonIdAndLearningObjectiveIdAndTypeAndStatus(
                        studentId, lessonId, learningObjectiveId,
                        Recommendation.TYPE_ADVANCED_ACTIVITY, Recommendation.STATUS_ACTIVE)) {
            return;
        }
        save(studentId, lessonId, learningObjectiveId, courseId, null,
                Recommendation.TYPE_ADVANCED_ACTIVITY);
    }

    private void save(UUID studentId, UUID lessonId, UUID learningObjectiveId, UUID courseId,
                      UUID gapId, String type) {
        UUID resourceId = lessonAccessPort.findLatestResourceIdByLessonId(lessonId).orElse(null);
        Recommendation saved = recommendationRepository.save(new Recommendation(studentId, lessonId,
                learningObjectiveId, courseId, gapId, type, resourceId));
        eventPublisher.publishEvent(new RecommendationCreatedEvent(saved.getId(), studentId, courseId,
                lessonId, learningObjectiveId, type));
    }
}
