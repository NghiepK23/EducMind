package com.educmind.analytics.dto;

import com.educmind.analytics.entity.Mastery;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Ma trận học sinh x learning objective cho 1 lesson (Module05 mục 5.2, không phân trang).
 * Mỗi StudentRow.cells có cùng thứ tự và độ dài với objectives; ô chưa có dữ liệu là null.
 */
public class MasteryMatrixResponse {

    public static class ObjectiveColumn {
        private final UUID id;
        private final String text;

        public ObjectiveColumn(UUID id, String text) {
            this.id = id;
            this.text = text;
        }

        public UUID getId() { return id; }
        public String getText() { return text; }
    }

    public static class Cell {
        private final UUID learningObjectiveId;
        private final BigDecimal masteryScore;
        private final Integer evidenceCount;
        private final String confidenceLevel;

        public Cell(Mastery m) {
            this.learningObjectiveId = m.getLearningObjectiveId();
            this.masteryScore = m.getMasteryScore();
            this.evidenceCount = m.getEvidenceCount();
            this.confidenceLevel = m.getConfidenceLevel();
        }

        public UUID getLearningObjectiveId() { return learningObjectiveId; }
        public BigDecimal getMasteryScore() { return masteryScore; }
        public Integer getEvidenceCount() { return evidenceCount; }
        public String getConfidenceLevel() { return confidenceLevel; }
    }

    public static class StudentRow {
        private final UUID studentId;
        private final List<Cell> cells;

        public StudentRow(UUID studentId, List<Cell> cells) {
            this.studentId = studentId;
            // không dùng List.copyOf vì cells có thể chứa null
            this.cells = Collections.unmodifiableList(new ArrayList<>(cells));
        }

        public UUID getStudentId() { return studentId; }
        public List<Cell> getCells() { return cells; }
    }

    private final UUID lessonId;
    private final UUID classId;
    private final List<ObjectiveColumn> objectives;
    private final List<StudentRow> students;

    public MasteryMatrixResponse(UUID lessonId, UUID classId,
                                 List<ObjectiveColumn> objectives, List<StudentRow> students) {
        this.lessonId = lessonId;
        this.classId = classId;
        this.objectives = List.copyOf(objectives);
        this.students = List.copyOf(students);
    }

    public UUID getLessonId() { return lessonId; }
    public UUID getClassId() { return classId; }
    public List<ObjectiveColumn> getObjectives() { return objectives; }
    public List<StudentRow> getStudents() { return students; }
}
