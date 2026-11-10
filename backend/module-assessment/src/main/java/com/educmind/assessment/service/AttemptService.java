package com.educmind.assessment.service;

import com.educmind.assessment.dto.AttemptQuestionView;
import com.educmind.assessment.dto.AttemptResponse;
import com.educmind.assessment.dto.MyAttemptRow;
import com.educmind.assessment.dto.SaveAnswerRequest;
import com.educmind.assessment.entity.Assessment;
import com.educmind.assessment.entity.AssessmentAttempt;
import com.educmind.assessment.entity.AssessmentQuestion;
import com.educmind.assessment.entity.AttemptStatus;
import com.educmind.assessment.entity.Question;
import com.educmind.assessment.entity.SubmissionAnswer;
import com.educmind.assessment.repository.AssessmentAttemptRepository;
import com.educmind.assessment.repository.AssessmentQuestionRepository;
import com.educmind.assessment.repository.AssessmentRepository;
import com.educmind.assessment.repository.QuestionRepository;
import com.educmind.assessment.repository.SubmissionAnswerRepository;
import com.educmind.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * US-S03 — làm bài kiểm tra trực tuyến.
 * AC-S03.1 chặn ngoài thời gian / hết lượt, AC-S03.2 mở lại lượt đang dở,
 * AC-S03.3 lưu câu trả lời + tự nộp khi hết giờ.
 */
@Service
public class AttemptService {

    private final AssessmentPermission permission;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final QuestionRepository questionRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final SubmissionAnswerRepository answerRepository;
    private final AttemptFinalizer finalizer;
    private final JsonCodec codec;

    public AttemptService(AssessmentPermission permission,
                          AssessmentRepository assessmentRepository,
                          AssessmentQuestionRepository assessmentQuestionRepository,
                          QuestionRepository questionRepository,
                          AssessmentAttemptRepository attemptRepository,
                          SubmissionAnswerRepository answerRepository,
                          AttemptFinalizer finalizer,
                          JsonCodec codec) {
        this.permission = permission;
        this.assessmentRepository = assessmentRepository;
        this.assessmentQuestionRepository = assessmentQuestionRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.finalizer = finalizer;
        this.codec = codec;
    }

