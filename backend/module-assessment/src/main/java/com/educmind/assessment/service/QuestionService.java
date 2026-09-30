package com.educmind.assessment.service;

import com.educmind.assessment.dto.CreateQuestionRequest;
import com.educmind.assessment.dto.QuestionResponse;
import com.educmind.assessment.dto.ReviewQuestionRequest;
import com.educmind.assessment.dto.UpdateQuestionRequest;
import com.educmind.assessment.entity.Question;
import com.educmind.assessment.entity.QuestionSource;
import com.educmind.assessment.repository.QuestionRepository;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.CourseAccessPort;
import com.educmind.common.port.LessonAccessPort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Logic nghiệp vụ Question Bank (ngân hàng câu hỏi) — Module04_Assignment_Assessment_FINAL.md,
 * PHẦN E (E.4-E.9).
 *
 * Authorization: mọi hành động (tạo/sửa/xóa/review) — owner HOẶC đồng giảng dạy
 * course (courseAccessPort.isOwnerOrCoTeacher), ngang quyền — nhất quán nguyên tắc
 * đã áp dụng cho toàn bộ Module 4 (xem AssignmentService, module-assignment).
 *
 * [ĐÃ SỬA — V5__module_analytics_lo_id_fix.sql] learningObjectiveId (UUID) != null
 * BẮT BUỘC kéo theo lessonId != null — validate ở đây (createQuestion/updateQuestion),
 * entity Question chỉ là data holder thuần túy, không tự validate. Việc objectiveId
 * có thực sự thuộc đúng lesson được kiểm tra qua LessonAccessPort (assertObjectiveBelongsToLesson).
 *
 * Câu hỏi tạo thủ công qua Service này LUÔN source=MANUAL (AI sinh câu hỏi là việc
 * của Module 7 — AI Teacher Assistant, chưa code, sẽ ghi thẳng qua QuestionRepository
 * với source=AI_GENERATED, review_status=APPROVED theo PATCH_4Decisions PATCH 1).
 */
@Service
public class QuestionService {

    private static final String ACTION_APPROVE = "APPROVE";
    private static final String ACTION_REJECT = "REJECT";

    private final QuestionRepository questionRepository;
    private final CourseAccessPort courseAccessPort;
    private final LessonAccessPort lessonAccessPort;

    public QuestionService(QuestionRepository questionRepository, CourseAccessPort courseAccessPort,
                           LessonAccessPort lessonAccessPort) {
        this.questionRepository = questionRepository;
        this.courseAccessPort = courseAccessPort;
        this.lessonAccessPort = lessonAccessPort;
    }

    /** POST /api/v1/courses/{id}/questions — owner/đồng giảng dạy. */
    @Transactional
    public QuestionResponse createQuestion(UUID actorId, UUID courseId, CreateQuestionRequest request) {
        assertCanManage(actorId, courseId);
        assertLessonRequiredWhenObjectiveIdPresent(request.getLearningObjectiveId(), request.getLessonId());
        assertLessonBelongsToCourse(request.getLessonId(), courseId);
        assertObjectiveBelongsToLesson(request.getLessonId(), request.getLearningObjectiveId());

        Question question = new Question(courseId, request.getQuestionText(), request.getQuestionType(),
                request.getDifficulty(), actorId);
        question.setLessonId(request.getLessonId());
        question.setCognitiveLevel(request.getCognitiveLevel());
        question.setLearningObjectiveId(request.getLearningObjectiveId());
        question.setOptions(request.getOptions());
        question.setCorrectAnswer(request.getCorrectAnswer());
        question.setExplanation(request.getExplanation());
        question.setSource(QuestionSource.MANUAL);

        question = questionRepository.save(question);
        return toResponse(question);
    }

