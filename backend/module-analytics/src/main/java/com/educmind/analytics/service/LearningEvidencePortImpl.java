package com.educmind.analytics.service;

import com.educmind.analytics.entity.EvidenceSource;
import com.educmind.analytics.entity.LearningEvidence;
import com.educmind.analytics.repository.LearningEvidenceRepository;
import com.educmind.common.port.LearningEvidencePort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

/**
 * Ghi bằng chứng học tập khi giáo viên chấm bài (US-T04 AC-T04.3) và khi bài kiểm tra
 * được chấm xong (US-S03 AC-S03.5/6/7).
 *
 * Mỗi lần ghi đều gọi {@link StudentAnalyticsUpdater} để cập nhật mastery / lỗ hổng / gợi ý
 * ngay trong cùng transaction (US-H01 AC-H01.1).
 */
@Component
public class LearningEvidencePortImpl implements LearningEvidencePort {

    private final LearningEvidenceRepository repository;
    private final StudentAnalyticsUpdater analyticsUpdater;

    public LearningEvidencePortImpl(LearningEvidenceRepository repository,
                                    StudentAnalyticsUpdater analyticsUpdater) {
        this.repository = repository;
        this.analyticsUpdater = analyticsUpdater;
    }

    @Override
    @Transactional
    public void recordHighest(UUID studentId, UUID objectiveId, SourceType sourceType, UUID sourceId,
                              double score) {
        BigDecimal normalized = normalize(score);
        EvidenceSource source = EvidenceSource.valueOf(sourceType.name());
        Optional<LearningEvidence> existing = repository
                .findByStudentIdAndObjectiveIdAndSourceTypeAndSourceId(studentId, objectiveId, source, sourceId);

        if (existing.isEmpty()) {
            repository.saveAndFlush(new LearningEvidence(studentId, objectiveId, source, sourceId, normalized));
        } else if (existing.get().getScore() == null
                || normalized.compareTo(existing.get().getScore()) > 0) {
            existing.get().updateScore(normalized);
            repository.saveAndFlush(existing.get());
        }
        analyticsUpdater.refresh(studentId, objectiveId);
    }

    @Override
    @Transactional
    public void record(UUID studentId, UUID objectiveId, SourceType sourceType, UUID sourceId, double score) {
        BigDecimal normalized = normalize(score);
        EvidenceSource source = EvidenceSource.valueOf(sourceType.name());

        Optional<LearningEvidence> existing = repository
                .findByStudentIdAndObjectiveIdAndSourceTypeAndSourceId(studentId, objectiveId, source, sourceId);

        if (existing.isPresent()) {
            existing.get().updateScore(normalized);
            repository.saveAndFlush(existing.get());
        } else {
            repository.saveAndFlush(new LearningEvidence(studentId, objectiveId, source, sourceId, normalized));
        }
        analyticsUpdater.refresh(studentId, objectiveId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasEvidenceForObjective(UUID objectiveId) {
        return repository.existsByObjectiveId(objectiveId);
    }

    private BigDecimal normalize(double score) {
        return BigDecimal.valueOf(score)
                .setScale(3, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO)
                .min(BigDecimal.ONE);
    }
}
