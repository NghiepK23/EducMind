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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentResultReportServiceTest {

    @Mock AssessmentRepository assessmentRepository;
    @Mock AssessmentAttemptRepository attemptRepository;
    @Mock SubmissionAnswerRepository answerRepository;
    @Mock CourseAccessPort courseAccessPort;
    @Mock Assessment assessment;

    AssessmentResultReportService service;

    final UUID actor = UUID.randomUUID();
    final UUID assessmentId = UUID.randomUUID();
    final UUID courseId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AssessmentResultReportService(assessmentRepository, attemptRepository,
                answerRepository, courseAccessPort);
    }

    private AssessmentAttempt attempt(AttemptStatus status) {
        AssessmentAttempt a = org.mockito.Mockito.mock(AssessmentAttempt.class);
        when(a.getStatus()).thenReturn(status);
        return a;
    }

    private SubmissionAnswerRepository.QuestionStatRow row(UUID q, Long answered, Long incorrect) {
        SubmissionAnswerRepository.QuestionStatRow r =
                org.mockito.Mockito.mock(SubmissionAnswerRepository.QuestionStatRow.class);
        when(r.getAnsweredCount()).thenReturn(answered);
        if (answered != null && answered > 0) {
            when(r.getQuestionId()).thenReturn(q);
            when(r.getIncorrectCount()).thenReturn(incorrect);
        }
        return r;
    }

    private void grantAccess() {
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessment.getCourseId()).thenReturn(courseId);
        when(courseAccessPort.isOwnerOrCoTeacher(actor, courseId)).thenReturn(true);
    }

    @Test
    void nullAssessmentIdIsRejected() {
        assertThrows(BusinessException.class, () -> service.generate(actor, null));
    }

    @Test
    void unknownAssessmentIsRejected() {
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.generate(actor, assessmentId));
    }

    @Test
    void deletedAssessmentIsRejected() {
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessment.getDeletedAt()).thenReturn(OffsetDateTime.now());
        assertThrows(BusinessException.class, () -> service.generate(actor, assessmentId));
    }

    @Test
    void nonOwnerIsForbiddenAndNothingIsQueried() {
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessment.getCourseId()).thenReturn(courseId);
        when(courseAccessPort.isOwnerOrCoTeacher(actor, courseId)).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.generate(actor, assessmentId));
        verify(attemptRepository, never()).findByAssessmentIdOrderBySubmittedAtDesc(any());
    }

    @Test
    void statsUseOnlyGradedAttemptsAndHardestSortedByIncorrectRate() {
        grantAccess();
        AssessmentAttempt a1 = attempt(AttemptStatus.GRADED);
        when(a1.getTotalScore()).thenReturn(new BigDecimal("4.00"));
        when(a1.getId()).thenReturn(UUID.randomUUID());
        AssessmentAttempt a2 = attempt(AttemptStatus.GRADED);
        when(a2.getTotalScore()).thenReturn(new BigDecimal("8.00"));
        when(a2.getId()).thenReturn(UUID.randomUUID());
        AssessmentAttempt a3 = attempt(AttemptStatus.SUBMITTED);
        when(a3.getId()).thenReturn(UUID.randomUUID());
        AssessmentAttempt a4 = attempt(AttemptStatus.IN_PROGRESS);
        when(attemptRepository.findByAssessmentIdOrderBySubmittedAtDesc(assessmentId))
                .thenReturn(List.of(a1, a2, a3, a4));

        UUID q1 = UUID.randomUUID();
        UUID q2 = UUID.randomUUID();
        UUID q3 = UUID.randomUUID();
        UUID q4 = UUID.randomUUID();
        List<SubmissionAnswerRepository.QuestionStatRow> rows = List.of(
                row(q1, 4L, 1L),
                row(q2, 4L, 3L),
                row(q3, 0L, 0L),
                row(q4, 2L, null));
        when(answerRepository.summarizeByAttempts(any())).thenReturn(rows);

        AssessmentResultReport report = service.generate(actor, assessmentId);

        assertEquals(3, report.totalAttempts());
        assertEquals(2, report.gradedAttempts());
        assertEquals(0, new BigDecimal("4.00").compareTo(report.minScore()));
        assertEquals(0, new BigDecimal("8.00").compareTo(report.maxScore()));
        assertEquals(0, new BigDecimal("6.00").compareTo(report.avgScore()));

        List<AssessmentResultReport.HardQuestion> hardest = report.hardestQuestions();
        assertEquals(3, hardest.size());
        assertEquals(q2, hardest.get(0).questionId());
        assertEquals(0.75, hardest.get(0).incorrectRate());
        assertEquals(q1, hardest.get(1).questionId());
        assertEquals(0.25, hardest.get(1).incorrectRate());
        assertEquals(q4, hardest.get(2).questionId());
        assertEquals(0.0, hardest.get(2).incorrectRate());
    }

    @Test
    void onlyInProgressAttemptsGivesEmptyStatsAndSkipsAnswerQuery() {
        grantAccess();
        AssessmentAttempt a = attempt(AttemptStatus.IN_PROGRESS);
        when(attemptRepository.findByAssessmentIdOrderBySubmittedAtDesc(assessmentId))
                .thenReturn(List.of(a));

        AssessmentResultReport report = service.generate(actor, assessmentId);

        assertEquals(0, report.totalAttempts());
        assertEquals(0, report.gradedAttempts());
        assertNull(report.minScore());
        assertNull(report.avgScore());
        assertNull(report.maxScore());
        assertTrue(report.hardestQuestions().isEmpty());
        verify(answerRepository, never()).summarizeByAttempts(any());
    }
}