    /** AC-S03.1 + AC-S03.2. */
    @Transactional
    public AttemptResponse startOrResume(UUID studentId, UUID assessmentId) {
        Assessment assessment = permission.requireVisibleAssessment(studentId, "STUDENT", assessmentId);
        permission.requirePublishedForStudent(assessment);

        OffsetDateTime now = OffsetDateTime.now();
        if (now.isBefore(assessment.getOpenAt())) {
            throw new BusinessException("ASSESSMENT_NOT_OPEN",
                    "Bài kiểm tra chưa mở", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (now.isAfter(assessment.getCloseAt())) {
            throw new BusinessException("ASSESSMENT_CLOSED",
                    "Bài kiểm tra đã đóng", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        var inProgress = attemptRepository.findByAssessmentIdAndStudentIdAndStatus(
                assessmentId, studentId, AttemptStatus.IN_PROGRESS);
        if (inProgress.isPresent()) {
            AssessmentAttempt attempt = inProgress.get();
            maybeAutoSubmit(attempt, assessment);
            return buildResponse(attempt, assessment);
        }

        long used = attemptRepository.countByAssessmentIdAndStudentId(assessmentId, studentId);
        if (used >= assessment.getMaxAttempts()) {
            throw new BusinessException("MAX_ATTEMPTS_REACHED",
                    "Bạn đã dùng hết " + assessment.getMaxAttempts() + " lần làm bài",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        AssessmentAttempt attempt = attemptRepository.save(
                new AssessmentAttempt(assessmentId, studentId, (int) used + 1));
        return buildResponse(attempt, assessment);
    }

    @Transactional
    public AttemptResponse getAttempt(UUID studentId, UUID attemptId) {
        AssessmentAttempt attempt = requireOwnAttempt(studentId, attemptId);
        Assessment assessment = assessmentRepository.findByIdAndDeletedAtIsNull(attempt.getAssessmentId())
                .orElseThrow(() -> permission.notFound("Không tìm thấy bài kiểm tra"));
        maybeAutoSubmit(attempt, assessment);
        return buildResponse(attempt, assessment);
    }

    /** AC-S03.3: lưu câu trả lời trong lúc làm bài (ghi đè nếu đã có). */
    @Transactional
    public AttemptResponse saveAnswer(UUID studentId, UUID attemptId, UUID questionId,
                                      SaveAnswerRequest request) {
        AssessmentAttempt attempt = requireOwnAttempt(studentId, attemptId);
        Assessment assessment = assessmentRepository.findByIdAndDeletedAtIsNull(attempt.getAssessmentId())
                .orElseThrow(() -> permission.notFound("Không tìm thấy bài kiểm tra"));

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BusinessException("ATTEMPT_NOT_EDITABLE",
                    "Lượt làm này đã được nộp, không thể sửa câu trả lời", HttpStatus.CONFLICT);
        }
        if (OffsetDateTime.now().isAfter(attempt.deadline(assessment.getDurationMinutes()))) {
            maybeAutoSubmit(attempt, assessment);
            throw new BusinessException("ATTEMPT_EXPIRED",
                    "Đã hết thời gian làm bài, bài đã được nộp tự động", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        AssessmentQuestion link = null;
        for (AssessmentQuestion item : assessmentQuestionRepository
                .findByAssessmentIdOrderByIdAsc(assessment.getId())) {
            if (item.getQuestionId().equals(questionId)) {
                link = item;
                break;
            }
        }
        if (link == null) {
            throw new BusinessException("QUESTION_NOT_IN_ASSESSMENT",
                    "Câu hỏi không thuộc bài kiểm tra này", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> permission.notFound("Không tìm thấy câu hỏi"));

        String payload;
        if (question.getType().isChoice()) {
            List<Integer> selected = request.getSelectedOptionIndexes() == null
                    ? List.of()
                    : request.getSelectedOptionIndexes();
            int optionCount = codec.readStringList(question.getOptions()).size();
            for (Integer index : selected) {
                if (index == null || index < 0 || index >= optionCount) {
                    throw new BusinessException("ANSWER_INVALID",
                            "Phương án đã chọn không hợp lệ", HttpStatus.UNPROCESSABLE_ENTITY);
                }
            }
            payload = codec.write(selected);
        } else {
            payload = request.getText() == null ? null : codec.writeText(request.getText());
        }

        SubmissionAnswer answer = answerRepository.findByAttemptIdAndQuestionId(attemptId, questionId)
                .orElseGet(() -> new SubmissionAnswer(attemptId, questionId, payload));
        answer.updateContent(payload);
        answerRepository.save(answer);

        return buildResponse(attempt, assessment);
    }

    /** AC-S03.3/04: nộp bài; trắc nghiệm chấm tự động, tự luận chờ giáo viên. */
    @Transactional
    public AttemptResponse submit(UUID studentId, UUID attemptId) {
        AssessmentAttempt attempt = requireOwnAttempt(studentId, attemptId);
        Assessment assessment = assessmentRepository.findByIdAndDeletedAtIsNull(attempt.getAssessmentId())
                .orElseThrow(() -> permission.notFound("Không tìm thấy bài kiểm tra"));

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BusinessException("ATTEMPT_ALREADY_SUBMITTED",
                    "Lượt làm này đã được nộp", HttpStatus.CONFLICT);
        }

        finalizer.ensureAnswersExist(attemptId, assessment.getId());
        finalizer.autoGradeChoiceAnswers(attemptId, assessment.getId());
        finalizer.finalizeIfComplete(attempt, assessment);
        attemptRepository.save(attempt);

        return buildResponse(attempt, assessment);
    }

    /** AC-S04.2/04.3: học sinh xem các lượt làm của chính mình. */
    @Transactional(readOnly = true)
    public List<MyAttemptRow> myAttempts(UUID studentId, UUID assessmentId) {
        Assessment assessment = permission.requireVisibleAssessment(studentId, "STUDENT", assessmentId);
        var maxScore = maxScoreOf(assessment.getId());

        List<MyAttemptRow> result = new ArrayList<>();
        for (AssessmentAttempt attempt : attemptRepository
                .findByAssessmentIdAndStudentIdOrderByAttemptNumberAsc(assessmentId, studentId)) {
            boolean graded = attempt.getStatus() == AttemptStatus.GRADED;
            result.add(new MyAttemptRow(attempt.getId(), attempt.getAttemptNumber(),
                    attempt.getStatus().name(), graded ? attempt.getTotalScore() : null,
                    maxScore, attempt.getStartedAt(), attempt.getSubmittedAt()));
        }
        return result;
    }

    private java.math.BigDecimal maxScoreOf(UUID assessmentId) {
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (AssessmentQuestion link : assessmentQuestionRepository.findByAssessmentIdOrderByIdAsc(assessmentId)) {
            total = total.add(link.getWeight());
        }
        return total;
    }

    private AssessmentAttempt requireOwnAttempt(UUID studentId, UUID attemptId) {
        return attemptRepository.findByIdAndStudentId(attemptId, studentId)
                .orElseThrow(() -> new BusinessException("ATTEMPT_NOT_FOUND",
                        "Không tìm thấy lượt làm bài", HttpStatus.NOT_FOUND));
    }

    private void maybeAutoSubmit(AssessmentAttempt attempt, Assessment assessment) {
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            return;
        }
        OffsetDateTime deadline = attempt.deadline(assessment.getDurationMinutes());
        if (!OffsetDateTime.now().isAfter(deadline)) {
            return;
        }
        finalizer.ensureAnswersExist(attempt.getId(), assessment.getId());
        finalizer.autoGradeChoiceAnswers(attempt.getId(), assessment.getId());
        finalizer.finalizeIfComplete(attempt, assessment);
        attemptRepository.save(attempt);
    }

    AttemptResponse buildResponse(AssessmentAttempt attempt, Assessment assessment) {
        Map<UUID, SubmissionAnswer> answers = new HashMap<>();
        for (SubmissionAnswer answer : answerRepository.findByAttemptIdOrderByQuestionIdAsc(attempt.getId())) {
            answers.put(answer.getQuestionId(), answer);
        }

        boolean graded = attempt.getStatus() == AttemptStatus.GRADED;
        List<AttemptQuestionView> questions = new ArrayList<>();
        java.math.BigDecimal maxScore = java.math.BigDecimal.ZERO;

        for (AssessmentQuestion link : assessmentQuestionRepository
                .findByAssessmentIdOrderByIdAsc(assessment.getId())) {
            maxScore = maxScore.add(link.getWeight());
            Question question = questionRepository.findById(link.getQuestionId()).orElse(null);
            if (question == null) {
                continue;
            }
            SubmissionAnswer answer = answers.get(link.getQuestionId());
            questions.add(new AttemptQuestionView(question.getId(), question.getContent(),
                    question.getType().name(), codec.readStringList(question.getOptions()), link.getWeight(),
                    answer == null ? List.of() : codec.readIntList(answer.getAnswerContent()),
                    answer == null ? null : codec.readText(answer.getAnswerContent()),
                    graded && answer != null ? answer.getScore() : null,
                    graded && answer != null ? answer.getFeedback() : null));
        }

        return new AttemptResponse(attempt.getId(), assessment.getId(), assessment.getTitle(),
                attempt.getAttemptNumber(), attempt.getStatus().name(), attempt.getStartedAt(),
                attempt.deadline(assessment.getDurationMinutes()), OffsetDateTime.now(), maxScore,
                graded ? attempt.getTotalScore() : null,
                attempt.getStatus() == AttemptStatus.IN_PROGRESS, questions);
    }
}
