package com.educmind.assignment.repository;

import com.educmind.assignment.entity.AssignmentObjective;
import com.educmind.assignment.entity.AssignmentObjectiveId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssignmentObjectiveRepository extends JpaRepository<AssignmentObjective, AssignmentObjectiveId> {

    List<AssignmentObjective> findByIdAssignmentId(UUID assignmentId);

    void deleteByIdAssignmentId(UUID assignmentId);

    boolean existsByIdAssignmentIdAndIdObjectiveId(UUID assignmentId, UUID objectiveId);

    List<AssignmentObjective> findByIdObjectiveId(UUID objectiveId);
}
