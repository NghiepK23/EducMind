package com.educmind.assignment.service;

import com.educmind.assignment.dto.GradeRequest;
import com.educmind.assignment.dto.RosterItem;
import com.educmind.assignment.dto.SubmissionResponse;
import com.educmind.assignment.entity.Assignment;
import com.educmind.assignment.entity.AssignmentObjective;
import com.educmind.assignment.entity.AssignmentSubmission;
import com.educmind.assignment.repository.AssignmentObjectiveRepository;
import com.educmind.assignment.repository.AssignmentSubmissionRepository;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.ClassAccessPort;
import com.educmind.common.port.CourseAccessPort;
import com.educmind.common.port.LearningEvidencePort;
import com.educmind.common.port.UserLookupPort;
import org.springframework.dao.DataIntegrityViolationException;
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
 * US-T04 — chấm bài và phản hồi.
 * AC-T04.1 danh sách đủ học sinh (kể cả chưa nộp), AC-T04.2 điểm trong [0, max],
 * AC-T04.3 ghi bằng chứng theo mục tiêu, AC-T04.4 chấm lại cập nhật bản ghi cũ.
 */
@Service
public class GradingService {

    private final AssignmentSubmissionRepository submissionRepository;
    private final AssignmentObjectiveRepository objectiveRepository;
    private final AssignmentPermission permission;
    private final ClassAccessPort classAccessPort;
    private final CourseAccessPort courseAccessPort;
    private final UserLookupPort userLookupPort;
    private final LearningEvidencePort evidencePort;

    public GradingService(AssignmentSubmissionRepository submissionRepository,
                          AssignmentObjectiveRepository objectiveRepository,
                          AssignmentPermission permission,
                          ClassAccessPort classAccessPort,
                          CourseAccessPort courseAccessPort,
                          UserLookupPort userLookupPort,
                          LearningEvidencePort evidencePort) {
        this.submissionRepository = submissionRepository;
        this.objectiveRepository = objectiveRepository;
        this.permission = permission;
        this.classAccessPort = classAccessPort;
        this.courseAccessPort = courseAccessPort;
        this.userLookupPort = userLookupPort;
        this.evidencePort = evidencePort;
    }

    /** AC-T04.2 + AC-T04.3 + AC-T04.4. */
    @Transactional
    public SubmissionResponse grade(UUID teacherId, UUID submissionId, GradeRequest request) {
        AssignmentSubmission submission = submissionRepository.findByIdAndDeletedAtIsNull(submissionId)
                .orElseThrow(() -> permission.notFound("Không tìm thấy bài nộp"));
        Assignment assignment = permission.requireManagedAssignment(teacherId, submission.getAssignmentId());

        BigDecimal score = request.getScore();
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(assignment.getMaxScore()) > 0) {
            throw new BusinessException("SCORE_OUT_OF_RANGE",
                    "Điểm phải nằm trong khoảng 0 đến " + assignment.getMaxScore().toPlainString(),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        submission.grade(score.setScale(2, RoundingMode.HALF_UP), request.getFeedback(),
                teacherId, OffsetDateTime.now());
        try {
            submission = submissionRepository.saveAndFlush(submission);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("SCORE_OUT_OF_RANGE",
                    "Điểm không hợp lệ với bài tập này", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        recordEvidence(submission, assignment);
        return SubmissionResponse.from(submission);
    }

    /** AC-T04.1: mọi học sinh của lớp, kể cả chưa nộp. */
    @Transactional(readOnly = true)
    public List<RosterItem> roster(UUID teacherId, UUID assignmentId) {
        Assignment assignment = permission.requireManagedAssignment(teacherId, assignmentId);

        UUID classId = courseAccessPort.findClassId(assignment.getCourseId())
                .orElseThrow(() -> permission.notFound("Không tìm thấy lớp của khóa học"));
        List<UUID> studentIds = classAccessPort.findStudentIds(classId);

        Map<UUID, AssignmentSubmission> byStudent = new HashMap<>();
        for (AssignmentSubmission submission : submissionRepository
                .findByAssignmentIdAndDeletedAtIsNullOrderBySubmittedAtAsc(assignmentId)) {
            byStudent.put(submission.getStudentId(), submission);
        }

        List<RosterItem> result = new ArrayList<>();
        for (UserLookupPort.UserDetail detail : userLookupPort.findUserDetails(studentIds)) {
            AssignmentSubmission submission = byStudent.get(detail.id());
            if (submission == null) {
                result.add(new RosterItem(detail.id(), detail.fullName(), detail.email(),
                        false, null, null, null, false, null, false));
            } else {
                result.add(new RosterItem(detail.id(), detail.fullName(), detail.email(),
                        true, submission.getId(), submission.getStatus().name(), submission.getScore(),
                        !submission.isGraded(), submission.getSubmittedAt(), submission.isEdited()));
            }
        }
        return result;
    }

    /**
     * AC-T04.3: mỗi mục tiêu gắn với bài tập nhận một bằng chứng có điểm chuẩn hoá 0..1.
     * Chấm lại chỉ cập nhật điểm của bằng chứng cũ (unique key ở DB).
     */
    private void recordEvidence(AssignmentSubmission submission, Assignment assignment) {
        BigDecimal max = assignment.getMaxScore();
        if (max.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        double ratio = submission.getScore().divide(max, 4, RoundingMode.HALF_UP).doubleValue();

        for (AssignmentObjective ao : objectiveRepository.findByIdAssignmentId(assignment.getId())) {
            evidencePort.record(submission.getStudentId(), ao.getObjectiveId(),
                    LearningEvidencePort.SourceType.ASSIGNMENT, submission.getId(), ratio);
        }
    }
}
