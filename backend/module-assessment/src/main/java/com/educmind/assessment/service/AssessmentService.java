package com.educmind.assessment.service;

import com.educmind.assessment.dto.AddQuestionToAssessmentRequest;
import com.educmind.assessment.dto.AssessmentAttemptDetailResponse;
import com.educmind.assessment.dto.AssessmentAttemptResponse;
import com.educmind.assessment.dto.AssessmentQuestionResponse;
import com.educmind.assessment.dto.AssessmentResponse;
import com.educmind.assessment.dto.CreateAssessmentRequest;
import com.educmind.assessment.dto.GradeAnswerRequest;
import com.educmind.assessment.dto.SaveAnswerRequest;
import com.educmind.assessment.dto.SubmissionAnswerResponse;
import com.educmind.assessment.entity.Assessment;
import com.educmind.assessment.entity.AssessmentAttempt;
import com.educmind.assessment.entity.AssessmentQuestion;
import com.educmind.assessment.entity.AttemptStatus;
import com.educmind.assessment.entity.Question;
import com.educmind.assessment.entity.QuestionType;
import com.educmind.assessment.entity.SubmissionAnswer;
import com.educmind.assessment.repository.AssessmentAttemptRepository;
import com.educmind.assessment.repository.AssessmentQuestionRepository;
import com.educmind.assessment.repository.AssessmentRepository;
import com.educmind.assessment.repository.QuestionRepository;
import com.educmind.assessment.repository.SubmissionAnswerRepository;
import com.educmind.common.event.AssessmentGradedEvent;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.CourseAccessPort;
import com.educmind.common.port.LearningEvidencePort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Logic nghiệp vụ Assessment (đề kiểm tra) + Attempt + SubmissionAnswer —
 * Module04_Assignment_Assessment_FINAL.md, PHẦN E (E.4-E.9).
 *
 * Authorization: tạo/sửa đề, thêm câu hỏi, chấm tay — owner HOẶC đồng giảng dạy
 * (courseAccessPort.isOwnerOrCoTeacher), ngang quyền, nhất quán AssignmentService/
 * QuestionService. Học sinh chỉ thao tác trên attempt của chính mình.
 *
 * [DECIDED — E.5 mục 4] Auto-scoring chỉ áp dụng SINGLE_CHOICE/MULTIPLE_CHOICE/
 * TRUE_FALSE, so khớp studentAnswer với correctAnswer bằng cách chuẩn hóa cả 2 về
 * Set<String> (không phụ thuộc thứ tự phần tử trong JSONB). SHORT_ANSWER/ESSAY giữ
 * isCorrect=NULL, scoreAwarded=NULL, chờ giáo viên chấm tay (gradeAnswer).
 */
@Service
public class AssessmentService {

    private static final Set<QuestionType> AUTO_SCORABLE_TYPES = Set.of(
            QuestionType.SINGLE_CHOICE, QuestionType.MULTIPLE_CHOICE, QuestionType.TRUE_FALSE);

    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final SubmissionAnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final CourseAccessPort courseAccessPort;
    private final LearningEvidencePort learningEvidencePort;
    private final ApplicationEventPublisher eventPublisher;

    public AssessmentService(AssessmentRepository assessmentRepository,
                              AssessmentQuestionRepository assessmentQuestionRepository,
                              AssessmentAttemptRepository attemptRepository,
                              SubmissionAnswerRepository answerRepository,
                              QuestionRepository questionRepository,
                              CourseAccessPort courseAccessPort,
                              LearningEvidencePort learningEvidencePort,
                              ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.assessmentRepository = assessmentRepository;
        this.assessmentQuestionRepository = assessmentQuestionRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.courseAccessPort = courseAccessPort;
        this.learningEvidencePort = learningEvidencePort;
    }

