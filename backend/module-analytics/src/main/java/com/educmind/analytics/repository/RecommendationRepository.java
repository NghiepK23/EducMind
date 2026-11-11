package com.educmind.analytics.repository;

import com.educmind.analytics.entity.Recommendation;
import com.educmind.analytics.entity.RecommendationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

    List<Recommendation> findByStudentIdAndStatusOrderByCreatedAtDesc(UUID studentId, RecommendationStatus status);

    List<Recommendation> findByStudentIdOrderByCreatedAtDesc(UUID studentId);

    List<Recommendation> findByStudentIdAndObjectiveId(UUID studentId, UUID objectiveId);

    Optional<Recommendation> findByIdAndStudentId(UUID id, UUID studentId);
}
