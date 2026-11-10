package com.educmind.schoolclass.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Bảng class_members (Spec v2, mục C — bảng 3). Chỉ chứa học sinh. */
@Entity
@Table(name = "class_members")
public class ClassMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    protected ClassMember() {
    }

    public ClassMember(UUID classId, UUID studentId) {
        this.classId = classId;
        this.studentId = studentId;
    }

    @PrePersist
    void onCreate() {
        if (joinedAt == null) {
            joinedAt = OffsetDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getClassId() {
        return classId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }
}
