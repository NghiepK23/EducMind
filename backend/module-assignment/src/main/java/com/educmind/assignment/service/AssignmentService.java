package com.educmind.assignment.service;

import com.educmind.assignment.dto.AssignmentResponse;
import com.educmind.assignment.dto.AssignmentSubmissionResponse;
import com.educmind.assignment.dto.CreateAssignmentRequest;
import com.educmind.assignment.dto.GradeSubmissionRequest;
import com.educmind.assignment.dto.SubmitAssignmentRequest;
import com.educmind.assignment.dto.UpdateAssignmentRequest;
import com.educmind.assignment.entity.Assignment;
import com.educmind.assignment.entity.AssignmentSubmission;
import com.educmind.assignment.repository.AssignmentRepository;
import com.educmind.assignment.repository.AssignmentSubmissionRepository;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.CourseAccessPort;
import com.educmind.common.port.LearningEvidencePort;
import com.educmind.common.port.UserLookupPort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Logic nghiệp vụ Assignment CRUD + submission + chấm điểm —
 * Module04_Assignment_Assessment_FINAL.md, PHẦN D (D.4-D.9), đã điều chỉnh
 * theo kiến trúc course_members mới (KHÔNG có assignment_class — xem
 * V4__module_assignment_assessment.sql, publish áp dụng cho toàn bộ
 * course_members đang active, không cần gắn riêng với lớp nào).
 *
 * [ĐÃ CẬP NHẬT — xác nhận lại với Nghiệp] Authorization thống nhất 1 mức
 *   cho MỌI hành động, bao gồm cả chấm điểm:
 *   - Tạo/sửa/xóa/publish assignment VÀ chấm điểm (gradeSubmission): owner
 *     HOẶC đồng giảng dạy (courseAccessPort.isOwnerOrCoTeacher, qua
 *     assertCanManage) — ngang quyền, đúng nguyên tắc chung đã dùng ở
 *     Module 3 (PATCH_4Decisions.md PATCH 2). Trước đây file này từng ghi
 *     chấm điểm là ngoại lệ chỉ-owner-gốc — không còn đúng nữa, đã bỏ.
 */
@Service
public class AssignmentService {

    private static final String ROLE_STUDENT = "STUDENT";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String SUB_NOT_STARTED = "NOT_STARTED";
    private static final String SUB_SUBMITTED = "SUBMITTED";
    private static final String SUB_LATE = "LATE";

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final CourseAccessPort courseAccessPort;
    private final UserLookupPort userLookupPort;
    private final LearningEvidencePort learningEvidencePort;

