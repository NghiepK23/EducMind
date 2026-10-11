package com.educmind.reports.service;

import com.educmind.assessment.entity.Assessment;
import com.educmind.assessment.entity.AssessmentAttempt;
import com.educmind.assessment.entity.AttemptStatus;
import com.educmind.assessment.repository.AssessmentAttemptRepository;
import com.educmind.assessment.repository.AssessmentRepository;
import com.educmind.assessment.repository.SubmissionAnswerRepository;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.CourseAccessPort;
import com.educmind.reports.dto.AssessmentResultReport;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class AssessmentResultReportService {

    private static final int HARDEST_LIMIT = 10;

    private final AssessmentRepository assessmentRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final SubmissionAnswerRepository answerRepository;
    private final CourseAccessPort courseAccessPort;

    public AssessmentResultReportService(AssessmentRepository assessmentRepository,
                                         AssessmentAttemptRepository attemptRepository,
                                         SubmissionAnswerRepository answerRepository,
                                         CourseAccessPort courseAccessPort) {
        this.assessmentRepository = assessmentRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.courseAccessPort = courseAccessPort;
    }

    @Transactional(readOnly = true)
    public AssessmentResultReport generate(UUID actorId, UUID assessmentId) {
        if (assessmentId == null) {
            throw new BusinessException("INVALID_FILTER",
                    "Cần truyền assessment_id", HttpStatus.BAD_REQUEST);
        }
        Assessment assessment = checkAccess(actorId, assessmentId);

        List<AssessmentAttempt> submitted = attemptRepository
                .findByAssessmentIdOrderBySubmittedAtDesc(assessmentId).stream()
                .filter(a -> a.getStatus() != AttemptStatus.IN_PROGRESS).toList();
        List<BigDecimal> scores = submitted.stream()
                .filter(a -> a.getStatus() == AttemptStatus.GRADED && a.getTotalScore() != null)
                .map(AssessmentAttempt::getTotalScore).toList();

        BigDecimal min = null;
        BigDecimal max = null;
        BigDecimal avg = null;
        if (!scores.isEmpty()) {
            min = scores.stream().min(Comparator.naturalOrder()).get();
            max = scores.stream().max(Comparator.naturalOrder()).get();
            BigDecimal sum = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            avg = sum.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
        }

        List<AssessmentResultReport.HardQuestion> hardest = List.of();
        if (!submitted.isEmpty()) {
            List<UUID> attemptIds = submitted.stream().map(AssessmentAttempt::getId).toList();
            hardest = answerRepository.summarizeByAttempts(attemptIds).stream()
                    .filter(r -> r.getAnsweredCount() != null && r.getAnsweredCount() > 0)
                    .map(r -> {
                        long answered = r.getAnsweredCount();
                        long incorrect = r.getIncorrectCount() == null ? 0 : r.getIncorrectCount();
                        double rate = Math.round(incorrect * 10000.0 / answered) / 10000.0;
                        return new AssessmentResultReport.HardQuestion(
                                r.getQuestionId(), answered, incorrect, rate);
                    })
                    .sorted(Comparator.comparingDouble(
                            AssessmentResultReport.HardQuestion::incorrectRate).reversed())
                    .limit(HARDEST_LIMIT)
                    .toList();
        }

        return new AssessmentResultReport(assessmentId, OffsetDateTime.now(),
                submitted.size(), scores.size(), min, avg, max, hardest);
    }

    public Assessment checkAccess(UUID actorId, UUID assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .filter(a -> a.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException("ASSESSMENT_NOT_FOUND",
                        "Đề kiểm tra không tồn tại", HttpStatus.NOT_FOUND));
        if (!courseAccessPort.isOwnerOrCoTeacher(actorId, assessment.getCourseId())) {
            throw new BusinessException("REPORT_SCOPE_FORBIDDEN",
                    "Chỉ owner hoặc đồng giảng dạy của course mới được xem báo cáo",
                    HttpStatus.FORBIDDEN);
        }
        return assessment;
    }
}