    /** POST /api/v1/courses/{id}/assessments — owner/đồng giảng dạy. */
    @Transactional
    public AssessmentResponse createAssessment(UUID actorId, UUID courseId, CreateAssessmentRequest request) {
        assertCanManage(actorId, courseId);

        Assessment assessment = new Assessment(courseId, request.getTitle(), actorId);
        assessment.setDescription(request.getDescription());
        assessment.setDurationMinutes(request.getDurationMinutes());
        if (request.getMaxAttempts() != null) {
            assessment.setMaxAttempts(request.getMaxAttempts());
        }
        assessment.setBlueprintConfig(request.getBlueprintConfig());
        assessment.setOpensAt(request.getOpensAt());
        assessment.setClosesAt(request.getClosesAt());

        assessment = assessmentRepository.save(assessment);
        return toResponse(assessment);
    }

    /**
     * POST /api/v1/assessments/{id}/questions — owner/đồng giảng dạy.
     * E.5 mục 6: đề đã có attempt thì không được sửa danh sách câu hỏi (ASSESSMENT_LOCKED).
     */
    @Transactional
    public AssessmentQuestionResponse addQuestion(UUID actorId, UUID assessmentId,
                                                   AddQuestionToAssessmentRequest request) {
        Assessment assessment = loadAssessmentOrThrow(assessmentId);
        assertCanManage(actorId, assessment.getCourseId());

        if (attemptRepository.existsByAssessmentId(assessmentId)) {
            throw new BusinessException("ASSESSMENT_LOCKED",
                    "Đề đã có học sinh làm bài, không thể sửa danh sách câu hỏi", HttpStatus.CONFLICT);
        }

        if (assessmentQuestionRepository.findByAssessmentIdAndQuestionId(assessmentId, request.getQuestionId())
                .isPresent()) {
            throw new BusinessException("VALIDATION_ERROR",
                    "Câu hỏi này đã có trong đề", HttpStatus.CONFLICT);
        }

        // Xác nhận câu hỏi tồn tại và thuộc đúng course của đề (tránh trộn câu hỏi course khác).
        Question question = questionRepository.findByIdAndDeletedAtIsNull(request.getQuestionId())
                .orElseThrow(() -> new BusinessException("QUESTION_NOT_FOUND",
                        "Không tìm thấy câu hỏi", HttpStatus.NOT_FOUND));
        if (!question.getCourseId().equals(assessment.getCourseId())) {
            throw new BusinessException("VALIDATION_ERROR",
                    "Câu hỏi không thuộc course của đề này", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        AssessmentQuestion aq = new AssessmentQuestion(assessmentId, request.getQuestionId());
        if (request.getOrderIndex() != null) {
            aq.setOrderIndex(request.getOrderIndex());
        }
        if (request.getScoreWeight() != null) {
            aq.setScoreWeight(request.getScoreWeight());
        }

        aq = assessmentQuestionRepository.save(aq);
        return toAssessmentQuestionResponse(aq);
    }

    /** PATCH /api/v1/assessments/{id}/publish — owner/đồng giảng dạy. */
    @Transactional
    public AssessmentResponse publishAssessment(UUID actorId, UUID assessmentId) {
        Assessment assessment = loadAssessmentOrThrow(assessmentId);
        assertCanManage(actorId, assessment.getCourseId());

        assessment.publish();
        assessment = assessmentRepository.saveAndFlush(assessment);
        return toResponse(assessment);
    }

    /**
     * POST /api/v1/assessments/{id}/attempts — STUDENT bắt đầu làm bài.
     * E.5 mục 2/3: phải PUBLISHED, trong khoảng opensAt-closesAt, không vượt maxAttempts.
     */
    @Transactional
    public AssessmentAttemptResponse startAttempt(UUID studentId, UUID assessmentId) {
        Assessment assessment = loadAssessmentOrThrow(assessmentId);
        assertStudentEnrolled(studentId, assessment.getCourseId());

        if (assessment.getPublishStatus() == null
                || !"PUBLISHED".equals(assessment.getPublishStatus().name())) {
            throw new BusinessException("ASSESSMENT_NOT_PUBLISHED",
                    "Đề kiểm tra này chưa được xuất bản", HttpStatus.FORBIDDEN);
        }

        OffsetDateTime now = OffsetDateTime.now();
        if ((assessment.getOpensAt() != null && now.isBefore(assessment.getOpensAt()))
                || (assessment.getClosesAt() != null && now.isAfter(assessment.getClosesAt()))) {
            throw new BusinessException("ASSESSMENT_NOT_OPEN",
                    "Ngoài khoảng thời gian cho phép làm bài", HttpStatus.FORBIDDEN);
        }

        long doneCount = attemptRepository.countByAssessmentIdAndStudentId(assessmentId, studentId);
        if (doneCount >= assessment.getMaxAttempts()) {
            throw new BusinessException("ATTEMPT_LIMIT_EXCEEDED",
                    "Đã hết số lần làm bài cho phép", HttpStatus.FORBIDDEN);
        }

        int nextAttemptNumber = (int) doneCount + 1;
        AssessmentAttempt attempt = new AssessmentAttempt(assessmentId, studentId, nextAttemptNumber);
        attempt = attemptRepository.save(attempt);
        return toAttemptResponse(attempt);
    }

    /**
     * PATCH /api/v1/attempts/{id}/answers — STUDENT lưu câu trả lời (autosave, gọi nhiều lần).
     * E.8: ATTEMPT_ALREADY_SUBMITTED nếu attempt không còn IN_PROGRESS.
     */
    @Transactional
    public SubmissionAnswerResponse saveAnswer(UUID studentId, UUID attemptId, SaveAnswerRequest request) {
        AssessmentAttempt attempt = loadAttemptOrThrow(attemptId);
        assertOwnAttempt(studentId, attempt);

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BusinessException("ATTEMPT_ALREADY_SUBMITTED",
                    "Bài làm đã nộp, không thể sửa câu trả lời", HttpStatus.CONFLICT);
        }

        SubmissionAnswer answer = answerRepository
                .findByAttemptIdAndQuestionId(attemptId, request.getQuestionId())
                .orElseGet(() -> new SubmissionAnswer(attemptId, request.getQuestionId()));
        answer.saveAnswer(request.getStudentAnswer());
        answer = answerRepository.save(answer);
        return toAnswerResponse(answer);
    }

    /**
     * POST /api/v1/attempts/{id}/submit — STUDENT nộp bài, kích hoạt auto-scoring.
     * E.5 mục 4: auto-score trắc nghiệm ngay; nếu không còn câu tự luận nào chưa
     * chấm thì chuyển thẳng GRADED (đề chỉ có trắc nghiệm), ngược lại giữ SUBMITTED
     * chờ giáo viên chấm tay.
     */
    @Transactional
    public AssessmentAttemptResponse submitAttempt(UUID studentId, UUID attemptId) {
        AssessmentAttempt attempt = loadAttemptOrThrow(attemptId);
        assertOwnAttempt(studentId, attempt);

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BusinessException("ATTEMPT_ALREADY_SUBMITTED",
                    "Bài làm này đã được nộp trước đó", HttpStatus.CONFLICT);
        }

        List<AssessmentQuestion> assessmentQuestions =
                assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndexAsc(attempt.getAssessmentId());

        for (AssessmentQuestion aq : assessmentQuestions) {
            Question question = questionRepository.findByIdAndDeletedAtIsNull(aq.getQuestionId()).orElse(null);
            if (question == null || !AUTO_SCORABLE_TYPES.contains(question.getQuestionType())) {
                continue;
            }
            SubmissionAnswer answer = answerRepository
                    .findByAttemptIdAndQuestionId(attemptId, aq.getQuestionId())
                    .orElseGet(() -> answerRepository.save(new SubmissionAnswer(attemptId, aq.getQuestionId())));

            boolean isCorrect = isAnswerCorrect(answer.getStudentAnswer(), question.getCorrectAnswer());
            BigDecimal scoreAwarded = isCorrect ? aq.getScoreWeight() : BigDecimal.ZERO;
            answer.autoScore(isCorrect, scoreAwarded);
            answerRepository.save(answer);
        }

        attempt.submit();

        long pendingManual = answerRepository.countByAttemptIdAndIsCorrectIsNull(attemptId);
        boolean gradedNow = false;
        if (pendingManual == 0) {
            attempt.markGraded(computeTotalScore(attemptId));
            gradedNow = true;
        }

        attempt = attemptRepository.save(attempt);
        if (gradedNow) {
            UUID gradedCourseId = loadAssessmentOrThrow(attempt.getAssessmentId()).getCourseId();
            publishAttemptEvidence(attempt, gradedCourseId);
            eventPublisher.publishEvent(new AssessmentGradedEvent(attempt.getId(),
                    attempt.getAssessmentId(), gradedCourseId, attempt.getStudentId(),
                    attempt.getTotalScore()));
        }
        return toAttemptResponse(attempt);
    }

