package com.educmind.common.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port kiểm tra quyền trên khóa học mà không lộ Entity của module-course.
 *
 * Spec v2: courses.class_id trỏ tới lớp; quyền quản lý khóa học = quyền của giáo viên
 * phụ trách lớp. Học sinh "thuộc" khóa học nếu thuộc lớp của khóa học đó.
 */
public interface CourseAccessPort {

    /** true nếu course chưa xóa và actorId là giáo viên phụ trách lớp của course. */
    boolean isOwner(UUID actorId, UUID courseId);

    /** Như {@link #isOwner} — giữ tên cũ cho nơi gọi đọc dễ hiểu (Spec v2 không có đồng giảng dạy). */
    default boolean isOwnerOrCoTeacher(UUID actorId, UUID courseId) {
        return isOwner(actorId, courseId);
    }

    /** true nếu studentId thuộc lớp của course (và course chưa xóa). */
    boolean isStudentEnrolled(UUID studentId, UUID courseId);

    /** class_id của course chưa xóa; empty nếu course không tồn tại. */
    Optional<UUID> findClassId(UUID courseId);

    /** Danh sách course chưa xóa mà giáo viên phụ trách. */
    List<UUID> findAccessibleCourseIdsForTeacher(UUID teacherId);

    /** Danh sách học sinh thuộc lớp của course. */
    List<UUID> findStudentIdsByCourse(UUID courseId);
}
