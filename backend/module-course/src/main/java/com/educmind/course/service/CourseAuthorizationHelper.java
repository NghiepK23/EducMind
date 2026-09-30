package com.educmind.course.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.UserLookupPort;
import com.educmind.course.entity.Course;
import com.educmind.course.repository.CourseMemberRepository;
import com.educmind.course.repository.CourseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Logic authorization DÙNG CHUNG cho mọi service thao tác trên Course (CourseService,
 * LessonService, và sau này ResourceService).
 *
 * [ĐÃ VIẾT LẠI — THAY ĐỔI KIẾN TRÚC LỚN] Trước đây "đồng giảng dạy" được suy ra gián
 * tiếp qua course_class (course N-N với nhiều class hành chính) + ClassAccessPort —
 * cách này có lỗ hổng: 1 giáo viên hoàn toàn không liên quan tới course cũng có thể vô
 * tình được coi là đồng giảng dạy, chỉ vì lớp họ dạy tình cờ cũng học chung course đó.
 *
 * Model MỚI đơn giản hơn nhiều: mỗi course tự có danh sách thành viên riêng
 * (course_members), không dùng chung class_members nữa. "Đồng giảng dạy" giờ nghĩa là:
 * có 1 dòng ACTIVE (leftAt IS NULL) trong course_members với memberRole='TEACHER' cho
 * ĐÚNG course đó — không còn cascade qua nhiều class trung gian. Việc owner thêm 1 giáo
 * viên khác vào course_members CHÍNH LÀ hành động "cấp quyền đồng giảng dạy", tường minh,
 * không còn suy luận gián tiếp.
 *
 * Do đó KHÔNG còn cần ClassAccessPort/CourseClassRepository ở đây nữa — chỉ cần
 * CourseMemberRepository, query trực tiếp trên đúng course đang xét.
 *
 * KHÔNG chứa loadActor() — việc load UserSummary từ actorId vẫn do từng Service tự gọi
 * UserLookupPort riêng, vì đây không phải logic "về Course" mà là logic "về actor".
 */
@Service
public class CourseAuthorizationHelper {

    private static final String ROLE_TEACHER = "TEACHER";
    private static final String ROLE_STUDENT = "STUDENT";

    private final CourseRepository courseRepository;
    private final CourseMemberRepository courseMemberRepository;

    public CourseAuthorizationHelper(CourseRepository courseRepository,
                                      CourseMemberRepository courseMemberRepository) {
        this.courseRepository = courseRepository;
        this.courseMemberRepository = courseMemberRepository;
    }

    public Course loadCourseOrThrow(UUID courseId) {
        return courseRepository.findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new BusinessException(
                        "COURSE_NOT_FOUND",
                        "Không tìm thấy khóa học",
                        HttpStatus.NOT_FOUND
                ));
    }

    public void assertCanView(UserLookupPort.UserSummary actor, Course course) {
        if ("SCHOOL_ADMIN".equals(actor.role())) {
            if (!course.getSchoolId().equals(actor.schoolId())) {
                throw forbidden();
            }
            return;
        }
        assertCanManage(actor, course);
    }

    /**
     * Owner (created_by) HOẶC đồng giảng dạy: actor có 1 dòng ACTIVE trong
     * course_members với memberRole='TEACHER' cho đúng course này.
     */
    public void assertCanManage(UserLookupPort.UserSummary actor, Course course) {
        if (course.getCreatedBy().equals(actor.id())) {
            return;
        }

        boolean isCoTeacher = courseMemberRepository
                .existsByCourseIdAndUserIdAndMemberRoleAndLeftAtIsNull(
                        course.getId(), actor.id(), ROLE_TEACHER);
        if (!isCoTeacher) {
            throw forbidden();
        }
    }

    /**
     * true nếu studentId đang có 1 dòng ACTIVE trong course_members với
     * memberRole='STUDENT' cho đúng course này — dùng để quyết định STUDENT
     * có được xem course/lesson của course đó hay không.
     */
    public boolean isStudentEnrolledInCourse(UUID studentId, Course course) {
        return courseMemberRepository
                .existsByCourseIdAndUserIdAndMemberRoleAndLeftAtIsNull(
                        course.getId(), studentId, ROLE_STUDENT);
    }

    /** Dùng riêng cho deleteCourse (CourseService) — KHÔNG mở rộng cho đồng giảng dạy. */
    public void assertIsOwner(UserLookupPort.UserSummary actor, Course course) {
        if (!course.getCreatedBy().equals(actor.id())) {
            throw forbidden();
        }
    }

    public BusinessException forbidden() {
        return new BusinessException(
                "NOT_COURSE_OWNER",
                "Bạn không có quyền truy cập khóa học này",
                HttpStatus.FORBIDDEN
        );
    }
}
