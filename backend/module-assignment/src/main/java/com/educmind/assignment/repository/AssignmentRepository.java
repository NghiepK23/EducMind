package com.educmind.assignment.repository;

import com.educmind.assignment.entity.Assignment;
import com.educmind.assignment.entity.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    Optional<Assignment> findByIdAndDeletedAtIsNull(UUID id);

    List<Assignment> findByCourseIdAndDeletedAtIsNullOrderByDueDateAsc(UUID courseId);

    List<Assignment> findByCourseIdAndStatusAndDeletedAtIsNullOrderByDueDateAsc(UUID courseId, AssignmentStatus status);
}
