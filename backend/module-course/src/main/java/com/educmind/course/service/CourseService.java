package com.educmind.course.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.ClassAccessPort;
import com.educmind.common.port.UserLookupPort;
import com.educmind.course.dto.AddStudentsToCourseRequest;
import com.educmind.course.dto.AddTeacherToCourseRequest;
import com.educmind.course.dto.CourseMemberResponse;
import com.educmind.course.dto.CourseResponse;
import com.educmind.course.dto.CreateCourseRequest;
import com.educmind.course.dto.UpdateCourseRequest;
import com.educmind.course.entity.Course;
import com.educmind.course.entity.CourseMember;
import com.educmind.course.entity.Subject;
import com.educmind.course.repository.CourseMemberRepository;
import com.educmind.course.repository.CourseRepository;
import com.educmind.course.repository.SubjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseMemberRepository courseMemberRepository;
    private final SubjectRepository subjectRepository;
    private final UserLookupPort userLookupPort;
    private final ClassAccessPort classAccessPort;
    private final CourseAuthorizationHelper courseAuthorizationHelper;

    public CourseService(CourseRepository courseRepository,
                          CourseMemberRepository courseMemberRepository,
                          SubjectRepository subjectRepository,
                          UserLookupPort userLookupPort,
                          ClassAccessPort classAccessPort,
                          CourseAuthorizationHelper courseAuthorizationHelper) {
        this.courseRepository = courseRepository;
        this.courseMemberRepository = courseMemberRepository;
        this.subjectRepository = subjectRepository;
        this.userLookupPort = userLookupPort;
        this.classAccessPort = classAccessPort;
        this.courseAuthorizationHelper = courseAuthorizationHelper;
    }

    @Transactional
    public CourseResponse createCourse(UUID actorId, CreateCourseRequest request) {
        UserLookupPort.UserSummary actor = loadActorOrThrow(actorId);
        if (!"TEACHER".equals(actor.role())) {
            throw courseAuthorizationHelper.forbidden();
        }
        loadSubjectForSchool(request.getSubjectId(), actor.schoolId());

        Course course = new Course(actor.schoolId(), request.getName(), request.getSubjectId(), actorId);
        course.setDescription(request.getDescription());
        course = courseRepository.save(course);
        return toResponse(course);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> listCourses(UUID actorId) {
        UserLookupPort.UserSummary actor = loadActorOrThrow(actorId);
        List<Course> courses;

        switch (actor.role()) {
            case "TEACHER":
                List<Course> owned = courseRepository.findByCreatedByAndDeletedAtIsNull(actorId);
                List<UUID> coTeachingIds = courseMemberRepository
                        .findCourseIdsByUserIdAndMemberRoleAndLeftAtIsNull(actorId, "TEACHER");
                List<Course> coTeaching = courseRepository.findAllById(coTeachingIds).stream()
                        .filter(c -> c.getDeletedAt() == null)
                        .collect(Collectors.toList());

                Map<UUID, Course> dedup = new LinkedHashMap<>();
                for (Course c : owned) {
                    dedup.put(c.getId(), c);
                }
                for (Course c : coTeaching) {
                    dedup.put(c.getId(), c);
                }
                courses = new ArrayList<>(dedup.values());
                break;
            case "SCHOOL_ADMIN":
                courses = courseRepository.findBySchoolIdAndDeletedAtIsNull(actor.schoolId());
                break;
            case "STUDENT":
                // TODO: sẽ mở khi làm bước Lesson (học sinh xem course qua lesson đã publish)
                throw courseAuthorizationHelper.forbidden();
            default:
                throw courseAuthorizationHelper.forbidden();
        }

        return courses.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourseDetail(UUID actorId, UUID courseId) {
        UserLookupPort.UserSummary actor = loadActorOrThrow(actorId);
        Course course = courseAuthorizationHelper.loadCourseOrThrow(courseId);
        courseAuthorizationHelper.assertCanView(actor, course);
        return toResponse(course);
    }

    @Transactional
    public CourseResponse updateCourse(UUID actorId, UUID courseId, UpdateCourseRequest request) {
        UserLookupPort.UserSummary actor = loadActorOrThrow(actorId);
        Course course = courseAuthorizationHelper.loadCourseOrThrow(courseId);
        courseAuthorizationHelper.assertCanManage(actor, course);

        if (request.getName() != null) {
            course.setName(request.getName());
        }
        if (request.getDescription() != null) {
            course.setDescription(request.getDescription());
        }
        if (request.getSubjectId() != null) {
            loadSubjectForSchool(request.getSubjectId(), course.getSchoolId());
            course.setSubjectId(request.getSubjectId());
        }

        course = courseRepository.save(course);
        return toResponse(course);
    }

    @Transactional
    public void deleteCourse(UUID actorId, UUID courseId) {
        UserLookupPort.UserSummary actor = loadActorOrThrow(actorId);
        Course course = courseAuthorizationHelper.loadCourseOrThrow(courseId);
        courseAuthorizationHelper.assertIsOwner(actor, course);

        course.setDeletedAt(OffsetDateTime.now());
        courseRepository.save(course);
    }

    @Transactional
    public List<CourseMemberResponse> addStudentsToCourse(UUID actorId, UUID courseId,
                                                            AddStudentsToCourseRequest request) {
        UserLookupPort.UserSummary actor = loadActorOrThrow(actorId);
        Course course = courseAuthorizationHelper.loadCourseOrThrow(courseId);
        courseAuthorizationHelper.assertIsOwner(actor, course);

        for (UUID studentId : request.getStudentIds()) {
            UserLookupPort.UserSummary student = userLookupPort.findUserSummary(studentId)
                    .orElseThrow(() -> new BusinessException(
                            "USER_NOT_FOUND", "Student not found", HttpStatus.NOT_FOUND));

            if (!"STUDENT".equals(student.role()) || !student.active()) {
                throw new BusinessException(
                        "STUDENT_ROLE_REQUIRED", "User is not an active student", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (!classAccessPort.isActiveMember(studentId, request.getSourceClassId())) {
                throw new BusinessException(
                        "STUDENT_NOT_IN_SOURCE_CLASS", "Student is not a member of the source class",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (courseMemberRepository.existsByCourseIdAndUserIdAndLeftAtIsNull(courseId, studentId)) {
                continue;
            }

            Optional<CourseMember> existing = courseMemberRepository.findByCourseIdAndUserId(courseId, studentId);
            if (existing.isPresent()) {
                CourseMember member = existing.get();
                member.setLeftAt(null);
                member.setSourceClassId(request.getSourceClassId());
                courseMemberRepository.save(member);
            } else {
                CourseMember member = new CourseMember(
                        courseId, studentId, "STUDENT", request.getSourceClassId(), actorId);
                courseMemberRepository.save(member);
            }
        }

        return listMembers(actorId, courseId);
    }

    @Transactional
    public List<CourseMemberResponse> addTeacherToCourse(UUID actorId, UUID courseId,
                                                           AddTeacherToCourseRequest request) {
        UserLookupPort.UserSummary actor = loadActorOrThrow(actorId);
        Course course = courseAuthorizationHelper.loadCourseOrThrow(courseId);
        courseAuthorizationHelper.assertIsOwner(actor, course);

        UUID teacherId = request.getTeacherId();
        UserLookupPort.UserSummary teacher = userLookupPort.findUserSummary(teacherId)
                .orElseThrow(() -> new BusinessException(
                        "USER_NOT_FOUND", "Teacher not found", HttpStatus.NOT_FOUND));

        if (!"TEACHER".equals(teacher.role()) || !teacher.active()) {
            throw new BusinessException(
                    "TEACHER_ROLE_REQUIRED", "User is not an active teacher", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (!courseMemberRepository.existsByCourseIdAndUserIdAndLeftAtIsNull(courseId, teacherId)) {
            Optional<CourseMember> existing = courseMemberRepository.findByCourseIdAndUserId(courseId, teacherId);
            if (existing.isPresent()) {
                CourseMember member = existing.get();
                member.setLeftAt(null);
                member.setSourceClassId(null);
                courseMemberRepository.save(member);
            } else {
                CourseMember member = new CourseMember(courseId, teacherId, "TEACHER", null, actorId);
                courseMemberRepository.save(member);
            }
        }

        return listMembers(actorId, courseId);
    }

    @Transactional
    public void removeMember(UUID actorId, UUID courseId, UUID userId) {
        UserLookupPort.UserSummary actor = loadActorOrThrow(actorId);
        Course course = courseAuthorizationHelper.loadCourseOrThrow(courseId);
        courseAuthorizationHelper.assertIsOwner(actor, course);

        CourseMember member = courseMemberRepository.findByCourseIdAndUserId(courseId, userId)
                .filter(m -> m.getLeftAt() == null)
                .orElseThrow(() -> new BusinessException(
                        "MEMBER_NOT_FOUND", "Course member not found", HttpStatus.NOT_FOUND));

        member.setLeftAt(OffsetDateTime.now());
        courseMemberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public List<CourseMemberResponse> listMembers(UUID actorId, UUID courseId) {
        UserLookupPort.UserSummary actor = loadActorOrThrow(actorId);
        Course course = courseAuthorizationHelper.loadCourseOrThrow(courseId);
        courseAuthorizationHelper.assertCanView(actor, course);

        return courseMemberRepository.findByCourseIdAndLeftAtIsNullOrderByAddedAtAsc(courseId).stream()
                .map(this::toCourseMemberResponse)
                .collect(Collectors.toList());
    }

    private UserLookupPort.UserSummary loadActorOrThrow(UUID actorId) {
        return userLookupPort.findUserSummary(actorId)
                .filter(UserLookupPort.UserSummary::active)
                .orElseThrow(() -> new BusinessException(
                        "USER_NOT_FOUND", "Actor not found", HttpStatus.NOT_FOUND));
    }

    private Subject loadSubjectForSchool(UUID subjectId, UUID schoolId) {
        Subject subject = subjectRepository.findByIdAndDeletedAtIsNull(subjectId)
                .orElseThrow(this::subjectNotFound);
        if (!subject.getSchoolId().equals(schoolId)) {
            throw subjectNotFound();
        }
        return subject;
    }

    private BusinessException subjectNotFound() {
        return new BusinessException("SUBJECT_NOT_FOUND", "Subject not found", HttpStatus.NOT_FOUND);
    }

    private CourseResponse toResponse(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getSchoolId(),
                course.getName(),
                course.getDescription(),
                course.getSubjectId(),
                course.getCreatedBy(),
                course.getStatus(),
                course.getCreatedAt(),
                course.getUpdatedAt(),
                course.getDeletedAt()
        );
    }

    private CourseMemberResponse toCourseMemberResponse(CourseMember member) {
        return new CourseMemberResponse(
                member.getId(),
                member.getCourseId(),
                member.getUserId(),
                member.getMemberRole(),
                member.getSourceClassId(),
                member.getAddedBy(),
                member.getAddedAt(),
                member.getLeftAt()
        );
    }
}
