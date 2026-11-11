package com.educmind.analytics.service;

import com.educmind.analytics.entity.EvidenceSource;
import com.educmind.analytics.entity.GapStatus;
import com.educmind.analytics.entity.KnowledgeGap;
import com.educmind.analytics.entity.LearningEvidence;
import com.educmind.analytics.entity.Mastery;
import com.educmind.analytics.entity.Recommendation;
import com.educmind.analytics.entity.RecommendationStatus;
import com.educmind.analytics.entity.RecommendationType;
import com.educmind.analytics.repository.KnowledgeGapRepository;
import com.educmind.analytics.repository.LearningEvidenceRepository;
import com.educmind.analytics.repository.MasteryRepository;
import com.educmind.analytics.repository.RecommendationRepository;
import com.educmind.common.port.CourseContentPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Tính lại mastery / lỗ hổng kiến thức / gợi ý cho MỘT (học sinh, mục tiêu) mỗi khi có
 * bằng chứng mới (US-H01 AC-H01.1/02/03/04).
 *
 * Công thức mastery: trung bình cộng các dòng `learning_evidence` của mục tiêu đó.
 * Mỗi dòng đã là "kết quả tốt nhất của một nguồn" (unique theo student+objective+source)
 * nên trung bình không bị pha loãng bởi số lượt làm lại (AC-T14.2, AC-H01.6).
 *
 * Gọi hàm này trong CHÍNH transaction đang ghi bằng chứng để đọc được dòng vừa ghi.
 */
@Component
public class StudentAnalyticsUpdater {

    private static final Logger log = LoggerFactory.getLogger(StudentAnalyticsUpdater.class);

    private final LearningEvidenceRepository evidenceRepository;
    private final MasteryRepository masteryRepository;
    private final KnowledgeGapRepository gapRepository;
    private final RecommendationRepository recommendationRepository;
    private final CourseContentPort contentPort;
    private final AnalyticsSettings settings;

    public StudentAnalyticsUpdater(LearningEvidenceRepository evidenceRepository,
                                   MasteryRepository masteryRepository,
                                   KnowledgeGapRepository gapRepository,
                                   RecommendationRepository recommendationRepository,
                                   CourseContentPort contentPort,
                                   AnalyticsSettings settings) {
        this.evidenceRepository = evidenceRepository;
        this.masteryRepository = masteryRepository;
        this.gapRepository = gapRepository;
        this.recommendationRepository = recommendationRepository;
        this.contentPort = contentPort;
        this.settings = settings;
    }

    /**
     * AC-H01.5: lỗi khi phân tích một mục tiêu được ghi log và không làm hỏng các phần khác.
     * (Lỗi ở tầng DB vẫn có thể đánh dấu transaction rollback — xem MIGRATION-STATUS.)
     */
    @Transactional
    public void refresh(UUID studentId, UUID objectiveId) {
        try {
            refreshOrThrow(studentId, objectiveId);
        } catch (RuntimeException e) {
            log.error("Phân tích học tập thất bại (student={}, objective={}) — bỏ qua, AC-H01.5",
                    studentId, objectiveId, e);
        }
    }

    private void refreshOrThrow(UUID studentId, UUID objectiveId) {
        List<LearningEvidence> evidences = evidenceRepository
                .findByStudentIdAndObjectiveIdOrderByUpdatedAtAsc(studentId, objectiveId);
        Optional<Mastery> existingMastery = masteryRepository
                .findByStudentIdAndObjectiveId(studentId, objectiveId);

        if (evidences.isEmpty()) {
            existingMastery.ifPresent(masteryRepository::delete);
            expireRecommendations(studentId, objectiveId);
            resolveGapIfOpen(studentId, objectiveId);
            return;
        }

        double total = 0d;
        for (LearningEvidence evidence : evidences) {
            total += evidence.getScore() == null ? 0d : evidence.getScore().doubleValue();
        }
        double average = total / evidences.size();
        BigDecimal score = BigDecimal.valueOf(average)
                .setScale(3, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO)
                .min(BigDecimal.ONE);

        if (existingMastery.isPresent()) {
            existingMastery.get().update(score, evidences.size());
            masteryRepository.save(existingMastery.get());
        } else {
            masteryRepository.save(new Mastery(studentId, objectiveId, score, evidences.size()));
        }

        MasteryLevel level = MasteryLevel.of(average, settings);
        ObjectiveContext context = context(objectiveId);
        refreshGap(studentId, objectiveId, context, level, average, evidences);
        refreshRecommendations(studentId, objectiveId, context, level, average, evidences);
    }