    /**
     * GET /api/v1/attempts/{id} — chủ attempt hoặc TEACHER (owner/đồng giảng dạy).
     */
    @Transactional(readOnly = true)
    public AssessmentAttemptDetailResponse getAttempt(UUID actorId, UUID attemptId) {
        AssessmentAttempt attempt = loadAttemptOrThrow(attemptId);
        Assessment assessment = loadAssessmentOrThrow(attempt.getAssessmentId());

        boolean isOwnAttempt = attempt.getStudentId().equals(actorId);
        boolean isManager = courseAccessPort.isOwnerOrCoTeacher(actorId, assessment.getCourseId());
        if (!isOwnAttempt && !isManager) {
            throw new BusinessException("FORBIDDEN_NOT_SELF",
                    "Bạn không có quyền xem bài làm này", HttpStatus.FORBIDDEN);
        }

        return toAttemptDetailResponse(attempt);
    }

    /**
     * PATCH /api/v1/attempts/{id}/grade — owner/đồng giảng dạy chấm tay câu tự luận.
     * E.5 mục 4/5: sau khi chấm, nếu không còn câu nào chưa chấm → chuyển GRADED,
     * tính lại total_score.
     */
    @Transactional
    public AssessmentAttemptResponse gradeAnswer(UUID actorId, UUID attemptId, GradeAnswerRequest request) {
        AssessmentAttempt attempt = loadAttemptOrThrow(attemptId);
        Assessment assessment = loadAssessmentOrThrow(attempt.getAssessmentId());
        assertCanManage(actorId, assessment.getCourseId());

        SubmissionAnswer answer = answerRepository
                .findByAttemptIdAndQuestionId(attemptId, request.getQuestionId())
                .orElseThrow(() -> new BusinessException("ANSWER_NOT_FOUND",
                        "Không tìm thấy câu trả lời cần chấm", HttpStatus.NOT_FOUND));

        answer.gradeManually(request.getScoreAwarded(), request.getTeacherFeedback());
        answerRepository.save(answer);

        boolean wasGraded = attempt.getStatus() == AttemptStatus.GRADED;
        long pendingManual = answerRepository.countByAttemptIdAndScoreAwardedIsNull(attemptId);
        if (pendingManual == 0) {
            attempt.markGraded(computeTotalScore(attemptId));
            attempt = attemptRepository.save(attempt);
            if (!wasGraded) {
                publishAttemptEvidence(attempt, assessment.getCourseId());
                eventPublisher.publishEvent(new AssessmentGradedEvent(attempt.getId(),
                        attempt.getAssessmentId(), assessment.getCourseId(), attempt.getStudentId(),
                        attempt.getTotalScore()));
            }
        }

        return toAttemptResponse(attempt);
    }

