package com.educmind.reports.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.reports.entity.ReportExportRequest;
import com.educmind.reports.repository.ReportExportRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReportExportServiceTest {

    private ReportExportRequestRepository repository;
    private ReportExportWorker worker;
    private ClassProgressReportService classService;
    private StudentProgressReportService studentService;
    private AssessmentResultReportService assessmentService;
    private ReportExportService service;

    private final UUID actor = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(ReportExportRequestRepository.class);
        worker = mock(ReportExportWorker.class);
        classService = mock(ClassProgressReportService.class);
        studentService = mock(StudentProgressReportService.class);
        assessmentService = mock(AssessmentResultReportService.class);
        service = new ReportExportService(repository, worker, mock(ExportFileStorage.class),
                classService, studentService, assessmentService);
        when(repository.save(any(ReportExportRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private Map<String, Object> body(String type, String format, Map<String, Object> scope) {
        return Map.of("report_type", type, "format", format, "scope_params", scope);
    }

    private BusinessException forbidden() {
        return new BusinessException("REPORT_SCOPE_FORBIDDEN", "Không có quyền", HttpStatus.FORBIDDEN);
    }

    @Test
    void classProgress_noAccess_throwsAndNeverSavesOrRunsWorker() {
        BusinessException ex = forbidden();
        doThrow(ex).when(classService).checkAccess(actor, courseId);

        BusinessException thrown = assertThrows(BusinessException.class, () ->
                service.create(actor, body("CLASS_PROGRESS", "CSV",
                        Map.of("course_id", courseId.toString()))));

        assertSame(ex, thrown);
        verify(repository, never()).save(any());
        verify(worker, never()).process(any());
    }

    @Test
    void studentProgress_noAccess_throwsAndNeverSavesOrRunsWorker() {
        BusinessException ex = forbidden();
        doThrow(ex).when(studentService).checkAccess(actor, courseId);

        BusinessException thrown = assertThrows(BusinessException.class, () ->
                service.create(actor, body("STUDENT_PROGRESS", "CSV",
                        Map.of("student_id", studentId.toString(),
                                "course_id", courseId.toString()))));

        assertSame(ex, thrown);
        verify(repository, never()).save(any());
        verify(worker, never()).process(any());
    }

    @Test
    void assessmentResult_noAccess_throwsAndNeverSavesOrRunsWorker() {
        BusinessException ex = forbidden();
        doThrow(ex).when(assessmentService).checkAccess(actor, assessmentId);

        BusinessException thrown = assertThrows(BusinessException.class, () ->
                service.create(actor, body("ASSESSMENT_RESULT", "CSV",
                        Map.of("assessment_id", assessmentId.toString()))));

        assertSame(ex, thrown);
        verify(repository, never()).save(any());
        verify(worker, never()).process(any());
    }

    @Test
    void classProgress_withAccess_savesAndRunsWorker() {
        service.create(actor, body("CLASS_PROGRESS", "CSV",
                Map.of("course_id", courseId.toString())));

        verify(classService).checkAccess(actor, courseId);
        verify(repository).save(any(ReportExportRequest.class));
        verify(worker).process(any());
    }

    @Test
    void unsupportedFormat_rejectedBeforeAnyAccessCheck() {
        assertThrows(BusinessException.class, () ->
                service.create(actor, body("CLASS_PROGRESS", "XLSX",
                        Map.of("course_id", courseId.toString()))));

        verifyNoInteractions(classService, studentService, assessmentService, worker);
        verify(repository, never()).save(any());
    }

    @Test
    void studentProgress_missingStudentId_rejectedBeforeAnyAccessCheck() {
        assertThrows(BusinessException.class, () ->
                service.create(actor, body("STUDENT_PROGRESS", "CSV",
                        Map.of("course_id", courseId.toString()))));

        verifyNoInteractions(classService, studentService, assessmentService, worker);
        verify(repository, never()).save(any());
    }

    @Test
    void pdfFormat_isAccepted() {
        service.create(actor, body("CLASS_PROGRESS", "PDF",
                Map.of("course_id", courseId.toString())));

        verify(classService).checkAccess(actor, courseId);
        verify(repository).save(any(ReportExportRequest.class));
        verify(worker).process(any());
    }
}
