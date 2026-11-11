package com.educmind.assessment.service;

import com.educmind.assessment.dto.AssessmentResponse;
import com.educmind.assessment.dto.CreateAssessmentRequest;
import com.educmind.assessment.dto.UpdateAssessmentRequest;
import com.educmind.assessment.entity.Assessment;
import com.educmind.assessment.entity.AssessmentQuestion;
import com.educmind.assessment.entity.AssessmentStatus;
import com.educmind.assessment.entity.Question;
import com.educmind.assessment.repository.AssessmentAttemptRepository;
import com.educmind.assessment.repository.AssessmentQuestionRepository;
import com.educmind.assessment.repository.AssessmentRepository;
import com.educmind.assessment.repository.QuestionRepository;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.CourseAccessPort;
import com.educmind.common.port.CourseContentPort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * US-T06 — tạo bài kiểm tra từ ngân hàng câu hỏi; US-T13 — xuất bản/huỷ xuất bản;
 * US-T12 — xoá mềm.
 */
@Service
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final QuestionRepository questionRepository;
    private final CourseAccessPort courseAccessPort;
    private final CourseContentPort contentPort;
    private final AssessmentPermission permission;
    private final JsonCodec codec;

    public AssessmentService(AssessmentRepository assessmentRepository,
                             AssessmentQuestionRepository assessmentQuestionRepository,
                             AssessmentAttemptRepository attemptRepository,
                             QuestionRepository questionRepository,
                             CourseAccessPort courseAccessPort,
                             CourseContentPort contentPort,
                             AssessmentPermission permission,
                             JsonCodec codec) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentQuestionRepository = assessmentQuestionRepository;
        this.attemptRepository = attemptRepository;
        this.questionRepository = questionRepository;
        this.courseAccessPort = courseAccessPort;
        this.contentPort = contentPort;
        this.permission = permission;
        this.codec = codec;
    }

    @Transactional
    public AssessmentResponse create(UUID teacherId, UUID courseId, CreateAssessmentRequest request) {
        permission.requireManagedCourse(teacherId, courseId);
        validateScope(courseId, request.getTopicId(), request.getLessonId());
        validateTimes(request.getOpenAt(), request.getCloseAt());

        Assessment assessment = new Assessment(courseId, request.getTitle().trim(),
                request.getDurationMinutes(), request.getMaxAttempts(),
                request.getOpenAt(), request.getCloseAt());
        assessment = assessmentRepository.save(assessment);

        replaceQuestions(assessment.getId(), courseId, request.getQuestions());
        return toResponse(assessment);
    }

    @Transactional(readOnly = true)
    public List<AssessmentResponse> list(UUID actorId, String role, UUID courseId) {
        boolean teacher = AssessmentPermission.isTeacher(role);
        if (teacher) {
            permission.requireManagedCourse(actorId, courseId);
        } else if (!courseAccessPort.isStudentEnrolled(actorId, courseId)) {
            throw new BusinessException("COURSE_ACCESS_DENIED",
                    "Bạn không có quyền truy cập khóa học này", HttpStatus.FORBIDDEN);
        }

        List<AssessmentResponse> result = new ArrayList<>();
        for (Assessment assessment : assessmentRepository
                .findByCourseIdAndDeletedAtIsNullOrderByOpenAtDesc(courseId)) {
            if (teacher || visibleForStudent(assessment)) {
                result.add(toResponse(assessment));
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public AssessmentResponse get(UUID actorId, String role, UUID assessmentId) {
        Assessment assessment = permission.requireVisibleAssessment(actorId, role, assessmentId);
        if (!AssessmentPermission.isTeacher(role)) {
            permission.requirePublishedForStudent(assessment);
        }
        return toResponse(assessment);
    }

    @Transactional
    public AssessmentResponse update(UUID teacherId, UUID assessmentId, UpdateAssessmentRequest request) {
        Assessment assessment = permission.requireManagedAssessment(teacherId, assessmentId);
        long attempts = attemptRepository.countByAssessmentId(assessmentId);

        if (request.getQuestions() != null && attempts > 0) {
            throw new BusinessException("ASSESSMENT_HAS_ATTEMPTS",
                    "Bài kiểm tra đã có học sinh làm, không thể đổi danh sách câu hỏi", HttpStatus.CONFLICT);
        }
        // AC-T11.6: không đặt số lần làm thấp hơn số lượt đã thực hiện
        if (request.getMaxAttempts() != null && attempts > 0) {
            int used = maxAttemptsUsed(assessmentId);
            if (request.getMaxAttempts() < used) {
                throw new BusinessException("MAX_ATTEMPTS_TOO_LOW",
                        "Số lần làm tối đa không được nhỏ hơn số lượt đã làm (" + used + ")",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        UUID topicId = assessment.getTopicId();
        UUID lessonId = assessment.getLessonId();
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
            validateScope(assessment.getCourseId(), topicId, lessonId);
        }

        OffsetDateTime openAt = request.getOpenAt() == null ? assessment.getOpenAt() : request.getOpenAt();
        OffsetDateTime closeAt = request.getCloseAt() == null ? assessment.getCloseAt() : request.getCloseAt();
        validateTimes(openAt, closeAt);

        assessment.update(request.getTitle() == null ? null : request.getTitle().trim(),
                request.getDurationMinutes(), request.getMaxAttempts(), request.getOpenAt(),
                request.getCloseAt(), topicId, lessonId, teacherId);
        assessment = assessmentRepository.save(assessment);

        if (request.getQuestions() != null) {
            replaceQuestions(assessmentId, assessment.getCourseId(), request.getQuestions());
        }
        return toResponse(assessment);
    }

    /** AC-T13.2: không xuất bản bài kiểm tra chưa có câu hỏi. */
    @Transactional
    public AssessmentResponse publish(UUID teacherId, UUID assessmentId) {
        Assessment assessment = permission.requireManagedAssessment(teacherId, assessmentId);

        if (assessmentQuestionRepository.countByAssessmentId(assessmentId) == 0) {
            throw new BusinessException("ASSESSMENT_HAS_NO_QUESTION",
                    "Bài kiểm tra phải có ít nhất một câu hỏi trước khi xuất bản",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        validateTimes(assessment.getOpenAt(), assessment.getCloseAt());

        assessment.publish(teacherId);
        return toResponse(assessmentRepository.save(assessment));
    }

    /** AC-T13.4: không huỷ xuất bản khi đã có học sinh làm bài. */
    @Transactional
    public AssessmentResponse unpublish(UUID teacherId, UUID assessmentId) {
        Assessment assessment = permission.requireManagedAssessment(teacherId, assessmentId);

        if (attemptRepository.countByAssessmentId(assessmentId) > 0) {
            throw new BusinessException("ASSESSMENT_HAS_ATTEMPTS",
                    "Bài kiểm tra đã có học sinh làm, không thể huỷ xuất bản", HttpStatus.CONFLICT);
        }
        assessment.unpublish(teacherId);
        return toResponse(assessmentRepository.save(assessment));
    }

    @Transactional
    public void delete(UUID teacherId, UUID assessmentId) {
        Assessment assessment = permission.requireManagedAssessment(teacherId, assessmentId);
        assessment.markDeleted(teacherId);
        assessmentRepository.save(assessment);
    }

    @Transactional(readOnly = true)
    public BigDecimal maxScore(UUID assessmentId) {
        BigDecimal total = BigDecimal.ZERO;
        for (AssessmentQuestion link : assessmentQuestionRepository.findByAssessmentIdOrderByIdAsc(assessmentId)) {
            total = total.add(link.getWeight());
        }
        return total;
    }

    AssessmentResponse toResponse(Assessment assessment) {
        return new AssessmentResponse(assessment.getId(), assessment.getCourseId(), assessment.getTopicId(),
                assessment.getLessonId(), assessment.getTitle(), assessment.getDurationMinutes(),
                assessment.getMaxAttempts(), assessment.getOpenAt(), assessment.getCloseAt(),
                assessment.getStatus().name(), maxScore(assessment.getId()),
                assessmentQuestionRepository.countByAssessmentId(assessment.getId()),
                assessment.getCreatedAt());
    }

    private int maxAttemptsUsed(UUID assessmentId) {
        int max = 0;
        for (var attempt : attemptRepository.findByAssessmentIdOrderByStartedAtAsc(assessmentId)) {
            max = Math.max(max, attempt.getAttemptNumber());
        }
        return max;
    }

    private boolean visibleForStudent(Assessment assessment) {
        if (assessment.getStatus() != AssessmentStatus.PUBLISHED) {
            return false;
        }
        if (assessment.getTopicId() != null && !contentPort.isTopicPublished(assessment.getTopicId())) {
            return false;
        }
        return assessment.getLessonId() == null || contentPort.isLessonPublished(assessment.getLessonId());
    }

    private void validateTimes(OffsetDateTime openAt, OffsetDateTime closeAt) {
        if (openAt == null || closeAt == null) {
            throw new BusinessException("ASSESSMENT_TIME_INVALID",
                    "Thiếu thời gian mở hoặc đóng", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        // AC-T06.2
        if (!closeAt.isAfter(openAt)) {
            throw new BusinessException("ASSESSMENT_TIME_INVALID",
                    "Thời gian đóng phải sau thời gian mở", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private void validateScope(UUID courseId, UUID topicId, UUID lessonId) {
        if (topicId != null && lessonId != null) {
            throw new BusinessException("SCOPE_CONFLICT",
                    "Bài kiểm tra chỉ thuộc một phạm vi: khóa học, chủ đề hoặc bài học",
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

    /** AC-T06.3 không trùng câu hỏi, AC-T06.6 câu hỏi cùng khóa học, trọng số > 0. */
    private void replaceQuestions(UUID assessmentId, UUID courseId,
                                  List<? extends Object> rawQuestions) {
        if (rawQuestions == null) {
            return;
        }
        assessmentQuestionRepository.deleteByAssessmentId(assessmentId);
        Set<UUID> seen = new HashSet<>();
        for (Object item : rawQuestions) {
            UUID questionId;
            BigDecimal weight;
            if (item instanceof CreateAssessmentRequest.QuestionWeight c) {
                questionId = c.getQuestionId();
                weight = c.getWeight();
            } else if (item instanceof UpdateAssessmentRequest.QuestionWeight u) {
                questionId = u.getQuestionId();
                weight = u.getWeight();
            } else {
                throw new BusinessException("INVALID_QUESTION_WEIGHT",
                        "Danh sách câu hỏi không hợp lệ", HttpStatus.UNPROCESSABLE_ENTITY);
            }

            if (questionId == null || weight == null) {
                throw new BusinessException("INVALID_QUESTION_WEIGHT",
                        "Câu hỏi phải có questionId và trọng số", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (weight.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("INVALID_QUESTION_WEIGHT",
                        "Trọng số phải lớn hơn 0", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (!seen.add(questionId)) {
                throw new BusinessException("DUPLICATE_QUESTION",
                        "Không được thêm cùng một câu hỏi nhiều lần", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            Question question = questionRepository.findByIdAndDeletedAtIsNull(questionId)
                    .orElseThrow(() -> new BusinessException("QUESTION_NOT_FOUND",
                            "Không tìm thấy câu hỏi: " + questionId, HttpStatus.UNPROCESSABLE_ENTITY));
            if (!question.getCourseId().equals(courseId)) {
                throw new BusinessException("QUESTION_NOT_IN_COURSE",
                        "Câu hỏi không thuộc khóa học này", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            assessmentQuestionRepository.save(new AssessmentQuestion(assessmentId, questionId, weight));
        }
    }
}