    // ------------------------------------------------------------------
    // Helpers riêng
    // ------------------------------------------------------------------

    private void assertCanManage(UUID actorId, UUID courseId) {
        if (!courseAccessPort.isOwnerOrCoTeacher(actorId, courseId)) {
            throw new BusinessException("NOT_COURSE_OWNER",
                    "Bạn không có quyền quản lý course này", HttpStatus.FORBIDDEN);
        }
    }

    private void assertStudentEnrolled(UUID studentId, UUID courseId) {
        if (!courseAccessPort.isStudentEnrolled(studentId, courseId)) {
            throw new BusinessException("CLASS_NOT_LINKED",
                    "Bạn chưa được thêm vào khóa học này", HttpStatus.FORBIDDEN);
        }
    }

    private void assertOwnAttempt(UUID studentId, AssessmentAttempt attempt) {
        if (!attempt.getStudentId().equals(studentId)) {
            throw new BusinessException("FORBIDDEN_NOT_SELF",
                    "Bạn không có quyền thao tác trên bài làm này", HttpStatus.FORBIDDEN);
        }
    }

    private Assessment loadAssessmentOrThrow(UUID assessmentId) {
        return assessmentRepository.findByIdAndDeletedAtIsNull(assessmentId)
                .orElseThrow(() -> new BusinessException("ASSESSMENT_NOT_FOUND",
                        "Không tìm thấy đề kiểm tra", HttpStatus.NOT_FOUND));
    }