    private void refreshGap(UUID studentId, UUID objectiveId, ObjectiveContext context, MasteryLevel level,
                            double average, List<LearningEvidence> evidences) {
        Optional<KnowledgeGap> maybeGap = gapRepository
                .findFirstByStudentIdAndObjectiveIdOrderByOpenedAtDesc(studentId, objectiveId);
        boolean shouldOpen = level == MasteryLevel.NEEDS_IMPROVEMENT
                && evidences.size() >= settings.gapMinEvidence();
        String reason = shouldOpen
                ? "Kết quả mục tiêu " + context.code() + " chỉ đạt " + percent(average) + "% qua "
                  + evidences.size() + " lần đánh giá (ngưỡng " + percent(settings.gapThreshold()) + "%)"
                : null;

        if (shouldOpen) {
            if (maybeGap.isEmpty()) {
                gapRepository.save(new KnowledgeGap(studentId, objectiveId, reason));
                return;
            }
            KnowledgeGap gap = maybeGap.get();
            switch (gap.getStatus()) {
                case OPEN -> {
                    gap.updateReason(reason);
                    gapRepository.save(gap);
                }
                case RESOLVED -> {
                    gap.reopen(reason);
                    gapRepository.save(gap);
                }
                case DISMISSED -> {
                    // AC-T08.5: chỉ mở lại khi có kết quả đánh giá mới hơn lần bỏ qua gần nhất.
                    // Trigger fn_block_reopen_dismissed_gap cũng kiểm tra đúng điều kiện này ở tầng DB.
                    OffsetDateTime lastDismissed = lastDismissedAt(studentId, objectiveId);
                    if (lastDismissed == null || hasEvidenceAfter(evidences, lastDismissed)) {
                        gap.reopen(reason);
                        gapRepository.save(gap);
                    }
                }
                default -> {
                    // không xảy ra với enum hiện tại
                }
            }
            return;
        }

        if (maybeGap.isPresent() && maybeGap.get().getStatus() == GapStatus.OPEN
                && average >= settings.gapThreshold()) {
            KnowledgeGap gap = maybeGap.get();
            gap.resolve(OffsetDateTime.now());
            gapRepository.save(gap);
        }
    }

    private void refreshRecommendations(UUID studentId, UUID objectiveId, ObjectiveContext context,
                                        MasteryLevel level, double average,
                                        List<LearningEvidence> evidences) {
        RecommendationType desired = switch (level) {
            case NEEDS_IMPROVEMENT -> RecommendationType.REVIEW_CONTENT;
            case IMPROVING -> RecommendationType.PRACTICE_TARGETED;
            case MASTERED -> RecommendationType.ADVANCED_ACTIVITY;
            case NO_DATA -> null;
        };

        List<Recommendation> existing = recommendationRepository
                .findByStudentIdAndObjectiveId(studentId, objectiveId);
        OffsetDateTime now = OffsetDateTime.now();
        for (Recommendation recommendation : existing) {
            // AC-H01.4 / AC-S06.4: gợi ý không còn phù hợp thì chuyển EXPIRED.
            if (recommendation.getStatus() == RecommendationStatus.ACTIVE
                    && (desired == null || recommendation.getType() != desired)) {
                recommendation.expire(now);
                recommendationRepository.save(recommendation);
            }
        }
        if (desired == null) {
            return;
        }

        String reason = reason(desired, context, average);
        String targetType = context.lessonId() == null ? null : "lesson";
        UUID targetId = context.lessonId();

        for (Recommendation recommendation : existing) {
            if (recommendation.getStatus() == RecommendationStatus.ACTIVE
                    && recommendation.getType() == desired) {
                recommendation.updateContent(reason, targetType, targetId);
                recommendationRepository.save(recommendation);
                return;
            }
        }
        recommendationRepository.save(new Recommendation(studentId, objectiveId, desired, reason,
                targetType, targetId));
    }

