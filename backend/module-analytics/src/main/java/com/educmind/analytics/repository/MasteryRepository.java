package com.educmind.analytics.repository;

import com.educmind.analytics.entity.Mastery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MasteryRepository extends JpaRepository<Mastery, UUID> {

    /** Dòng tổng hợp theo (lesson, objective) cho mastery-overview. */
    interface OverviewRow {
        UUID getLessonId();
        UUID getLearningObjectiveId();
        Long getStudentCount();
        Double getAvgMastery();
        Long getLowCount();
        Long getMediumCount();
        Long getHighCount();
    }

    String OVERVIEW_SELECT =
            "SELECT m.lessonId AS lessonId, m.learningObjectiveId AS learningObjectiveId, "
          + "COUNT(m) AS studentCount, AVG(m.masteryScore) AS avgMastery, "
          + "SUM(CASE WHEN m.confidenceLevel = 'LOW' THEN 1 ELSE 0 END) AS lowCount, "
          + "SUM(CASE WHEN m.confidenceLevel = 'MEDIUM' THEN 1 ELSE 0 END) AS mediumCount, "
          + "SUM(CASE WHEN m.confidenceLevel = 'HIGH' THEN 1 ELSE 0 END) AS highCount "
          + "FROM Mastery m WHERE m.courseId = :courseId ";

    /** Tổng hợp mastery cả course (mastery-overview, không truyền class_id). */
    @Query(OVERVIEW_SELECT + "GROUP BY m.lessonId, m.learningObjectiveId")
    java.util.List<OverviewRow> summarizeByCourse(@Param("courseId") UUID courseId);

    /** Tổng hợp mastery của 1 nhóm học sinh trong course (mastery-overview, có class_id). */
    @Query(OVERVIEW_SELECT + "AND m.studentId IN :studentIds GROUP BY m.lessonId, m.learningObjectiveId")
    java.util.List<OverviewRow> summarizeByCourseAndStudents(
            @Param("courseId") UUID courseId,
            @Param("studentIds") java.util.Collection<UUID> studentIds);

    /** Tìm dòng mastery theo khóa unique (student, lesson, objective) — dùng cho upsert khi tính lại. */
    Optional<Mastery> findByStudentIdAndLessonIdAndLearningObjectiveId(
            UUID studentId, UUID lessonId, UUID learningObjectiveId);

    /** Mastery của 1 học sinh trong 1 lesson (mọi objective). */
    java.util.List<Mastery> findByStudentIdAndLessonId(UUID studentId, UUID lessonId);

    /** Mastery của 1 học sinh trong cả course (mọi lesson, mọi objective). */
    java.util.List<Mastery> findByStudentIdAndCourseId(UUID studentId, UUID courseId);

    /** Mastery của nhiều học sinh trong 1 lesson (1 query, dùng cho mastery-matrix). */
    java.util.List<Mastery> findByLessonIdAndStudentIdIn(UUID lessonId, java.util.Collection<UUID> studentIds);

    /** Dòng tổng hợp theo course cho API SCHOOL_ADMIN (không có thông tin từng học sinh). */
    interface CourseSummaryRow {
        UUID getCourseId();
        Long getStudentCount();
        Double getAvgMastery();
    }

    /** Caller phải đảm bảo courseIds không rỗng. avgMastery = AVG trên các dòng mastery của course. */
    @Query("SELECT m.courseId AS courseId, COUNT(DISTINCT m.studentId) AS studentCount, "
         + "AVG(m.masteryScore) AS avgMastery "
         + "FROM Mastery m WHERE m.courseId IN :courseIds GROUP BY m.courseId")
    java.util.List<CourseSummaryRow> summarizeBySchoolCourses(
            @Param("courseIds") java.util.Collection<UUID> courseIds);
}
