package com.educmind.course.service;

import com.educmind.common.port.ClassAccessPort;
import com.educmind.common.port.CourseAccessPort;
import com.educmind.course.entity.Course;
import com.educmind.course.repository.CourseRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CourseAccessPortImpl implements CourseAccessPort {

    private final CourseRepository courseRepository;
    private final ClassAccessPort classAccessPort;

    public CourseAccessPortImpl(CourseRepository courseRepository, ClassAccessPort classAccessPort) {
        this.courseRepository = courseRepository;
        this.classAccessPort = classAccessPort;
    }

    @Override
    public boolean isOwner(UUID actorId, UUID courseId) {
        return courseRepository.findByIdAndDeletedAtIsNull(courseId)
                .map(c -> classAccessPort.isOwner(actorId, c.getClassId()))
                .orElse(false);
    }

    @Override
    public boolean isStudentEnrolled(UUID studentId, UUID courseId) {
        return courseRepository.findByIdAndDeletedAtIsNull(courseId)
                .map(c -> classAccessPort.isMember(studentId, c.getClassId()))
                .orElse(false);
    }

    @Override
    public Optional<UUID> findClassId(UUID courseId) {
        return courseRepository.findByIdAndDeletedAtIsNull(courseId).map(Course::getClassId);
    }

    @Override
    public List<UUID> findAccessibleCourseIdsForTeacher(UUID teacherId) {
        List<UUID> classIds = classAccessPort.findClassIdsForTeacher(teacherId);
        List<UUID> result = new ArrayList<>();
        for (UUID classId : classIds) {
            courseRepository.findByClassIdAndDeletedAtIsNullOrderByCreatedAtAsc(classId)
                    .forEach(c -> result.add(c.getId()));
        }
        return result;
    }

    @Override
    public List<UUID> findCourseIdsForStudent(UUID studentId) {
        List<UUID> result = new ArrayList<>();
        for (UUID classId : classAccessPort.findClassIdsForStudent(studentId)) {
            courseRepository.findByClassIdAndDeletedAtIsNullOrderByCreatedAtAsc(classId)
                    .forEach(c -> result.add(c.getId()));
        }
        return result;
    }

    @Override
    public List<UUID> findStudentIdsByCourse(UUID courseId) {
        return courseRepository.findByIdAndDeletedAtIsNull(courseId)
                .map(c -> classAccessPort.findStudentIds(c.getClassId()))
                .orElse(List.of());
    }
}