    /** GET /api/v1/courses/{id}/questions — TEACHER (owner/đồng giảng dạy) xem ngân hàng câu hỏi. */
    @Transactional(readOnly = true)
    public List<QuestionResponse> listQuestions(UUID actorId, UUID courseId) {
        assertCanManage(actorId, courseId);
        return questionRepository.findByCourseIdAndDeletedAtIsNullOrderByCreatedAtDesc(courseId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** PATCH /api/v1/questions/{id} — owner/đồng giảng dạy. */
    @Transactional
    public QuestionResponse updateQuestion(UUID actorId, UUID questionId, UpdateQuestionRequest request) {
        Question question = loadQuestionOrThrow(questionId);
        assertCanManage(actorId, question.getCourseId());

        UUID lessonId = request.getLessonId() != null ? request.getLessonId() : question.getLessonId();
        UUID objectiveId = request.getLearningObjectiveId() != null
                ? request.getLearningObjectiveId() : question.getLearningObjectiveId();
        assertLessonRequiredWhenObjectiveIdPresent(objectiveId, lessonId);
        if (request.getLessonId() != null) {
            assertLessonBelongsToCourse(request.getLessonId(), question.getCourseId());
        }
        if (request.getLessonId() != null || request.getLearningObjectiveId() != null) {
            assertObjectiveBelongsToLesson(lessonId, objectiveId);
        }

        if (request.getLessonId() != null) {
            question.setLessonId(request.getLessonId());
        }
        if (request.getQuestionText() != null) {
            if (!StringUtils.hasText(request.getQuestionText())) {
                throw new BusinessException("VALIDATION_ERROR", "questionText không được để trống",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
            question.setQuestionText(request.getQuestionText());
        }
        if (request.getQuestionType() != null) {
            question.setQuestionType(request.getQuestionType());
        }
        if (request.getDifficulty() != null) {
            question.setDifficulty(request.getDifficulty());
        }
        if (request.getCognitiveLevel() != null) {
            question.setCognitiveLevel(request.getCognitiveLevel());
        }
        if (request.getLearningObjectiveId() != null) {
            question.setLearningObjectiveId(request.getLearningObjectiveId());
        }
        if (request.getOptions() != null) {
            question.setOptions(request.getOptions());
        }
        if (request.getCorrectAnswer() != null) {
            question.setCorrectAnswer(request.getCorrectAnswer());
        }
        if (request.getExplanation() != null) {
            question.setExplanation(request.getExplanation());
        }

        question = questionRepository.saveAndFlush(question);
        return toResponse(question);
    }

    /**
     * PATCH /api/v1/questions/{id}/review — owner/đồng giảng dạy. Sau PATCH_4Decisions
     * PATCH 1, chỉ còn ý nghĩa thực tế cho source=MANUAL (E.5bis).
     */
    @Transactional
    public QuestionResponse reviewQuestion(UUID actorId, UUID questionId, ReviewQuestionRequest request) {
        Question question = loadQuestionOrThrow(questionId);
        assertCanManage(actorId, question.getCourseId());

        String action = request.getAction() == null ? "" : request.getAction().trim().toUpperCase();
        if (ACTION_APPROVE.equals(action)) {
            question.approve();
        } else if (ACTION_REJECT.equals(action)) {
            question.reject();
        } else {
            throw new BusinessException("VALIDATION_ERROR",
                    "action phải là APPROVE hoặc REJECT", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        question = questionRepository.saveAndFlush(question);
        return toResponse(question);
    }

    /** DELETE /api/v1/questions/{id} — soft-delete, owner/đồng giảng dạy. */
    @Transactional
    public QuestionResponse deleteQuestion(UUID actorId, UUID questionId) {
        Question question = loadQuestionOrThrow(questionId);
        assertCanManage(actorId, question.getCourseId());

        question.setDeletedAt(OffsetDateTime.now());
        question = questionRepository.saveAndFlush(question);
        return toResponse(question);
    }

    // ------------------------------------------------------------------
    // Helpers riêng — dùng chung với AssessmentService qua package-private nếu cần sau này
    // ------------------------------------------------------------------

    private void assertCanManage(UUID actorId, UUID courseId) {
        if (!courseAccessPort.isOwnerOrCoTeacher(actorId, courseId)) {
            throw new BusinessException("NOT_COURSE_OWNER",
                    "Bạn không có quyền quản lý course này", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * [ĐÃ SỬA — V5__module_analytics_lo_id_fix.sql] learningObjectiveId != null BẮT BUỘC
     * kéo theo lessonId != null.
     */
    private void assertLessonRequiredWhenObjectiveIdPresent(UUID learningObjectiveId, UUID lessonId) {
        if (learningObjectiveId != null && lessonId == null) {
            throw new BusinessException("VALIDATION_ERROR",
                    "learningObjectiveId khác NULL bắt buộc phải có lessonId đi kèm",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private void assertObjectiveBelongsToLesson(UUID lessonId, UUID learningObjectiveId) {
        if (learningObjectiveId == null) {
            return;
        }
        if (!lessonAccessPort.objectiveBelongsToLesson(lessonId, learningObjectiveId)) {
            throw new BusinessException("VALIDATION_ERROR",
                    "learningObjectiveId không thuộc lesson này",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    /**
     * Nếu có lessonId thì lesson đó phải thuộc đúng course của câu hỏi.
     * lessonId == null (câu hỏi tổng hợp toàn course) thì bỏ qua.
     */
    private void assertLessonBelongsToCourse(UUID lessonId, UUID courseId) {
        if (lessonId == null) {
            return;
        }
        if (!lessonAccessPort.lessonBelongsToCourse(lessonId, courseId)) {
            throw new BusinessException("VALIDATION_ERROR",
                    "lessonId không thuộc course này",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private Question loadQuestionOrThrow(UUID questionId) {
        return questionRepository.findByIdAndDeletedAtIsNull(questionId)
                .orElseThrow(() -> new BusinessException("QUESTION_NOT_FOUND",
                        "Không tìm thấy câu hỏi", HttpStatus.NOT_FOUND));
    }

    private QuestionResponse toResponse(Question q) {
        return new QuestionResponse(
                q.getId(), q.getCourseId(), q.getLessonId(), q.getQuestionText(),
                q.getQuestionType() != null ? q.getQuestionType().name() : null,
                q.getDifficulty() != null ? q.getDifficulty().name() : null,
                q.getCognitiveLevel() != null ? q.getCognitiveLevel().name() : null,
                q.getLearningObjectiveId(), q.getOptions(), q.getCorrectAnswer(), q.getExplanation(),
                q.getSource() != null ? q.getSource().name() : null,
                q.getReviewStatus() != null ? q.getReviewStatus().name() : null,
                q.getCreatedBy(), q.getCreatedAt(), q.getUpdatedAt(), q.getDeletedAt()
        );
    }
}
