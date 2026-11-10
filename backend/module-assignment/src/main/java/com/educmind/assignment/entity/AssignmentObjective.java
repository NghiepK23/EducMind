package com.educmind.assignment.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/** Bảng assignment_objectives (Spec v2 — bảng 11; AC-T03.6, AC-T04.3). */
@Entity
@Table(name = "assignment_objectives")
public class AssignmentObjective {

    @EmbeddedId
    private AssignmentObjectiveId id;

    protected AssignmentObjective() {
    }

    public AssignmentObjective(UUID assignmentId, UUID objectiveId) {
        this.id = new AssignmentObjectiveId(assignmentId, objectiveId);
    }

    public AssignmentObjectiveId getId() {
        return id;
    }

    public UUID getAssignmentId() {
        return id.getAssignmentId();
    }

    public UUID getObjectiveId() {
        return id.getObjectiveId();
    }
}
