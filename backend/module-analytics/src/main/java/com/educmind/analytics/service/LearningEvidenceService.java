package com.educmind.analytics.service;

import com.educmind.analytics.entity.LearningEvidence;
import com.educmind.analytics.repository.LearningEvidenceRepository;
import com.educmind.common.port.CourseAccessPort;
import com.educmind.common.port.LearningEvidencePort;
import com.educmind.common.port.LessonAccessPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implement LearningEvidencePort — Module05_LearningEvidence_Analytics_FINAL.md mục 4.1 / 4.2 / 4.6.
 * Idempotent theo (sourceType, sourceId). Không tìm được class_id thì log ERROR và bỏ qua,
 * không throw để không làm hỏng luồng nộp/chấm bài của Module 4.
 * Sau khi evidence commit, gọi MasteryService.recompute (đồng bộ, transaction riêng, lỗi chỉ log).
 * Chưa tính knowledge gap (bước sau).
 */
@Service
public class LearningEvidenceService implements LearningEvidencePort {

    private static final Logger log = LoggerFactory.getLogger(LearningEvidenceService.class);
    private static final BigDecimal ONE = new BigDecimal("1.0000");

    private final LearningEvidenceRepository evidenceRepository;
    private final CourseAccessPort courseAccessPort;
    private final LessonAccessPort lessonAccessPort;
    private final MasteryService masteryService;
    private final ReattemptRecommendationService reattemptRecommendationService;

