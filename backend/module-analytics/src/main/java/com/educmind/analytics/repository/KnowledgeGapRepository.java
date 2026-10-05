package com.educmind.analytics.repository;

import com.educmind.analytics.entity.KnowledgeGap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KnowledgeGapRepository extends JpaRepository<KnowledgeGap, UUID> {

    /**
     * Gap đang OPEN của 1 (student, lesson, objective). Tối đa 1 dòng nhờ partial unique index
     * uq_knowledge_gap_open ở DB.
     */
    Optional<KnowledgeGap> findByStudentIdAndLessonIdAndLearningObjectiveIdAndStatus(
            UUID studentId, UUID lessonId, UUID learningObjectiveId, String status);

    Page<KnowledgeGap> findByStudentIdAndCourseId(UUID studentId, UUID courseId, Pageable pageable);

    Page<KnowledgeGap> findByStudentIdAndCourseIdAndStatus(
            UUID studentId, UUID courseId, String status, Pageable pageable);

    // ---- API giao vien: truy van theo course (va tuy chon theo tap hoc sinh cua lop) ----

    Page<KnowledgeGap> findByCourseId(UUID courseId, Pageable pageable);

    Page<KnowledgeGap> findByCourseIdAndStatus(UUID courseId, String status, Pageable pageable);

    /** Caller phai dam bao studentIds khong rong (IN () rong khong an toan voi Hibernate). */
    Page<KnowledgeGap> findByCourseIdAndStudentIdIn(
            UUID courseId, Collection<UUID> studentIds, Pageable pageable);

    Page<KnowledgeGap> findByCourseIdAndStudentIdInAndStatus(
            UUID courseId, Collection<UUID> studentIds, String status, Pageable pageable);

    /** Dòng đếm gap OPEN theo course cho API SCHOOL_ADMIN (không có thông tin từng học sinh). */
    interface CourseOpenGapRow {
        UUID getCourseId();
        Long getOpenCount();
    }

    /** Caller phải đảm bảo courseIds không rỗng. Course không có gap OPEN sẽ không có dòng nào. */
    @Query("SELECT g.courseId AS courseId, COUNT(g) AS openCount "
         + "FROM KnowledgeGap g WHERE g.courseId IN :courseIds AND g.status = 'OPEN' "
         + "GROUP BY g.courseId")
    List<CourseOpenGapRow> countOpenByCourseIds(@Param("courseIds") Collection<UUID> courseIds);
}
