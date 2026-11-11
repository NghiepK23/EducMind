package com.educmind.assignment.service;

import com.educmind.assignment.dto.AssignmentResponse;
import com.educmind.assignment.dto.CreateAssignmentRequest;
import com.educmind.assignment.dto.UpdateAssignmentRequest;
import com.educmind.assignment.entity.Assignment;
import com.educmind.assignment.entity.AssignmentObjective;
import com.educmind.assignment.entity.AssignmentStatus;
import com.educmind.assignment.repository.AssignmentObjectiveRepository;
import com.educmind.assignment.repository.AssignmentRepository;
import com.educmind.assignment.repository.AssignmentSubmissionRepository;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.CourseAccessPort;
import com.educmind.common.port.CourseContentPort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * US-T03 — tạo / sửa / xuất bản bài tập (AC-T03.1 … AC-T03.6),
 * US-T11 (AC-T11.2 khóa điểm tối đa khi đã có bài chấm, AC-T11.3 hạn nộp),
 * US-T12 (xoá mềm).
 */
@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentObjectiveRepository objectiveRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final CourseAccessPort courseAccessPort;
    private final CourseContentPort contentPort;
    private final AssignmentPermission permission;

    public AssignmentService(AssignmentRepository assignmentRepository,
                             AssignmentObjectiveRepository objectiveRepository,
                             AssignmentSubmissionRepository submissionRepository,
                             CourseAccessPort courseAccessPort,
                             CourseContentPort contentPort,
                             AssignmentPermission permission) {
        this.assignmentRepository = assignmentRepository;
        this.objectiveRepository = objectiveRepository;
        this.submissionRepository = submissionRepository;
        this.courseAccessPort = courseAccessPort;
        this.contentPort = contentPort;
        this.permission = permission;
    }

    @Transactional
    public AssignmentResponse create(UUID teacherId, UUID courseId, CreateAssignmentRequest request) {
        permission.requireManagedCourse(teacherId, courseId);

        validateScope(courseId, request.getTopicId(), request.getLessonId());
        validateLateWindow(request.isAllowLate(), request.getLateCloseDate(), request.getDueDate(), null);

        Assignment assignment = new Assignment(courseId, request.getTitle().trim(),
                request.getDescription().trim(), request.getDueDate(), request.getMaxScore(),
                request.isAllowLate(), request.getLateCloseDate());
        assignment = assignmentRepository.save(assignment);

        replaceObjectives(assignment.getId(), courseId, request.getObjectiveIds());

        return toResponse(assignment);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> list(UUID actorId, String role, UUID courseId) {
        if (AssignmentPermission.isTeacher(role)) {
            permission.requireManagedCourse(actorId, courseId);
        } else if (!courseAccessPort.isStudentEnrolled(actorId, courseId)) {
            throw new BusinessException("COURSE_ACCESS_DENIED",
                    "Bạn không có quyền truy cập khóa học này", HttpStatus.FORBIDDEN);
        }

        boolean teacher = AssignmentPermission.isTeacher(role);
        List<AssignmentResponse> result = new ArrayList<>();
        for (Assignment assignment : assignmentRepository
                .findByCourseIdAndDeletedAtIsNullOrderByDueDateAsc(courseId)) {
            if (!teacher && !visibleForStudent(assignment)) {
                continue;
            }
            result.add(toResponse(assignment));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public AssignmentResponse get(UUID actorId, String role, UUID assignmentId) {
        Assignment assignment = permission.requireVisibleAssignment(actorId, role, assignmentId);
        if (!AssignmentPermission.isTeacher(role)) {
            permission.requirePublishedForStudent(assignment);
        }
        return toResponse(assignment);
    }

    @Transactional
    public AssignmentResponse update(UUID teacherId, UUID assignmentId, UpdateAssignmentRequest request) {
        Assignment assignment = permission.requireManagedAssignment(teacherId, assignmentId);

        // AC-T11.2: đã có bài được chấm thì không đổi điểm tối đa.
        if (request.getMaxScore() != null
                && request.getMaxScore().compareTo(assignment.getMaxScore()) != 0
                && submissionRepository.countByAssignmentIdAndGradedAtIsNotNullAndDeletedAtIsNull(assignmentId) > 0) {
            throw new BusinessException("MAX_SCORE_LOCKED",
                    "Bài tập đã có bài được chấm, không thể đổi điểm tối đa", HttpStatus.CONFLICT);
        }

        UUID topicId = assignment.getTopicId();
        UUID lessonId = assignment.getLessonId();
        String scopeType = request.getScopeType();
        if (scopeType != null && !scopeType.isBlank()) {
            switch (scopeType.toUpperCase()) {
                case "COURSE" -> {
                    topicId = null;
                    lessonId = null;
                }
                case "TOPIC" -> {
                    if (request.getTopicId() == null) {
                        throw new BusinessException("SCOPE_INVALID",
                                "Phạm vi TOPIC phải có topicId", HttpStatus.UNPROCESSABLE_ENTITY);
                    }
                    topicId = request.getTopicId();
                    lessonId = null;
                }
                case "LESSON" -> {
                    if (request.getLessonId() == null) {
                        throw new BusinessException("SCOPE_INVALID",
                                "Phạm vi LESSON phải có lessonId", HttpStatus.UNPROCESSABLE_ENTITY);
                    }
                    lessonId = request.getLessonId();
                    topicId = null;
                }
                default -> throw new BusinessException("SCOPE_INVALID",
                        "scopeType chỉ nhận COURSE, TOPIC hoặc LESSON", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            validateScope(assignment.getCourseId(), topicId, lessonId);
        }

        OffsetDateTime dueDate = request.getDueDate() == null ? assignment.getDueDate() : request.getDueDate();
        Boolean allowLate = request.getAllowLate();
        boolean lateFlag = allowLate == null ? assignment.isAllowLate() : allowLate;
        validateLateWindow(lateFlag, request.getLateCloseDate(), dueDate, assignment.getId());

        assignment.update(request.getTitle() == null ? null : request.getTitle().trim(),
                request.getDescription(), request.getDueDate(), request.getMaxScore(), allowLate,
                request.getLateCloseDate(), topicId, lessonId, teacherId);
        assignment = assignmentRepository.save(assignment);

        if (request.getObjectiveIds() != null) {
            replaceObjectives(assignment.getId(), assignment.getCourseId(), request.getObjectiveIds());
        }

        return toResponse(assignment);
    }

    /** AC-T03.3: không xuất bản bài tập có hạn nộp trong quá khứ. */
    @Transactional
    public AssignmentResponse publish(UUID teacherId, UUID assignmentId) {
        Assignment assignment = permission.requireManagedAssignment(teacherId, assignmentId);

        if (assignment.getDueDate().isBefore(OffsetDateTime.now())) {
            throw new BusinessException("DUE_DATE_IN_PAST",
                    "Không thể xuất bản bài tập có hạn nộp trong quá khứ", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        assignment.publish(teacherId);
        return toResponse(assignmentRepository.save(assignment));
    }

    @Transactional
    public AssignmentResponse unpublish(UUID teacherId, UUID assignmentId) {
        Assignment assignment = permission.requireManagedAssignment(teacherId, assignmentId);

        if (submissionRepository.countByAssignmentIdAndGradedAtIsNotNullAndDeletedAtIsNull(assignmentId) > 0) {
            throw new BusinessException("ASSIGNMENT_HAS_GRADED_SUBMISSIONS",
                    "Bài tập đã có bài được chấm, không thể hủy xuất bản", HttpStatus.CONFLICT);
        }
        assignment.unpublish(teacherId);
        return toResponse(assignmentRepository.save(assignment));
    }

    /** US-T12: xoá mềm, giữ bài nộp và kết quả đã chấm (AC-T12.3, AC-T12.4). */
    @Transactional
    public void delete(UUID teacherId, UUID assignmentId) {
        Assignment assignment = permission.requireManagedAssignment(teacherId, assignmentId);
        assignment.markDeleted(teacherId);
        assignmentRepository.save(assignment);
    }

    @Transactional(readOnly = true)
    public List<UUID> objectiveIds(UUID assignmentId) {
        List<UUID> ids = new ArrayList<>();
        for (AssignmentObjective ao : objectiveRepository.findByIdAssignmentId(assignmentId)) {
            ids.add(ao.getObjectiveId());
        }
        return ids;
    }

    AssignmentResponse toResponse(Assignment assignment) {
        return AssignmentResponse.from(assignment, objectiveIds(assignment.getId()),
                submissionRepository.countByAssignmentIdAndGradedAtIsNotNullAndDeletedAtIsNull(assignment.getId()));
    }

    private boolean visibleForStudent(Assignment assignment) {
        if (assignment.getStatus() != AssignmentStatus.PUBLISHED) {
            return false;
        }
        if (assignment.getTopicId() != null && !contentPort.isTopicPublished(assignment.getTopicId())) {
            return false;
        }
        return assignment.getLessonId() == null || contentPort.isLessonPublished(assignment.getLessonId());
    }

    private void validateScope(UUID courseId, UUID topicId, UUID lessonId) {
        if (topicId != null && lessonId != null) {
            throw new BusinessException("SCOPE_CONFLICT",
                    "Bài tập chỉ thuộc một phạm vi: khóa học, chủ đề hoặc bài học",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (topicId != null) {
            UUID topicCourse = contentPort.findTopicCourseId(topicId)
                    .orElseThrow(() -> new BusinessException("SCOPE_NOT_IN_COURSE",
                            "Không tìm thấy chủ đề", HttpStatus.UNPROCESSABLE_ENTITY));
            if (!topicCourse.equals(courseId)) {
                throw new BusinessException("SCOPE_NOT_IN_COURSE",
                        "Chủ đề không thuộc khóa học này", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
        if (lessonId != null) {
            UUID lessonCourse = contentPort.findLessonCourseId(lessonId)
                    .orElseThrow(() -> new BusinessException("SCOPE_NOT_IN_COURSE",
                            "Không tìm thấy bài học", HttpStatus.UNPROCESSABLE_ENTITY));
            if (!lessonCourse.equals(courseId)) {
                throw new BusinessException("SCOPE_NOT_IN_COURSE",
                        "Bài học không thuộc khóa học này", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
    }

    /** AC-T03.2: cho phép nộp trễ thì phải có thời điểm đóng nộp trễ sau hạn nộp. */
    private void validateLateWindow(boolean allowLate, OffsetDateTime lateCloseDate,
                                    OffsetDateTime dueDate, UUID assignmentId) {
        if (!allowLate) {
            return;
        }
        OffsetDateTime close = lateCloseDate;
        if (close == null && assignmentId != null) {
            close = assignmentRepository.findByIdAndDeletedAtIsNull(assignmentId)
                    .map(Assignment::getLateCloseDate)
                    .orElse(null);
        }
        if (close == null) {
            throw new BusinessException("LATE_CLOSE_REQUIRED",
                    "Bật nộp trễ thì phải có thời điểm đóng nộp trễ", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (!close.isAfter(dueDate)) {
            throw new BusinessException("LATE_CLOSE_INVALID",
                    "Thời điểm đóng nộp trễ phải sau hạn nộp", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    /** AC-T03.6: mục tiêu phải thuộc cùng khóa học, không trùng lặp. */
    private void replaceObjectives(UUID assignmentId, UUID courseId, List<UUID> objectiveIds) {
        objectiveRepository.deleteByIdAssignmentId(assignmentId);
        if (objectiveIds == null || objectiveIds.isEmpty()) {
            return;
        }
        Set<UUID> unique = new LinkedHashSet<>(objectiveIds);
        Set<UUID> valid = new LinkedHashSet<>();
        for (CourseContentPort.ObjectiveRef ref : contentPort.findObjectives(unique)) {
            if (ref.courseId().equals(courseId)) {
                valid.add(ref.id());
            }
        }
        if (valid.size() != unique.size()) {
            throw new BusinessException("OBJECTIVE_NOT_IN_COURSE",
                    "Có mục tiêu học tập không thuộc khóa học này", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        for (UUID objectiveId : valid) {
            objectiveRepository.save(new AssignmentObjective(assignmentId, objectiveId));
        }
    }

}