    public LearningEvidenceService(LearningEvidenceRepository evidenceRepository,
                                   CourseAccessPort courseAccessPort,
                                   LessonAccessPort lessonAccessPort,
                                   MasteryService masteryService,
                                   ReattemptRecommendationService reattemptRecommendationService) {
        this.evidenceRepository = evidenceRepository;
        this.courseAccessPort = courseAccessPort;
        this.lessonAccessPort = lessonAccessPort;
        this.masteryService = masteryService;
        this.reattemptRecommendationService = reattemptRecommendationService;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAssignmentSubmitted(UUID courseId, UUID studentId, UUID submissionId,
                                      UUID lessonId, OffsetDateTime occurredAt) {
        if (evidenceRepository.existsBySourceTypeAndSourceId(LearningEvidence.SOURCE_ASSIGNMENT, submissionId)) {
            return;
        }
        Optional<UUID> classId = courseAccessPort.findSourceClassId(courseId, studentId);
        if (classId.isEmpty()) {
            log.error("Không xác định được class_id: course={}, student={}, submission={} — bỏ qua evidence",
                    courseId, studentId, submissionId);
            return;
        }

        List<LessonAccessPort.ObjectiveInfo> objectives =
                lessonId == null ? List.of() : lessonAccessPort.findObjectivesByLessonId(lessonId);

        if (objectives.isEmpty()) {
            evidenceRepository.save(new LearningEvidence(studentId, classId.get(), courseId,
                    null, null, LearningEvidence.SOURCE_ASSIGNMENT, submissionId, ONE, null, occurredAt));
            return;
        }
        List<LearningEvidence> saved = new ArrayList<>();
        for (LessonAccessPort.ObjectiveInfo objective : objectives) {
            saved.add(evidenceRepository.save(new LearningEvidence(studentId, classId.get(), courseId,
                    lessonId, objective.id(), LearningEvidence.SOURCE_ASSIGNMENT, submissionId,
                    ONE, null, occurredAt)));
        }
        recomputeAfterCommit(saved);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAssignmentGraded(UUID submissionId, BigDecimal score, BigDecimal maxScore) {
        if (score == null || maxScore == null || maxScore.signum() <= 0) {
            log.warn("onAssignmentGraded bỏ qua vì score/maxScore không hợp lệ: submission={}", submissionId);
            return;
        }
        List<LearningEvidence> evidences =
                evidenceRepository.findBySourceTypeAndSourceId(LearningEvidence.SOURCE_ASSIGNMENT, submissionId);
        if (evidences.isEmpty()) {
            log.warn("Chấm điểm nhưng chưa có evidence cho submission={}", submissionId);
            return;
        }
        BigDecimal normalized = clamp01(score.divide(maxScore, 4, RoundingMode.HALF_UP));
        BigDecimal raw = score.setScale(2, RoundingMode.HALF_UP);
        for (LearningEvidence evidence : evidences) {
            evidence.updateScore(normalized, raw);
        }
        evidenceRepository.saveAll(evidences);
        recomputeAfterCommit(evidences);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAttemptGraded(UUID courseId, UUID studentId, UUID attemptId, UUID assessmentId,
                                int attemptNumber, int maxAttempts,
                                OffsetDateTime occurredAt, List<ObjectiveGroupScore> groups) {
        String sourceType = attemptNumber > 1
                ? LearningEvidence.SOURCE_REASSESSMENT : LearningEvidence.SOURCE_ASSESSMENT;
        if (evidenceRepository.existsBySourceTypeAndSourceId(sourceType, attemptId)) {
            return;
        }
        Optional<UUID> classId = courseAccessPort.findSourceClassId(courseId, studentId);
        if (classId.isEmpty()) {
            log.error("Không xác định được class_id: course={}, student={}, attempt={} — bỏ qua evidence",
                    courseId, studentId, attemptId);
            return;
        }
        List<LearningEvidence> saved = new ArrayList<>();
        for (ObjectiveGroupScore group : groups) {
            if (group.totalWeight() == null || group.totalWeight().signum() <= 0) {
                continue;
            }
            BigDecimal score = clamp01(group.totalAwarded().divide(group.totalWeight(), 4, RoundingMode.HALF_UP));
            BigDecimal raw = group.totalAwarded().setScale(2, RoundingMode.HALF_UP);
            saved.add(evidenceRepository.save(new LearningEvidence(studentId, classId.get(), courseId,
                    group.lessonId(), group.learningObjectiveId(), sourceType, attemptId,
                    score, raw, occurredAt)));
        }
        recomputeAfterCommit(saved, () -> reattemptRecommendationService.generateIfNeeded(
                studentId, courseId, attemptNumber, maxAttempts, groups));
    }

    /**
     * Tính lại mastery cho các evidence có cả lesson và objective, SAU KHI evidence commit.
     * Evidence "chung" (lesson/objective null) bị bỏ qua. Lỗi từng objective chỉ log ERROR,
     * không ảnh hưởng evidence đã lưu và không ảnh hưởng nộp/chấm bài.
     */
    private void recomputeAfterCommit(List<LearningEvidence> evidences) {
        recomputeAfterCommit(evidences, null);
    }

    /** afterRecompute chạy sau khi mọi recompute xong (cùng điểm afterCommit); lỗi chỉ log. */
    private void recomputeAfterCommit(List<LearningEvidence> evidences, Runnable afterRecompute) {
        List<LearningEvidence> targets = evidences.stream()
                .filter(e -> e.getLessonId() != null && e.getLearningObjectiveId() != null)
                .toList();
        if (targets.isEmpty()) {
            return;
        }
        Runnable task = () -> {
            for (LearningEvidence e : targets) {
                try {
                    masteryService.recompute(e.getStudentId(), e.getLessonId(),
                            e.getLearningObjectiveId(), e.getId());
                } catch (Exception ex) {
                    log.error("Tính mastery lỗi: student={}, lesson={}, objective={}, evidence={}",
                            e.getStudentId(), e.getLessonId(), e.getLearningObjectiveId(), e.getId(), ex);
                }
            }
            if (afterRecompute != null) {
                try {
                    afterRecompute.run();
                } catch (Exception ex) {
                    log.error("Sinh REATTEMPT_ASSESSMENT lỗi, bỏ qua", ex);
                }
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    private static BigDecimal clamp01(BigDecimal value) {
        if (value.signum() < 0) {
            return BigDecimal.ZERO.setScale(4);
        }
        if (value.compareTo(ONE) > 0) {
            return ONE;
        }
        return value;
    }
}
