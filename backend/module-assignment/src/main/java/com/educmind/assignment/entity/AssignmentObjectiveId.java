package com.educmind.assignment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Khóa chính ghép của bảng assignment_objectives (assignment_id, objective_id). */
@Embeddable
public class AssignmentObjectiveId implements Serializable {

    @Column(name = "assignment_id")
    private UUID assignmentId;

    @Column(name = "objective_id")
    private UUID objectiveId;

    protected AssignmentObjectiveId() {
    }

    public AssignmentObjectiveId(UUID assignmentId, UUID objectiveId) {
        this.assignmentId = assignmentId;
        this.objectiveId = objectiveId;
    }

    public UUID getAssignmentId() {
        return assignmentId;
    }

    public UUID getObjectiveId() {
        return objectiveId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AssignmentObjectiveId that)) {
            return false;
        }
        return Objects.equals(assignmentId, that.assignmentId) && Objects.equals(objectiveId, that.objectiveId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignmentId, objectiveId);
    }
}
