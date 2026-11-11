package com.educmind.course.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.ClassAccessPort;
import com.educmind.course.dto.CreateCourseRequest;
import com.educmind.course.dto.CourseResponse;
import com.educmind.course.dto.UpdateCourseRequest;
import com.educmind.course.entity.Course;
import com.educmind.course.repository.CourseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** US-T01 (tạo khóa học) + AC-T01.1/2 (chỉ lớp mình phụ trách, học sinh lớp khác không xem được). */
@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final ClassAccessPort classAccessPort;
    private final CoursePermission permission;

    public CourseService(CourseRepository courseRepository,
                         ClassAccessPort classAccessPort,
                         CoursePermission permission) {
        this.courseRepository = courseRepository;
        this.classAccessPort = classAccessPort;
        this.permission = permission;
    }

    @Transactional
    public CourseResponse create(UUID teacherId, UUID classId, CreateCourseRequest request) {
        if (!classAccessPort.isOwner(teacherId, classId)) {
            throw new BusinessException("NOT_CLASS_OWNER",
                    "Bạn không phụ trách lớp này", HttpStatus.FORBIDDEN);
        }
        Course saved = courseRepository.save(new Course(
                classId, request.getTitle().trim(), request.getDescription()));
        return CourseResponse.from(saved);
    }

    /** Giáo viên: khóa học của lớp mình; học sinh: khóa học của lớp mình đang thuộc. */
    @Transactional(readOnly = true)
    public List<CourseResponse> listForClass(UUID actorId, String role, UUID classId) {
        boolean allowed = CoursePermission.isTeacher(role)
                ? classAccessPort.isOwner(actorId, classId)
                : classAccessPort.isMember(actorId, classId);

        if (!allowed) {
            throw new BusinessException("CLASS_ACCESS_DENIED",
                    "Bạn không có quyền truy cập lớp này", HttpStatus.FORBIDDEN);
        }

        List<CourseResponse> result = new ArrayList<>();
        for (Course c : courseRepository.findByClassIdAndDeletedAtIsNullOrderByCreatedAtAsc(classId)) {
            result.add(CourseResponse.from(c));
        }
        return result;
    }

    /** Danh sách khóa học của chính người dùng (dùng cho trang chủ). */
    @Transactional(readOnly = true)
    public List<CourseResponse> listMine(UUID actorId, String role) {
        List<UUID> classIds = CoursePermission.isTeacher(role)
                ? classAccessPort.findClassIdsForTeacher(actorId)
                : classAccessPort.findClassIdsForStudent(actorId);

        List<CourseResponse> result = new ArrayList<>();
        for (UUID classId : classIds) {
            for (Course c : courseRepository.findByClassIdAndDeletedAtIsNullOrderByCreatedAtAsc(classId)) {
                result.add(CourseResponse.from(c));
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public CourseResponse get(UUID actorId, String role, UUID courseId) {
        return CourseResponse.from(permission.requireVisibleCourse(actorId, role, courseId));
    }

    @Transactional
    public CourseResponse update(UUID teacherId, UUID courseId, UpdateCourseRequest request) {
        Course course = permission.requireManagedCourse(teacherId, courseId);
        course.update(request.getTitle() == null ? null : request.getTitle().trim(),
                request.getDescription(), teacherId);
        return CourseResponse.from(courseRepository.save(course));
    }

    /** Xóa mềm; kết quả học tập đã ghi nhận vẫn được giữ (US-T12.4). */
    @Transactional
    public void delete(UUID teacherId, UUID courseId) {
        Course course = permission.requireManagedCourse(teacherId, courseId);
        course.markDeleted(teacherId);
        courseRepository.save(course);
    }
}