    private void expireRecommendations(UUID studentId, UUID objectiveId) {
        OffsetDateTime now = OffsetDateTime.now();
        for (Recommendation recommendation : recommendationRepository
                .findByStudentIdAndObjectiveId(studentId, objectiveId)) {
            if (recommendation.getStatus() == RecommendationStatus.ACTIVE) {
                recommendation.expire(now);
                recommendationRepository.save(recommendation);
            }
        }
    }

    private void resolveGapIfOpen(UUID studentId, UUID objectiveId) {
        gapRepository.findFirstByStudentIdAndObjectiveIdOrderByOpenedAtDesc(studentId, objectiveId)
                .filter(gap -> gap.getStatus() == GapStatus.OPEN)
                .ifPresent(gap -> {
                    gap.resolve(OffsetDateTime.now());
                    gapRepository.save(gap);
                });
    }

    private OffsetDateTime lastDismissedAt(UUID studentId, UUID objectiveId) {
        OffsetDateTime latest = null;
        for (KnowledgeGap gap : gapRepository.findByStudentIdAndObjectiveId(studentId, objectiveId)) {
            if (gap.getStatus() == GapStatus.DISMISSED && gap.getDismissedAt() != null
                    && (latest == null || gap.getDismissedAt().isAfter(latest))) {
                latest = gap.getDismissedAt();
            }
        }
        return latest;
    }

    private boolean hasEvidenceAfter(List<LearningEvidence> evidences, OffsetDateTime moment) {
        for (LearningEvidence evidence : evidences) {
            if (evidence.getUpdatedAt() != null && evidence.getUpdatedAt().isAfter(moment)) {
                return true;
            }
        }
        return false;
    }

    private String reason(RecommendationType type, ObjectiveContext context, double average) {
        String code = context.code();
        String lesson = context.lessonTitle() == null ? "bài học liên quan" : context.lessonTitle();
        return switch (type) {
            case REVIEW_CONTENT -> "Kết quả mục tiêu " + code + " còn thấp (" + percent(average)
                    + "%) — hãy ôn lại bài \"" + lesson + "\"";
            case PRACTICE_TARGETED -> "Bạn đang tiến bộ ở mục tiêu " + code + " (" + percent(average)
                    + "%) — luyện thêm bài tập của bài \"" + lesson + "\"";
            case ADVANCED_ACTIVITY -> "Bạn đã đạt mục tiêu " + code + " (" + percent(average)
                    + "%) — thử thách với nội dung nâng cao của bài \"" + lesson + "\"";
            case REATTEMPT_ASSESSMENT -> "Hãy làm lại bài kiểm tra để cải thiện mục tiêu " + code;
        };
    }

    private int percent(double value) {
        return (int) Math.round(value * 100);
    }

    private ObjectiveContext context(UUID objectiveId) {
        for (CourseContentPort.ObjectiveInfo info : contentPort.findObjectiveInfos(List.of(objectiveId))) {
            return new ObjectiveContext(info.code(), info.lessonId(), info.lessonTitle());
        }
        return new ObjectiveContext("mục tiêu", null, null);
    }

    /** Ngữ cảnh hiển thị của mục tiêu (dùng cho lý do gợi ý + liên kết bài học). */
    private record ObjectiveContext(String code, UUID lessonId, String lessonTitle) {
    }

    /** Dùng cho nơi khác cần biết source type của bằng chứng. */
    static boolean isAssessment(LearningEvidence evidence) {
        return evidence.getSourceType() == EvidenceSource.ASSESSMENT;
    }
}
