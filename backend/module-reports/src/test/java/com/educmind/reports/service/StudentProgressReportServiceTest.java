package com.educmind.reports.service;

import com.educmind.analytics.repository.KnowledgeGapRepository;
import com.educmind.analytics.repository.MasteryRepository;
import com.educmind.analytics.repository.RecommendationRepository;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.CourseAccessPort;
import com.educmind.reports.dto.StudentProgressReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentProgressReportServiceTest {

    @Mock MasteryRepository masteryRepository;
    @Mock KnowledgeGapRepository knowledgeGapRepository;
    @Mock RecommendationRepository recommendationRepository;
    @Mock CourseAccessPort courseAccessPort;

    StudentProgressReportService service;

    final UUID actor = UUID.randomUUID();
    final UUID student = UUID.randomUUID();
    final UUID course = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new StudentProgressReportService(masteryRepository, knowledgeGapRepository,
                recommendationRepository, courseAccessPort);
    }

    @Test
    void missingStudentIdIsRejected() {
        assertThrows(BusinessException.class, () -> service.generate(actor, null, course));
    }

    @Test
    void missingCourseIdIsRejected() {
        assertThrows(BusinessException.class, () -> service.generate(actor, student, null));
    }

    @Test
    void nonOwnerIsForbiddenAndNothingIsQueried() {
        when(courseAccessPort.isOwnerOrCoTeacher(actor, course)).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.generate(actor, student, course));
        verify(masteryRepository, never()).findByStudentIdAndCourseId(student, course);
    }

    @Test
    void ownerWithNoDataGetsEmptyListsWithIds() {
        when(courseAccessPort.isOwnerOrCoTeacher(actor, course)).thenReturn(true);
        when(masteryRepository.findByStudentIdAndCourseId(student, course)).thenReturn(List.of());
        when(knowledgeGapRepository.findByStudentIdAndCourseIdOrderByOpenedAtDesc(student, course))
                .thenReturn(List.of());
        when(recommendationRepository.findByStudentIdAndCourseIdOrderByCreatedAtDesc(student, course))
                .thenReturn(List.of());

        StudentProgressReport report = service.generate(actor, student, course);

        assertEquals(student, report.studentId());
        assertEquals(course, report.courseId());
        assertTrue(report.masteryByObjective().isEmpty());
        assertTrue(report.knowledgeGaps().isEmpty());
        assertTrue(report.recommendations().isEmpty());
    }
}
