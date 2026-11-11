package com.educmind.assessment.repository;

import com.educmind.assessment.entity.Assessment;
import com.educmind.assessment.entity.AssessmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {

    Optional<Assessment> findByIdAndDeletedAtIsNull(UUID id);

    List<Assessment> findByCourseIdAndDeletedAtIsNullOrderByOpenAtDesc(UUID courseId);

    List<Assessment> findByCourseIdAndStatusAndDeletedAtIsNullOrderByOpenAtDesc(
            UUID courseId, AssessmentStatus status);
}
