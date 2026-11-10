package com.educmind.assessment.service;

import com.educmind.assessment.dto.AttemptAnswerDetail;
import com.educmind.assessment.dto.AttemptDetailResponse;
import com.educmind.assessment.dto.GradeAnswerRequest;
import com.educmind.assessment.dto.StudentResultRow;
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
import com.educmind.common.port.ClassAccessPort;
import com.educmind.common.port.CourseAccessPort;
import com.educmind.common.port.UserLookupPort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * US-T14 — xem danh sách lượt làm & kết quả; US-T15 — chấm tay.
 * AC-T14.1 đủ học sinh, AC-T14.2 điểm chính thức = lượt đã chấm cao nhất,
 * AC-T14.3 lọc lượt chờ chấm, AC-T15.1-4 chấm từng câu và tổng kết.
 */
@Service
public class AttemptGradingService {

    private final AssessmentPermission permission;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final SubmissionAnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final AttemptFinalizer finalizer;
    private final ClassAccessPort classAccessPort;
    private final CourseAccessPort courseAccessPort;
    private final UserLookupPort userLookupPort;
    private final JsonCodec codec;

    public AttemptGradingService(AssessmentPermission permission,
                                 AssessmentRepository assessmentRepository,
                                 AssessmentQuestionRepository assessmentQuestionRepository,
                                 AssessmentAttemptRepository attemptRepository,
                                 SubmissionAnswerRepository answerRepository,
                                 QuestionRepository questionRepository,
                                 AttemptFinalizer finalizer,
                                 ClassAccessPort classAccessPort,
                                 CourseAccessPort courseAccessPort,
                                 UserLookupPort userLookupPort,
                                 JsonCodec codec) {
        this.permission = permission;
        this.assessmentRepository = assessmentRepository;
        this.assessmentQuestionRepository = assessmentQuestionRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.finalizer = finalizer;
        this.classAccessPort = classAccessPort;
        this.courseAccessPort = courseAccessPort;
        this.userLookupPort = userLookupPort;
        this.codec = codec;
    }

    @Transactional(readOnly = true)
    public List<StudentResultRow> results(UUID teacherId, UUID assessmentId) {
        Assessment assessment = permission.requireManagedAssessment(teacherId, assessmentId);
        BigDecimal maxScore = maxScore(assessmentId);

        UUID classId = courseAccessPort.findClassId(assessment.getCourseId())
                .orElseThrow(() -> permission.notFound("Không tìm thấy lớp của khóa học"));
        List<UUID> studentIds = classAccessPort.findStudentIds(classId);

        Map<UUID, List<AssessmentAttempt>> byStudent = new HashMap<>();
        for (AssessmentAttempt attempt : attemptRepository.findByAssessmentIdOrderByStartedAtAsc(assessmentId)) {
            byStudent.computeIfAbsent(attempt.getStudentId(), k -> new ArrayList<>()).add(attempt);
        }

        List<StudentResultRow> rows = new ArrayList<>();
        for (UserLookupPort.UserDetail detail : userLookupPort.findUserDetails(studentIds)) {
            List<AssessmentAttempt> attempts = byStudent.getOrDefault(detail.id(), List.of());
            long gradedCount = attempts.stream().filter(a -> a.getStatus() == AttemptStatus.GRADED).count();
            long pendingCount = attempts.stream().filter(a -> a.getStatus() == AttemptStatus.SUBMITTED).count();

            // AC-T14.2: điểm chính thức = điểm cao nhất trong các lượt đã chấm
            BigDecimal best = null;
            for (AssessmentAttempt attempt : attempts) {
                if (attempt.getStatus() == AttemptStatus.GRADED && attempt.getTotalScore() != null) {
                    if (best == null || attempt.getTotalScore().compareTo(best) > 0) {
                        best = attempt.getTotalScore();
                    }
                }
            }
            rows.add(new StudentResultRow(detail.id(), detail.fullName(), detail.email(),
                    attempts.size(), gradedCount, pendingCount, best, maxScore));
        }
        return rows;
    }

