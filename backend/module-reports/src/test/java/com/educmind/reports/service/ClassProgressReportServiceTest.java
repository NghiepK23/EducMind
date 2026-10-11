package com.educmind.reports.service;

import com.educmind.analytics.dto.MasteryOverviewResponse;
import com.educmind.analytics.repository.KnowledgeGapRepository;
import com.educmind.analytics.service.TeacherMasteryOverviewQueryService;
import com.educmind.assignment.entity.Assignment;
import com.educmind.assignment.repository.AssignmentRepository;
import com.educmind.assignment.repository.AssignmentSubmissionRepository;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.CourseAccessPort;
import com.educmind.reports.dto.AssignmentCompletionItem;
import com.educmind.reports.dto.ClassProgressReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassProgressReportServiceTest {

    @Mock TeacherMasteryOverviewQueryService overviewService;
    @Mock KnowledgeGapRepository knowledgeGapRepository;
    @Mock CourseAccessPort courseAccessPort;
    @Mock AssignmentRepository assignmentRepository;
    @Mock AssignmentSubmissionRepository submissionRepository;
    @Mock MasteryOverviewResponse overview;
    @Mock Assignment assignment;

    ClassProgressReportService service;

    final UUID actor = UUID.randomUUID();
    final UUID course = UUID.randomUUID();
    final UUID classId = UUID.randomUUID();
    final UUID assignmentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ClassProgressReportService(overviewService, knowledgeGapRepository,
                courseAccessPort, assignmentRepository, submissionRepository);
    }

    @Test
    void rejectsActorWhoIsNotOwnerOrCoTeacher() {
        when(courseAccessPort.isOwnerOrCoTeacher(actor, course)).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.generate(actor, course, null));
        verify(overviewService, never()).find(any(), any(), any());
    }

    @Test
    void computesCompletionRateOnlyForStudentsInScope() {
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();
        UUID outsider = UUID.randomUUID();
        when(courseAccessPort.isOwnerOrCoTeacher(actor, course)).thenReturn(true);
        when(overviewService.find(actor, course, classId)).thenReturn(overview);
        when(courseAccessPort.findActiveStudentIdsByCourseAndSourceClass(course, classId))
                .thenReturn(List.of(s1, s2));
        when(knowledgeGapRepository.countByCourseIdAndStudentIdInAndStatus(
                course, List.of(s1, s2), "OPEN")).thenReturn(2L);
        when(assignmentRepository
                .findByCourseIdAndPublishStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                        course, "PUBLISHED")).thenReturn(List.of(assignment));
        when(assignment.getId()).thenReturn(assignmentId);
        when(assignment.getTitle()).thenReturn("Bài 1");
        when(submissionRepository.findSubmittedStudentIds(assignmentId))
                .thenReturn(List.of(s1, outsider));

        ClassProgressReport report = service.generate(actor, course, classId);

        assertEquals(2L, report.openKnowledgeGapsCount());
        AssignmentCompletionItem item = report.assignmentCompletion().get(0);
        assertEquals(2, item.studentCount());
        assertEquals(1, item.submittedCount());
        assertEquals(0.5, item.completionRate());
        assertEquals("Bài 1", item.title());
    }

    @Test
    void wholeCourseUsesAllActiveStudentsAndCourseWideGapCount() {
        UUID s1 = UUID.randomUUID();
        when(courseAccessPort.isOwnerOrCoTeacher(actor, course)).thenReturn(true);
        when(overviewService.find(actor, course, null)).thenReturn(overview);
        when(courseAccessPort.findActiveStudentIdsByCourse(course)).thenReturn(List.of(s1));
        when(knowledgeGapRepository.countByCourseIdAndStatus(course, "OPEN")).thenReturn(3L);
        when(assignmentRepository
                .findByCourseIdAndPublishStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                        course, "PUBLISHED")).thenReturn(List.of());

        ClassProgressReport report = service.generate(actor, course, null);

        assertEquals(3L, report.openKnowledgeGapsCount());
        assertEquals(0, report.assignmentCompletion().size());
    }

    @Test
    void noStudentsGivesZeroRateAndZeroGapsWithoutQueryingGaps() {
        when(courseAccessPort.isOwnerOrCoTeacher(actor, course)).thenReturn(true);
        when(overviewService.find(actor, course, null)).thenReturn(overview);
        when(courseAccessPort.findActiveStudentIdsByCourse(course)).thenReturn(List.of());
        when(assignmentRepository
                .findByCourseIdAndPublishStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                        course, "PUBLISHED")).thenReturn(List.of(assignment));
        when(assignment.getId()).thenReturn(assignmentId);
        when(submissionRepository.findSubmittedStudentIds(assignmentId)).thenReturn(List.of());

        ClassProgressReport report = service.generate(actor, course, null);

        assertEquals(0L, report.openKnowledgeGapsCount());
        assertEquals(0.0, report.assignmentCompletion().get(0).completionRate());
        verify(knowledgeGapRepository, never()).countByCourseIdAndStatus(any(), any());
    }
}
