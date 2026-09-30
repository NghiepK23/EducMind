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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implement LearningEvidencePort — Module05_LearningEvidence_Analytics_FINAL.md mục 4.1 / 4.2 / 4.6.
 * Idempotent theo (sourceType, sourceId). Không tìm được class_id thì log ERROR và bỏ qua,
 * không throw để không làm hỏng luồng nộp/chấm bài của Module 4.
 * Chưa tính mastery/knowledge gap (bước sau).
 */
@Service
public class LearningEvidenceService implements LearningEvidencePort {

    private static final Logger log = LoggerFactory.getLogger(LearningEvidenceService.class);
    private static final BigDecimal ONE = new BigDecimal("1.0000");

    private final LearningEvidenceRepository evidenceRepository;
    private final CourseAccessPort courseAccessPort;
    private final LessonAccessPort lessonAccessPort;

    public LearningEvidenceService(LearningEvidenceRepository evidenceRepository,
                                   CourseAccessPort courseAccessPort,
                                   LessonAccessPort lessonAccessPort) {
        this.evidenceRepository = evidenceRepository;
        this.courseAccessPort = courseAccessPort;
        this.lessonAccessPort = lessonAccessPort;
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
        for (LessonAccessPort.ObjectiveInfo objective : objectives) {
            evidenceRepository.save(new LearningEvidence(studentId, classId.get(), courseId,
                    lessonId, objective.id(), LearningEvidence.SOURCE_ASSIGNMENT, submissionId,
                    ONE, null, occurredAt));
        }
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
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAttemptGraded(UUID courseId, UUID studentId, UUID attemptId, int attemptNumber,
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
        for (ObjectiveGroupScore group : groups) {
            if (group.totalWeight() == null || group.totalWeight().signum() <= 0) {
                continue;
            }
            BigDecimal score = clamp01(group.totalAwarded().divide(group.totalWeight(), 4, RoundingMode.HALF_UP));
            BigDecimal raw = group.totalAwarded().setScale(2, RoundingMode.HALF_UP);
            evidenceRepository.save(new LearningEvidence(studentId, classId.get(), courseId,
                    group.lessonId(), group.learningObjectiveId(), sourceType, attemptId,
                    score, raw, occurredAt));
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