    /** Danh sách lượt làm cho giáo viên (AC-T14.3): lọc theo trạng thái. */
    @Transactional(readOnly = true)
    public List<AttemptDetailResponse> attempts(UUID teacherId, UUID assessmentId, String status) {
        permission.requireManagedAssessment(teacherId, assessmentId);

        List<AssessmentAttempt> attempts;
        if (status == null || status.isBlank()) {
            attempts = attemptRepository.findByAssessmentIdOrderByStartedAtAsc(assessmentId);
        } else {
            AttemptStatus filter;
            try {
                filter = AttemptStatus.valueOf(status.trim().toUpperCase());
            } catch (RuntimeException e) {
                throw new BusinessException("INVALID_FILTER",
                        "Trạng thái không hợp lệ: " + status, HttpStatus.BAD_REQUEST);
            }
            attempts = attemptRepository.findByAssessmentIdAndStatusOrderByStartedAtAsc(assessmentId, filter);
        }

        List<AttemptDetailResponse> result = new ArrayList<>();
        for (AssessmentAttempt attempt : attempts) {
            result.add(detail(attempt));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public AttemptDetailResponse attemptDetail(UUID teacherId, UUID attemptId) {
        AssessmentAttempt attempt = requireAttempt(attemptId);
        permission.requireManagedAssessment(teacherId, attempt.getAssessmentId());
        return detail(attempt);
    }

    /** AC-T15.1 … AC-T15.4. */
    @Transactional
    public AttemptDetailResponse gradeAnswer(UUID teacherId, UUID attemptId, UUID questionId,
                                             GradeAnswerRequest request) {
        AssessmentAttempt attempt = requireAttempt(attemptId);
        Assessment assessment = permission.requireManagedAssessment(teacherId, attempt.getAssessmentId());

        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            throw new BusinessException("ATTEMPT_NOT_SUBMITTED",
                    "Học sinh chưa nộp bài, không thể chấm", HttpStatus.CONFLICT);
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

        BigDecimal score = request.getScore();
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(link.getWeight()) > 0) {
            throw new BusinessException("SCORE_OUT_OF_RANGE",
                    "Điểm câu hỏi phải nằm trong khoảng 0 đến " + link.getWeight().toPlainString(),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        SubmissionAnswer answer = answerRepository.findByAttemptIdAndQuestionId(attemptId, questionId)
                .orElseThrow(() -> permission.notFound("Không tìm thấy câu trả lời"));

        answer.manualGrade(score.setScale(2, RoundingMode.HALF_UP), request.getFeedback(),
                teacherId, OffsetDateTime.now());
        answerRepository.save(answer);

        finalizer.finalizeIfComplete(attempt, assessment);
        attemptRepository.save(attempt);

        return detail(attempt);
    }

    private AssessmentAttempt requireAttempt(UUID attemptId) {
        return attemptRepository.findById(attemptId)
                .orElseThrow(() -> permission.notFound("Không tìm thấy lượt làm bài"));
    }

    private BigDecimal maxScore(UUID assessmentId) {
        BigDecimal total = BigDecimal.ZERO;
        for (AssessmentQuestion link : assessmentQuestionRepository.findByAssessmentIdOrderByIdAsc(assessmentId)) {
            total = total.add(link.getWeight());
        }
        return total;
    }

    private AttemptDetailResponse detail(AssessmentAttempt attempt) {
        Map<UUID, SubmissionAnswer> answers = new HashMap<>();
        for (SubmissionAnswer answer : answerRepository.findByAttemptIdOrderByQuestionIdAsc(attempt.getId())) {
            answers.put(answer.getQuestionId(), answer);
        }

        String studentName = userLookupPort.findUserDetails(List.of(attempt.getStudentId()))
                .stream().findFirst().map(UserLookupPort.UserDetail::fullName).orElse("");

        List<AttemptAnswerDetail> items = new ArrayList<>();
        BigDecimal maxScore = BigDecimal.ZERO;
        for (AssessmentQuestion link : assessmentQuestionRepository
                .findByAssessmentIdOrderByIdAsc(attempt.getAssessmentId())) {
            maxScore = maxScore.add(link.getWeight());
            Question question = questionRepository.findById(link.getQuestionId()).orElse(null);
            if (question == null) {
                continue;
            }
            SubmissionAnswer answer = answers.get(link.getQuestionId());
            items.add(new AttemptAnswerDetail(question.getId(), question.getContent(),
                    question.getType().name(), codec.readStringList(question.getOptions()),
                    codec.readIntList(question.getCorrectAnswer()), codec.readText(question.getCorrectAnswer()),
                    link.getWeight(),
                    answer == null ? List.of() : codec.readIntList(answer.getAnswerContent()),
                    answer == null ? null : codec.readText(answer.getAnswerContent()),
                    answer == null ? null : answer.getScore(),
                    answer == null ? null : answer.getFeedback()));
        }

        return new AttemptDetailResponse(attempt.getId(), attempt.getAssessmentId(), attempt.getStudentId(),
                studentName, attempt.getAttemptNumber(), attempt.getStatus().name(),
                attempt.getTotalScore(), maxScore, attempt.getStartedAt(), attempt.getSubmittedAt(), items);
    }
}
