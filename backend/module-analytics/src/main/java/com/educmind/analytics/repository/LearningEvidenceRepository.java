package com.educmind.analytics.repository;

import com.educmind.analytics.entity.EvidenceSource;
import com.educmind.analytics.entity.LearningEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LearningEvidenceRepository extends JpaRepository<LearningEvidence, UUID> {

    Optional<LearningEvidence> findByStudentIdAndObjectiveIdAndSourceTypeAndSourceId(
            UUID studentId, UUID objectiveId, EvidenceSource sourceType, UUID sourceId);

    List<LearningEvidence> findByStudentIdOrderByObjectiveIdAsc(UUID studentId);

    List<LearningEvidence> findBySourceTypeAndSourceId(EvidenceSource sourceType, UUID sourceId);
}
