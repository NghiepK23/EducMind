package com.educmind.analytics.service;

import com.educmind.analytics.entity.LearningEvidence;
import com.educmind.analytics.entity.Mastery;
import com.educmind.analytics.entity.MasteryHistory;
import com.educmind.analytics.repository.LearningEvidenceRepository;
import com.educmind.analytics.repository.MasteryHistoryRepository;
import com.educmind.analytics.repository.MasteryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Tính mastery — Module05_LearningEvidence_Analytics_FINAL.md mục 4.3.
 *
 * mastery_score = SUM(score * weight * decay) / SUM(weight * decay)
 * decay = 0.5 ^ (ageDays / halfLifeDays)
 *
 * Nhóm theo (studentId, lessonId, learningObjectiveId). Chỉ gọi cho evidence có cả lesson và
 * objective khác null. Chạy trong transaction riêng (REQUIRES_NEW): người gọi phải gọi SAU KHI
 * evidence đã commit, và bọc try/catch để lỗi tính mastery không ảnh hưởng nộp/chấm bài.
 * Ngưỡng half-life và confidence là giá trị tạm [TBD] — xem MasteryProperties.
 */
@Service
public class MasteryService {

    private static final Logger log = LoggerFactory.getLogger(MasteryService.class);
    private static final BigDecimal ONE = new BigDecimal("1.0000");
    private static final double SECONDS_PER_DAY = 86400.0;

    private final LearningEvidenceRepository evidenceRepository;
    private final MasteryRepository masteryRepository;
    private final MasteryHistoryRepository historyRepository;
    private final MasteryProperties properties;
    private final KnowledgeGapService knowledgeGapService;

    public MasteryService(LearningEvidenceRepository evidenceRepository,
                          MasteryRepository masteryRepository,
                          MasteryHistoryRepository historyRepository,
                          MasteryProperties properties,
                          KnowledgeGapService knowledgeGapService) {
        this.evidenceRepository = evidenceRepository;
        this.masteryRepository = masteryRepository;
        this.historyRepository = historyRepository;
        this.properties = properties;
        this.knowledgeGapService = knowledgeGapService;
    }

    /**
     * Tính lại mastery cho 1 (student, lesson, objective) và ghi 1 snapshot lịch sử.
     * triggerEvidenceId là evidence đã gây ra lần tính lại này.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recompute(UUID studentId, UUID lessonId, UUID learningObjectiveId,
                          UUID triggerEvidenceId) {
        List<LearningEvidence> evidences = evidenceRepository
                .findByStudentIdAndLessonIdAndLearningObjectiveId(studentId, lessonId, learningObjectiveId);
        if (evidences.isEmpty()) {
            log.warn("Không có evidence để tính mastery: student={}, lesson={}, objective={}",
                    studentId, lessonId, learningObjectiveId);
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        BigDecimal score = computeScore(evidences, now, properties.getHalfLifeDays());
        if (score == null) {
            log.warn("Tổng trọng số bằng 0, bỏ qua mastery: student={}, lesson={}, objective={}",
                    studentId, lessonId, learningObjectiveId);
            return;
        }

        UUID courseId = evidences.get(0).getCourseId();
        int count = evidences.size();
        String confidence = confidenceFor(count);
        OffsetDateTime lastEvidenceAt = evidences.stream()
                .map(LearningEvidence::getOccurredAt)
                .max(Comparator.naturalOrder())
                .orElse(null);

        Mastery mastery = masteryRepository
                .findByStudentIdAndLessonIdAndLearningObjectiveId(studentId, lessonId, learningObjectiveId)
                .orElse(null);
        if (mastery == null) {
            mastery = new Mastery(studentId, lessonId, learningObjectiveId, courseId,
                    score, count, confidence, lastEvidenceAt);
        } else {
            mastery.applyRecomputation(score, count, confidence, lastEvidenceAt);
        }
        mastery = masteryRepository.save(mastery);

        historyRepository.save(new MasteryHistory(mastery.getId(), score, triggerEvidenceId));

        knowledgeGapService.evaluate(studentId, lessonId, learningObjectiveId, courseId,
                score.doubleValue(), count);
    }

    /**
     * Công thức thuần, không phụ thuộc Spring/DB (để unit test).
     * Trả về null nếu tổng trọng số bằng 0.
     */
    public static BigDecimal computeScore(List<LearningEvidence> evidences,
                                          OffsetDateTime now, double halfLifeDays) {
        double numerator = 0;
        double denominator = 0;
        for (LearningEvidence evidence : evidences) {
            double weight = evidence.getWeight() == null ? 1.0 : evidence.getWeight().doubleValue();
            double ageDays = Math.max(0,
                    Duration.between(evidence.getOccurredAt(), now).toSeconds() / SECONDS_PER_DAY);
            double decay = halfLifeDays > 0 ? Math.pow(0.5, ageDays / halfLifeDays) : 1.0;
            double effectiveWeight = weight * decay;
            numerator += evidence.getScore().doubleValue() * effectiveWeight;
            denominator += effectiveWeight;
        }
        if (denominator <= 0) {
            return null;
        }
        BigDecimal result = BigDecimal.valueOf(numerator / denominator).setScale(4, RoundingMode.HALF_UP);
        if (result.signum() < 0) {
            return BigDecimal.ZERO.setScale(4);
        }
        if (result.compareTo(ONE) > 0) {
            return ONE;
        }
        return result;
    }

    private String confidenceFor(int evidenceCount) {
        if (evidenceCount >= properties.getConfidenceHighMinEvidence()) {
            return Mastery.CONFIDENCE_HIGH;
        }
        if (evidenceCount >= properties.getConfidenceMediumMinEvidence()) {
            return Mastery.CONFIDENCE_MEDIUM;
        }
        return Mastery.CONFIDENCE_LOW;
    }
}
