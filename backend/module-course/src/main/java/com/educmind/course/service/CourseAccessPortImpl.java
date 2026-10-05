package com.educmind.course.service;

import com.educmind.common.port.CourseAccessPort;
import com.educmind.course.entity.Course;
import com.educmind.course.repository.CourseMemberRepository;
import com.educmind.course.repository.CourseRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation thật của CourseAccessPort (định nghĩa ở educmind-common).
 * Đây là "adapter" trong kiến trúc Ports-and-Adapters — nơi duy nhất trong
 * toàn hệ thống được phép truy vấn trực tiếp Course + CourseMember, vì cả
 * 2 entity đều thuộc module này.
 *
 * [ĐÃ VIẾT LẠI — THAY ĐỔI KIẾN TRÚC LỚN] Trước đây phải join qua CourseClass +
 * ClassAccessPort (vòng lặp qua nhiều classId trung gian). Model mới đơn giản
 * hơn hẳn: course tự có course_members riêng, chỉ cần query trực tiếp trên
 * đúng courseId đang xét — không còn ClassAccessPort ở đây nữa.
 *
 * Chữ ký các method trong CourseAccessPort (interface ở educmind-common) GIỮ
 * NGUYÊN không đổi — chỉ cách hiện thực bên trong thay đổi, đúng tinh thần
 * Ports-and-Adapters (interface mô tả "làm gì", implementation mô tả "làm thế
 * nào") — các module khác (module-assignment, module-assessment...) đã
 * constructor-inject CourseAccessPort không cần sửa gì.
 *
 * Spring sẽ tự động đăng ký bean này và "nối dây" cho bất kỳ module nào
 * khai báo constructor-inject CourseAccessPort.
 */
@Component
public class CourseAccessPortImpl implements CourseAccessPort {

    private static final String ROLE_TEACHER = "TEACHER";
    private static final String ROLE_STUDENT = "STUDENT";

    private final CourseRepository courseRepository;
    private final CourseMemberRepository courseMemberRepository;

    public CourseAccessPortImpl(
            CourseRepository courseRepository,
            CourseMemberRepository courseMemberRepository
    ) {
        this.courseRepository = courseRepository;
        this.courseMemberRepository = courseMemberRepository;
    }

    @Override
    public boolean isOwnerOrCoTeacher(UUID actorId, UUID courseId) {
        Optional<Course> course = courseRepository.findByIdAndDeletedAtIsNull(courseId);
        if (course.isEmpty()) {
            return false;
        }

        if (course.get().getCreatedBy().equals(actorId)) {
            return true;
        }

        return courseMemberRepository
                .existsByCourseIdAndUserIdAndMemberRoleAndLeftAtIsNull(courseId, actorId, ROLE_TEACHER);
    }

    @Override
    public boolean isOwner(UUID actorId, UUID courseId) {
        return courseRepository.findByIdAndDeletedAtIsNull(courseId)
                .map(c -> c.getCreatedBy().equals(actorId))
                .orElse(false);
    }

    @Override
    public boolean isStudentEnrolled(UUID studentId, UUID courseId) {
        Optional<Course> course = courseRepository.findByIdAndDeletedAtIsNull(courseId);
        if (course.isEmpty()) {
            return false;
        }

        return courseMemberRepository
                .existsByCourseIdAndUserIdAndMemberRoleAndLeftAtIsNull(courseId, studentId, ROLE_STUDENT);
    }

    @Override
    public Optional<UUID> findSchoolIdByCourseId(UUID courseId) {
        return courseRepository.findByIdAndDeletedAtIsNull(courseId)
                .map(Course::getSchoolId);
    }

    @Override
    public List<UUID> findAccessibleCourseIdsForTeacher(UUID teacherId) {
        Map<UUID, UUID> ids = new LinkedHashMap<>();

        for (Course c : courseRepository.findByCreatedByAndDeletedAtIsNull(teacherId)) {
            ids.put(c.getId(), c.getId());
        }

        List<UUID> coTaughtCourseIds = courseMemberRepository
                .findCourseIdsByUserIdAndMemberRoleAndLeftAtIsNull(teacherId, ROLE_TEACHER);
        if (!coTaughtCourseIds.isEmpty()) {
            for (Course c : courseRepository.findAllById(coTaughtCourseIds)) {
                if (c.getDeletedAt() == null) {
                    ids.put(c.getId(), c.getId());
                }
            }
        }

        return List.copyOf(ids.values());
    }

    @Override
    public Optional<UUID> findSourceClassId(UUID courseId, UUID studentId) {
        return courseMemberRepository.findByCourseIdAndUserId(courseId, studentId)
                .filter(cm -> ROLE_STUDENT.equals(cm.getMemberRole()))
                .map(cm -> cm.getSourceClassId());
    }

    @Override
    public List<UUID> findActiveStudentIdsByCourseAndSourceClass(UUID courseId, UUID classId) {
        return courseMemberRepository.findActiveStudentIdsByCourseIdAndSourceClassId(courseId, classId);
    }

    @Override
    public List<UUID> findCourseIdsBySchoolId(UUID schoolId) {
        return courseRepository.findBySchoolIdAndDeletedAtIsNull(schoolId).stream()
                .map(Course::getId)
                .toList();
    }
}