    private AssessmentAttempt loadAttemptOrThrow(UUID attemptId) {
        return attemptRepository.findById(attemptId)
                .orElseThrow(() -> new BusinessException("ATTEMPT_NOT_FOUND",
                        "Không tìm thấy bài làm", HttpStatus.NOT_FOUND));
    }

    /**
     * So khớp studentAnswer với correctAnswer — chuẩn hóa cả 2 về Set<String> để
     * không phụ thuộc thứ tự phần tử (MULTIPLE_CHOICE có thể chọn nhiều đáp án).
     */
    boolean isAnswerCorrect(Object studentAnswer, Object correctAnswer) {
        Set<String> normalizedStudent = normalizeToStringSet(studentAnswer);
        Set<String> normalizedCorrect = normalizeToStringSet(correctAnswer);
        return !normalizedCorrect.isEmpty() && normalizedStudent.equals(normalizedCorrect);
    }

    Set<String> normalizeToStringSet(Object value) {
        Set<String> result = new HashSet<>();
        if (value == null) {
            return result;
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item != null) {
                    result.add(String.valueOf(item).trim());
                }
            }
        } else {
            result.add(String.valueOf(value).trim());
        }
        return result;
    }

    /**
     * Báo Module 5 khi attempt vừa chuyển sang GRADED (Module05 FINAL mục 4.2).
     * Gom điểm theo nhóm (lessonId, learningObjectiveId) của câu hỏi; câu không gắn lesson
     * rơi vào nhóm "chung" (cả hai null). totalAwarded/totalWeight cùng thang weight của câu hỏi.
     */
    private void doPublishAttemptEvidence(AssessmentAttempt attempt, UUID courseId) {
        Map<UUID, BigDecimal> weightByQuestion = assessmentQuestionRepository
                .findByAssessmentIdOrderByOrderIndexAsc(attempt.getAssessmentId()).stream()
                .collect(Collectors.toMap(AssessmentQuestion::getQuestionId,
                        AssessmentQuestion::getScoreWeight, (a, b) -> a));
        List<SubmissionAnswer> answers = answerRepository.findByAttemptId(attempt.getId());
        Map<UUID, Question> questionById = questionRepository
                .findAllById(answers.stream().map(SubmissionAnswer::getQuestionId).toList()).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        Map<GroupKey, BigDecimal[]> totals = new LinkedHashMap<>();
        for (SubmissionAnswer answer : answers) {
            Question question = questionById.get(answer.getQuestionId());
            BigDecimal weight = weightByQuestion.get(answer.getQuestionId());
            if (question == null || weight == null) {
                continue;
            }
            GroupKey key = new GroupKey(question.getLessonId(), question.getLearningObjectiveId());
            BigDecimal[] sum = totals.computeIfAbsent(key,
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal awarded = answer.getScoreAwarded() != null ? answer.getScoreAwarded() : BigDecimal.ZERO;
            sum[0] = sum[0].add(awarded);
            sum[1] = sum[1].add(weight);
        }

        List<LearningEvidencePort.ObjectiveGroupScore> groups = totals.entrySet().stream()
                .map(e -> new LearningEvidencePort.ObjectiveGroupScore(
                        e.getKey().lessonId(), e.getKey().learningObjectiveId(),
                        e.getValue()[0], e.getValue()[1]))
                .toList();
        OffsetDateTime occurredAt = attempt.getSubmittedAt() != null
                ? attempt.getSubmittedAt() : OffsetDateTime.now();
        int assessmentMaxAttempts = assessmentRepository.findById(attempt.getAssessmentId())
                .map(Assessment::getMaxAttempts).orElse(0);
        learningEvidencePort.onAttemptGraded(courseId, attempt.getStudentId(), attempt.getId(),
                attempt.getAssessmentId(),
                attempt.getAttemptNumber(), assessmentMaxAttempts, occurredAt, groups);
    }

    private static final org.slf4j.Logger EVIDENCE_LOG =
            org.slf4j.LoggerFactory.getLogger(AssessmentService.class);

    /**
     * Lỗi sinh evidence không được làm hỏng nghiệp vụ nộp/chấm bài (spec Module 5).
     * LearningEvidenceService chạy transaction riêng (REQUIRES_NEW) nên lỗi ở đó không
     * đánh dấu rollback transaction của bài làm.
     */
    private void publishAttemptEvidence(AssessmentAttempt attempt, UUID courseId) {
        try {
            doPublishAttemptEvidence(attempt, courseId);
        } catch (Exception e) {
            EVIDENCE_LOG.error("Sinh learning_evidence thất bại, bỏ qua: attempt={}, course={}",
                    attempt.getId(), courseId, e);
        }
    }

    private record GroupKey(UUID lessonId, UUID learningObjectiveId) {
    }

    private BigDecimal computeTotalScore(UUID attemptId) {
        return answerRepository.findByAttemptId(attemptId).stream()
                .map(SubmissionAnswer::getScoreAwarded)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private AssessmentResponse toResponse(Assessment a) {
        return new AssessmentResponse(
                a.getId(), a.getCourseId(), a.getTitle(), a.getDescription(), a.getDurationMinutes(),
                a.getMaxAttempts(), a.getBlueprintConfig(), a.getOpensAt(), a.getClosesAt(),
                a.getPublishStatus() != null ? a.getPublishStatus().name() : null,
                a.getPublishedAt(), a.getCreatedBy(), a.getCreatedAt(), a.getUpdatedAt(), a.getDeletedAt()
        );
    }

    private AssessmentQuestionResponse toAssessmentQuestionResponse(AssessmentQuestion aq) {
        return new AssessmentQuestionResponse(
                aq.getId(), aq.getAssessmentId(), aq.getQuestionId(), aq.getOrderIndex(), aq.getScoreWeight()
        );
    }

    private AssessmentAttemptResponse toAttemptResponse(AssessmentAttempt a) {
        return new AssessmentAttemptResponse(
                a.getId(), a.getAssessmentId(), a.getStudentId(), a.getAttemptNumber(),
                a.getStatus() != null ? a.getStatus().name() : null,
                a.getStartedAt(), a.getSubmittedAt(), a.getTotalScore()
        );
    }

    private AssessmentAttemptDetailResponse toAttemptDetailResponse(AssessmentAttempt a) {
        List<SubmissionAnswerResponse> answers = answerRepository.findByAttemptId(a.getId()).stream()
                .map(this::toAnswerResponse)
                .toList();
        return new AssessmentAttemptDetailResponse(
                a.getId(), a.getAssessmentId(), a.getStudentId(), a.getAttemptNumber(),
                a.getStatus() != null ? a.getStatus().name() : null,
                a.getStartedAt(), a.getSubmittedAt(), a.getTotalScore(), answers
        );
    }

    private SubmissionAnswerResponse toAnswerResponse(SubmissionAnswer s) {
        return new SubmissionAnswerResponse(
                s.getId(), s.getAttemptId(), s.getQuestionId(), s.getStudentAnswer(),
                s.getIsCorrect(), s.getScoreAwarded(), s.getTeacherFeedback()
        );
    }
}
