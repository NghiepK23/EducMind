package com.educmind.course.repository;

import com.educmind.course.entity.CourseMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository cho entity {@link CourseMember} — thay thế hoàn toàn vai trò của
 * CourseClassRepository cũ. Đây là bảng trung tâm cho toàn bộ authorization
 * của Course từ nay: KHÔNG còn join qua course_class + ClassAccessPort, mà
 * kiểm tra trực tiếp actor có phải thành viên active của ĐÚNG course đó không.
 */
public interface CourseMemberRepository extends JpaRepository<CourseMember, UUID> {

    /**
     * Kiểm tra userId có đang là thành viên ACTIVE (leftAt IS NULL) với đúng
     * memberRole trong 1 course cụ thể hay không — dùng cho:
     *   - CourseAuthorizationHelper.assertCanManage(): kiểm tra TEACHER
     *   - CourseAuthorizationHelper.isStudentEnrolledInCourse(): kiểm tra STUDENT
     */
    boolean existsByCourseIdAndUserIdAndMemberRoleAndLeftAtIsNull(
            UUID courseId, UUID userId, String memberRole);

    /**
     * Danh sách courseId mà userId đang là thành viên ACTIVE với đúng memberRole —
     * dùng cho CourseService.listCourses() (TEACHER thấy course mình đồng giảng dạy)
     * và CourseAccessPortImpl.findAccessibleCourseIdsForTeacher().
     */
    @Query("SELECT cm.courseId FROM CourseMember cm "
            + "WHERE cm.userId = :userId AND cm.memberRole = :memberRole AND cm.leftAt IS NULL")
    List<UUID> findCourseIdsByUserIdAndMemberRoleAndLeftAtIsNull(
            @Param("userId") UUID userId, @Param("memberRole") String memberRole);

    /**
     * Tìm dòng course_member theo cặp (courseId, userId), KHÔNG lọc theo leftAt —
     * dùng cho luồng re-add: cần tìm lại dòng cũ dù đang active hay đã bị gỡ, để
     * UPDATE (set leftAt = null) thay vì tạo dòng mới (tránh vi phạm UNIQUE constraint),
     * tương tự pattern findByCourseIdAndClassId cũ của CourseClassRepository.
     */
    Optional<CourseMember> findByCourseIdAndUserId(UUID courseId, UUID userId);

    /**
     * Kiểm tra 1 cặp (courseId, userId) có đang là thành viên ACTIVE hay không
     * (bất kể memberRole) — dùng để chặn thêm trùng 1 người đã có mặt trong course.
     */
    boolean existsByCourseIdAndUserIdAndLeftAtIsNull(UUID courseId, UUID userId);

    /** Danh sách thành viên đang active (leftAt IS NULL) của 1 course — dùng cho listMembers. */
    List<CourseMember> findByCourseIdAndLeftAtIsNullOrderByAddedAtAsc(UUID courseId);
}
