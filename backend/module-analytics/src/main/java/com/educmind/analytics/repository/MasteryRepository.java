package com.educmind.analytics.repository;

import com.educmind.analytics.entity.Mastery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MasteryRepository extends JpaRepository<Mastery, UUID> {

    Optional<Mastery> findByStudentIdAndObjectiveId(UUID studentId, UUID objectiveId);

    List<Mastery> findByStudentIdIn(Collection<UUID> studentIds);

    List<Mastery> findByStudentId(UUID studentId);
}
