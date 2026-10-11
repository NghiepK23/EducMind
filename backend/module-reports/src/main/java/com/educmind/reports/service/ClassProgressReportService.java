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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ClassProgressReportService {

    private final TeacherMasteryOverviewQueryService overviewService;
    private final KnowledgeGapRepository knowledgeGapRepository;
    private final CourseAccessPort courseAccessPort;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;

    public ClassProgressReportService(TeacherMasteryOverviewQueryService overviewService,
                                      KnowledgeGapRepository knowledgeGapRepository,
                                      CourseAccessPort courseAccessPort,
                                      AssignmentRepository assignmentRepository,
                                      AssignmentSubmissionRepository submissionRepository) {
        this.overviewService = overviewService;
        this.knowledgeGapRepository = knowledgeGapRepository;
        this.courseAccessPort = courseAccessPort;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
    }

    @Transactional(readOnly = true)
    public ClassProgressReport generate(UUID actorId, UUID courseId, UUID classId) {
        checkAccess(actorId, courseId);
        MasteryOverviewResponse overview = overviewService.find(actorId, courseId, classId);

        List<UUID> studentIds = classId == null
                ? courseAccessPort.findActiveStudentIdsByCourse(courseId)
                : courseAccessPort.findActiveStudentIdsByCourseAndSourceClass(courseId, classId);

        long openGaps = studentIds.isEmpty() ? 0
                : (classId == null
                        ? knowledgeGapRepository.countByCourseIdAndStatus(courseId, "OPEN")
                        : knowledgeGapRepository.countByCourseIdAndStudentIdInAndStatus(
                                courseId, studentIds, "OPEN"));

        Set<UUID> studentSet = new HashSet<>(studentIds);
        List<AssignmentCompletionItem> completion = new ArrayList<>();
        List<Assignment> assignments = assignmentRepository
                .findByCourseIdAndPublishStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                        courseId, "PUBLISHED");
        for (Assignment a : assignments) {
            int submitted = 0;
            for (UUID sid : submissionRepository.findSubmittedStudentIds(a.getId())) {
                if (studentSet.contains(sid)) {
                    submitted++;
                }
            }
            double rate = studentSet.isEmpty() ? 0.0
                    : Math.round(10000.0 * submitted / studentSet.size()) / 10000.0;
            completion.add(new AssignmentCompletionItem(
                    a.getId(), a.getTitle(), a.getDueDate(),
                    studentSet.size(), submitted, rate));
        }

        return new ClassProgressReport(courseId, classId, OffsetDateTime.now(),
                overview, completion, openGaps);
    }

    public void checkAccess(UUID actorId, UUID courseId) {
        if (!courseAccessPort.isOwnerOrCoTeacher(actorId, courseId)) {
            throw new BusinessException("REPORT_SCOPE_FORBIDDEN",
                    "Chỉ owner hoặc đồng giảng dạy của course mới được xem báo cáo",
                    HttpStatus.FORBIDDEN);
        }
    }
}
