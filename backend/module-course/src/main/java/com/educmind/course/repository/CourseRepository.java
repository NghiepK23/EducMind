package com.educmind.course.repository;

import com.educmind.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CRUD cơ bản cho `courses` — Module03_Course_Lesson_Resource_FINAL.md, mục C.6.
 * Mọi phương thức list/get ở đây đều loại trừ course đã soft-delete (deletedAt IS NULL),
 * cùng lý do như SchoolClassRepository: ddl-auto=validate, không dùng @SQLRestriction,
 * nên phải tự lọc thủ công ở tầng query.
 *
 * Logic "owner HOẶC đồng giảng dạy" (kết hợp course_class + ClassAccessPort) KHÔNG
 * đặt ở đây — nằm ở CourseService, vì cần gọi cross-module port (không viết được
 * bằng JPQL thuần trong module-course).
 */
public interface CourseRepository extends JpaRepository<Course, UUID> {

    List<Course> findBySchoolId(UUID schoolId);

    List<Course> findByCreatedBy(UUID createdBy);

    /** SCHOOL_ADMIN: mọi course còn hoạt động trong trường mình. */
    List<Course> findBySchoolIdAndDeletedAtIsNull(UUID schoolId);

    /** TEACHER: course do chính mình tạo (owner gốc), chưa xét đồng giảng dạy. */
    List<Course> findByCreatedByAndDeletedAtIsNull(UUID createdBy);

    /**
     * Dùng cho GET/{id}, PATCH/{id}, DELETE/{id} — luôn lọc deletedAt IS NULL,
     * nếu không sẽ vô tình cho phép xem/sửa lại 1 course đã bị soft-delete trước đó.
     */
    Optional<Course> findByIdAndDeletedAtIsNull(UUID id);

    /**
     * Dùng khi xóa 1 Subject (SubjectService.deleteSubject) — chặn xóa nếu còn Course
     * đang hoạt động tham chiếu subjectId đó, theo quyết định "chặn xóa nếu còn Course dùng".
     */
    boolean existsBySubjectIdAndDeletedAtIsNull(java.util.UUID subjectId);
}
