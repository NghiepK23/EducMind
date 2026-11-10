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
 * được chấm xong (US-S03 AC-S03.5, AC-S03.6).
 *
 * Phase 3 sẽ dùng chính bảng này để tính mastery / lỗ hổng kiến thức.
 */
@Component
public class LearningEvidencePortImpl implements LearningEvidencePort {

    private final LearningEvidenceRepository repository;

    public LearningEvidencePortImpl(LearningEvidenceRepository repository) {
        this.repository = repository;
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
            repository.save(new LearningEvidence(studentId, objectiveId, source, sourceId, normalized));
            return;
        }
        if (existing.get().getScore() == null || normalized.compareTo(existing.get().getScore()) > 0) {
            existing.get().updateScore(normalized);
            repository.save(existing.get());
        }
    }

    private BigDecimal normalize(double score) {
        return BigDecimal.valueOf(score)
                .setScale(3, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO)
                .min(BigDecimal.ONE);
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
            repository.save(existing.get());
            return;
        }
        repository.save(new LearningEvidence(studentId, objectiveId, source, sourceId, normalized));
    }
}