    public AssignmentService(AssignmentRepository assignmentRepository,
                              AssignmentSubmissionRepository submissionRepository,
                              CourseAccessPort courseAccessPort,
                              UserLookupPort userLookupPort,
                              LearningEvidencePort learningEvidencePort) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.courseAccessPort = courseAccessPort;
        this.userLookupPort = userLookupPort;
        this.learningEvidencePort = learningEvidencePort;
    }

    /** POST /api/v1/courses/{id}/assignments — owner/đồng giảng dạy. */
    @Transactional
    public AssignmentResponse createAssignment(UUID actorId, UUID courseId, CreateAssignmentRequest request) {
        assertCanManage(actorId, courseId);

        Assignment assignment = new Assignment(courseId, request.getTitle(), actorId);
        assignment.setDescription(request.getDescription());
        assignment.setLessonId(request.getLessonId());
        assignment.setDueDate(request.getDueDate());
        if (request.getAllowLateSubmission() != null) {
            assignment.setAllowLateSubmission(request.getAllowLateSubmission());
        }
        if (request.getMaxScore() != null) {
            assertMaxScorePositive(request.getMaxScore());
            assignment.setMaxScore(request.getMaxScore());
        }

        assignment = assignmentRepository.save(assignment);
        return toResponse(assignment);
    }

    /**
     * GET /api/v1/courses/{id}/assignments — TEACHER/SCHOOL_ADMIN thấy mọi
     * publish_status, STUDENT chỉ thấy PUBLISHED (phải đang active trong
     * course_members, không thì CLASS_NOT_LINKED — nhất quán cách LessonService
     * đã xử lý, dùng cùng mã lỗi dù bản chất bây giờ là "course_members" chứ
     * không phải "lớp").
     */
    @Transactional(readOnly = true)
    public List<AssignmentResponse> listAssignments(UUID actorId, UUID courseId) {
        UserLookupPort.UserSummary actor = loadActor(actorId);

        List<Assignment> assignments;
        if (ROLE_STUDENT.equals(actor.role())) {
            assertStudentEnrolled(actorId, courseId);
            assignments = assignmentRepository
                    .findByCourseIdAndPublishStatusAndDeletedAtIsNullOrderByCreatedAtDesc(courseId, STATUS_PUBLISHED);
        } else {
            assertCanView(actorId, courseId);
            assignments = assignmentRepository.findByCourseIdAndDeletedAtIsNullOrderByCreatedAtDesc(courseId);
        }

        return assignments.stream().map(this::toResponse).toList();
    }

    /** PATCH /api/v1/assignments/{id} — owner/đồng giảng dạy. */
    @Transactional
    public AssignmentResponse updateAssignment(UUID actorId, UUID assignmentId, UpdateAssignmentRequest request) {
        Assignment assignment = loadAssignmentOrThrow(assignmentId);
        assertCanManage(actorId, assignment.getCourseId());

        if (request.getTitle() != null) {
            if (!StringUtils.hasText(request.getTitle())) {
                throw new BusinessException("VALIDATION_ERROR", "title không được để trống",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
            assignment.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            assignment.setDescription(request.getDescription());
        }
        if (request.getDueDate() != null) {
            assignment.setDueDate(request.getDueDate());
        }
        if (request.getAllowLateSubmission() != null) {
            assignment.setAllowLateSubmission(request.getAllowLateSubmission());
        }
        if (request.getMaxScore() != null) {
            assertMaxScorePositive(request.getMaxScore());
            boolean changed = request.getMaxScore().compareTo(assignment.getMaxScore()) != 0;
            if (changed && !submissionRepository.findByAssignmentId(assignmentId).isEmpty()) {
                throw new BusinessException("ASSIGNMENT_HAS_SUBMISSIONS",
                        "Bài tập đã có bài nộp, không thể đổi maxScore", HttpStatus.CONFLICT);
            }
            assignment.setMaxScore(request.getMaxScore());
        }

        assignment = assignmentRepository.saveAndFlush(assignment);
        return toResponse(assignment);
    }

    /** PATCH /api/v1/assignments/{id}/publish — owner/đồng giảng dạy. */
    @Transactional
    public AssignmentResponse publishAssignment(UUID actorId, UUID assignmentId) {
        Assignment assignment = loadAssignmentOrThrow(assignmentId);
        assertCanManage(actorId, assignment.getCourseId());

        assignment.publish();
        assignment = assignmentRepository.saveAndFlush(assignment);
        return toResponse(assignment);
    }

    /** DELETE /api/v1/assignments/{id} — soft-delete, owner/đồng giảng dạy. */
    @Transactional
    public AssignmentResponse deleteAssignment(UUID actorId, UUID assignmentId) {
        Assignment assignment = loadAssignmentOrThrow(assignmentId);
        assertCanManage(actorId, assignment.getCourseId());

        assignment.setDeletedAt(OffsetDateTime.now());
        assignment = assignmentRepository.saveAndFlush(assignment);
        return toResponse(assignment);
    }

    /**
     * POST /api/v1/assignments/{id}/submissions — STUDENT nộp bài.
     * D.5 mục 2/3: chỉ nộp được đúng 1 lần; D.7: ASSIGNMENT_NOT_PUBLISHED,
     * SUBMISSION_CLOSED, ALREADY_SUBMITTED.
     */
    @Transactional
    public AssignmentSubmissionResponse submitAssignment(UUID studentId, UUID assignmentId,
                                                          SubmitAssignmentRequest request) {
        Assignment assignment = loadAssignmentOrThrow(assignmentId);
        assertStudentEnrolled(studentId, assignment.getCourseId());

        if (!STATUS_PUBLISHED.equals(assignment.getPublishStatus())) {
            throw new BusinessException("ASSIGNMENT_NOT_PUBLISHED",
                    "Bài tập này chưa được xuất bản", HttpStatus.FORBIDDEN);
        }

        AssignmentSubmission submission = submissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseGet(() -> new AssignmentSubmission(assignmentId, studentId));

        if (!SUB_NOT_STARTED.equals(submission.getStatus())) {
            throw new BusinessException("ALREADY_SUBMITTED",
                    "Bạn đã nộp bài này rồi, không thể nộp lại", HttpStatus.CONFLICT);
        }

        boolean isLate = assignment.getDueDate() != null
                && OffsetDateTime.now().isAfter(assignment.getDueDate());
        if (isLate && !assignment.isAllowLateSubmission()) {
            throw new BusinessException("SUBMISSION_CLOSED",
                    "Đã hết hạn nộp bài", HttpStatus.FORBIDDEN);
        }

        submission.submit(request.getTextAnswer(), request.getResourceId(), isLate);
        submission = submissionRepository.save(submission);
        try {
            learningEvidencePort.onAssignmentSubmitted(assignment.getCourseId(), studentId,
                    submission.getId(), assignment.getLessonId(), submission.getSubmittedAt());
        } catch (Exception e) {
            EVIDENCE_LOG.error("Sinh learning_evidence (nộp bài) thất bại, bỏ qua: submission={}",
                    submission.getId(), e);
        }
        return toSubmissionResponse(submission);
    }

    private static final org.slf4j.Logger EVIDENCE_LOG =
            org.slf4j.LoggerFactory.getLogger(AssignmentService.class);

    /** GET /api/v1/assignments/{id}/submissions — TEACHER (owner/đồng giảng dạy) xem tất cả. */
    @Transactional(readOnly = true)
    public List<AssignmentSubmissionResponse> listSubmissions(UUID actorId, UUID assignmentId) {
        Assignment assignment = loadAssignmentOrThrow(assignmentId);
        assertCanView(actorId, assignment.getCourseId());

        return submissionRepository.findByAssignmentId(assignmentId).stream()
                .map(this::toSubmissionResponse)
                .toList();
    }

    /** GET /api/v1/assignments/{id}/submissions/me — STUDENT xem bài của chính mình. */
    @Transactional(readOnly = true)
    public AssignmentSubmissionResponse getMySubmission(UUID studentId, UUID assignmentId) {
        Assignment assignment = loadAssignmentOrThrow(assignmentId);
        assertStudentEnrolled(studentId, assignment.getCourseId());

        AssignmentSubmission submission = submissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseThrow(() -> new BusinessException("SUBMISSION_NOT_FOUND",
                        "Bạn chưa nộp bài này", HttpStatus.NOT_FOUND));
        return toSubmissionResponse(submission);
    }

    /**
     * PATCH /api/v1/assignment-submissions/{id}/grade — owner HOẶC đồng
     * giảng dạy (ngang quyền), giống mọi method khác ở trên. [ĐÃ CẬP NHẬT]
     * trước đây chỉ owner gốc mới chấm được, nay đã xác nhận lại với Nghiệp
     * là ngang quyền, không còn là ngoại lệ riêng của Module 4 nữa.
     */
    @Transactional
    public AssignmentSubmissionResponse gradeSubmission(UUID actorId, UUID submissionId,
                                                         GradeSubmissionRequest request) {
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessException("SUBMISSION_NOT_FOUND",
                        "Không tìm thấy bài nộp", HttpStatus.NOT_FOUND));
        Assignment assignment = loadAssignmentOrThrow(submission.getAssignmentId());
        assertCanManage(actorId, assignment.getCourseId());

        BigDecimal score = request.getScore();
        if (score != null && (score.signum() < 0 || score.compareTo(assignment.getMaxScore()) > 0)) {
            throw new BusinessException("VALIDATION_ERROR",
                    "score phải nằm trong khoảng 0 đến " + assignment.getMaxScore().stripTrailingZeros().toPlainString(),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        submission.grade(request.getScore(), request.getFeedback(), actorId);
        submission = submissionRepository.save(submission);
        if (submission.getScore() != null) {
            try {
                learningEvidencePort.onAssignmentGraded(submission.getId(), submission.getScore(),
                        assignment.getMaxScore());
            } catch (Exception e) {
                EVIDENCE_LOG.error("Cập nhật learning_evidence (chấm bài) thất bại, bỏ qua: submission={}",
                        submission.getId(), e);
            }
        }
        return toSubmissionResponse(submission);
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

    /** TEACHER/SCHOOL_ADMIN xem — dùng chung isOwnerOrCoTeacher cho đơn giản
     * (SCHOOL_ADMIN oversight sẽ được xử lý riêng ở phạm vi rộng hơn sau này,
     * hiện tại D.8 chưa yêu cầu phân biệt SCHOOL_ADMIN xem gì khác TEACHER). */
    private void assertCanView(UUID actorId, UUID courseId) {
        assertCanManage(actorId, courseId);
    }

    private void assertStudentEnrolled(UUID studentId, UUID courseId) {
        if (!courseAccessPort.isStudentEnrolled(studentId, courseId)) {
            throw new BusinessException("CLASS_NOT_LINKED",
                    "Bạn chưa được thêm vào khóa học này", HttpStatus.FORBIDDEN);
        }
    }

    private void assertMaxScorePositive(BigDecimal maxScore) {
        if (maxScore.signum() <= 0) {
            throw new BusinessException("VALIDATION_ERROR", "maxScore phải lớn hơn 0",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private Assignment loadAssignmentOrThrow(UUID assignmentId) {
        return assignmentRepository.findByIdAndDeletedAtIsNull(assignmentId)
                .orElseThrow(() -> new BusinessException("ASSIGNMENT_NOT_FOUND",
                        "Không tìm thấy bài tập", HttpStatus.NOT_FOUND));
    }

    private UserLookupPort.UserSummary loadActor(UUID actorId) {
        return userLookupPort.findUserSummary(actorId)
                .orElseThrow(() -> new BusinessException("ACTOR_NOT_FOUND",
                        "Không tìm thấy thông tin tài khoản của bạn", HttpStatus.UNAUTHORIZED));
    }

    private AssignmentResponse toResponse(Assignment a) {
        return new AssignmentResponse(
                a.getId(), a.getCourseId(), a.getLessonId(), a.getTitle(), a.getDescription(),
                a.getDueDate(), a.isAllowLateSubmission(), a.getMaxScore(), a.getPublishStatus(),
                a.getCreatedBy(), a.getCreatedAt(), a.getUpdatedAt(), a.getDeletedAt()
        );
    }

    private AssignmentSubmissionResponse toSubmissionResponse(AssignmentSubmission s) {
        return new AssignmentSubmissionResponse(
                s.getId(), s.getAssignmentId(), s.getStudentId(), s.getTextAnswer(),
                s.getResourceId(), s.getStatus(), s.getSubmittedAt(), s.getScore(),
                s.getFeedback(), s.getGradedBy(), s.getGradedAt()
        );
    }
}
