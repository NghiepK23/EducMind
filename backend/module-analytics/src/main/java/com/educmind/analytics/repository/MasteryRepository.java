package com.educmind.analytics.repository;

import com.educmind.analytics.entity.Mastery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MasteryRepository extends JpaRepository<Mastery, UUID> {

    /** Tìm dòng mastery theo khóa unique (student, lesson, objective) — dùng cho upsert khi tính lại. */
    Optional<Mastery> findByStudentIdAndLessonIdAndLearningObjectiveId(
            UUID studentId, UUID lessonId, UUID learningObjectiveId);

    /** Mastery của 1 học sinh trong 1 lesson (mọi objective). */
    java.util.List<Mastery> findByStudentIdAndLessonId(UUID studentId, UUID lessonId);

    /** Mastery của 1 học sinh trong cả course (mọi lesson, mọi objective). */
    java.util.List<Mastery> findByStudentIdAndCourseId(UUID studentId, UUID courseId);
}
