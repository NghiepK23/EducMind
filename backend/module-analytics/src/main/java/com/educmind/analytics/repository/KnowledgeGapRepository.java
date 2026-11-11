package com.educmind.analytics.repository;

import com.educmind.analytics.entity.GapStatus;
import com.educmind.analytics.entity.KnowledgeGap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KnowledgeGapRepository extends JpaRepository<KnowledgeGap, UUID> {

    Optional<KnowledgeGap> findFirstByStudentIdAndObjectiveIdOrderByOpenedAtDesc(UUID studentId, UUID objectiveId);

    List<KnowledgeGap> findByStudentIdAndObjectiveId(UUID studentId, UUID objectiveId);

    List<KnowledgeGap> findByStudentIdInAndStatusOrderByOpenedAtDesc(Collection<UUID> studentIds, GapStatus status);

    List<KnowledgeGap> findByStudentIdInOrderByOpenedAtDesc(Collection<UUID> studentIds);
}
