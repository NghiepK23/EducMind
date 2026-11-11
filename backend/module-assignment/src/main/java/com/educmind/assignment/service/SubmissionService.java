package com.educmind.assignment.service;

import com.educmind.assignment.dto.AttachmentResponse;
import com.educmind.assignment.dto.SubmissionResponse;
import com.educmind.assignment.dto.SubmitRequest;
import com.educmind.assignment.entity.Assignment;
import com.educmind.assignment.entity.AssignmentSubmission;
import com.educmind.assignment.entity.SubmissionStatus;
import com.educmind.assignment.repository.AssignmentSubmissionRepository;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.storage.LocalFileStorage;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * US-S02 — làm và nộp bài tập; US-S07 — sửa/xoá/nộp lại trong thời hạn; US-S04 — xem kết quả.
 *
 * Các ràng buộc "đã chấm thì không sửa", "quá hạn thì không sửa" còn được trigger DB
 * `trg_restrict_submission_edit` bảo vệ lần thứ hai; service kiểm tra trước để trả mã lỗi rõ ràng.
 */
@Service
public class SubmissionService {

    private static final long MAX_ATTACHMENT_BYTES = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "md",
            "png", "jpg", "jpeg", "gif", "zip", "java", "py", "sql"
    );

    private final AssignmentSubmissionRepository submissionRepository;
    private final AssignmentPermission permission;
    private final LocalFileStorage storage;

    public SubmissionService(AssignmentSubmissionRepository submissionRepository,
                             AssignmentPermission permission,
                             LocalFileStorage storage) {
        this.submissionRepository = submissionRepository;
        this.permission = permission;
        this.storage = storage;
    }

    public record Download(String fileName, FileSystemResource file) {
    }

    /** AC-S02.1 … AC-S02.3, AC-S07.4: nộp mới hoặc khôi phục bài đã xoá mềm. */
    @Transactional
    public SubmissionResponse submit(UUID studentId, UUID assignmentId, SubmitRequest request) {
        Assignment assignment = permission.requireVisibleAssignment(studentId, "STUDENT", assignmentId);

        OffsetDateTime now = OffsetDateTime.now();
        ensureWindowOpen(assignment, now);
        ensureHasContent(request);

        Optional<AssignmentSubmission> existing =
                submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId);

        if (existing.isPresent() && existing.get().getDeletedAt() == null) {
            throw new BusinessException("SUBMISSION_ALREADY_EXISTS",
                    "Bạn đã nộp bài này, hãy dùng chức năng chỉnh sửa", HttpStatus.CONFLICT);
        }
        if (existing.isPresent()) {
            AssignmentSubmission submission = existing.get();
            submission.restore(trim(request.getTextContent()), trim(request.getFileUrl()));
            return SubmissionResponse.from(submissionRepository.save(submission));
        }

        SubmissionStatus status = now.isAfter(assignment.getDueDate())
                ? SubmissionStatus.LATE
                : SubmissionStatus.SUBMITTED;
        AssignmentSubmission submission = new AssignmentSubmission(assignmentId, studentId,
                trim(request.getTextContent()), trim(request.getFileUrl()), status);
        return SubmissionResponse.from(submissionRepository.save(submission));
    }

    /** AC-S07.1/2/3/5: chỉ sửa khi chưa chấm và còn trong thời hạn nộp. */
    @Transactional
    public SubmissionResponse edit(UUID studentId, UUID assignmentId, SubmitRequest request) {
        Assignment assignment = permission.requireVisibleAssignment(studentId, "STUDENT", assignmentId);
        AssignmentSubmission submission = requireMine(assignmentId, studentId);

        if (submission.isGraded()) {
            throw new BusinessException("SUBMISSION_ALREADY_GRADED",
                    "Bài nộp đã được chấm, không thể chỉnh sửa", HttpStatus.CONFLICT);
        }
        ensureWindowOpen(assignment, OffsetDateTime.now());
        ensureHasContent(request);

        submission.editContent(trim(request.getTextContent()), trim(request.getFileUrl()));
        return SubmissionResponse.from(submissionRepository.save(submission));
    }

    /** AC-S07.4: xoá bài nộp chưa chấm trong thời hạn; trigger xoá luôn bằng chứng liên quan. */
    @Transactional
    public void deleteMine(UUID studentId, UUID assignmentId) {
        Assignment assignment = permission.requireVisibleAssignment(studentId, "STUDENT", assignmentId);
        AssignmentSubmission submission = requireMine(assignmentId, studentId);

        if (submission.isGraded()) {
            throw new BusinessException("SUBMISSION_ALREADY_GRADED",
                    "Bài nộp đã được chấm, không thể xoá", HttpStatus.CONFLICT);
        }
        ensureWindowOpen(assignment, OffsetDateTime.now());

        submission.markDeleted();
        submissionRepository.save(submission);
    }

    /** US-S04: học sinh xem bài nộp của chính mình (AC-S04.1, AC-S04.3). */
    @Transactional(readOnly = true)
    public SubmissionResponse getMine(UUID studentId, UUID assignmentId) {
        permission.requireVisibleAssignment(studentId, "STUDENT", assignmentId);
        return SubmissionResponse.from(requireMine(assignmentId, studentId));
    }

    /** Đính kèm tệp cho bài nộp (AC-S02.3) — trả khoá tệp để gửi kèm khi nộp bài. */
    @Transactional
    public AttachmentResponse uploadAttachment(UUID studentId, UUID assignmentId, MultipartFile file) {
        Assignment assignment = permission.requireVisibleAssignment(studentId, "STUDENT", assignmentId);
        ensureWindowOpen(assignment, OffsetDateTime.now());
        storage.validate(file, ALLOWED_EXTENSIONS, MAX_ATTACHMENT_BYTES);

        String key = storage.store("submissions/" + assignmentId, file);
        return new AttachmentResponse(key, file.getOriginalFilename(), file.getSize());
    }

    /** Tải tệp bài nộp: giáo viên phụ trách hoặc chính học sinh đã nộp. */
    @Transactional(readOnly = true)
    public Download download(UUID actorId, String role, UUID submissionId) {
        AssignmentSubmission submission = submissionRepository.findByIdAndDeletedAtIsNull(submissionId)
                .orElseThrow(() -> permission.notFound("Không tìm thấy bài nộp"));

        if (AssignmentPermission.isTeacher(role)) {
            permission.requireManagedAssignment(actorId, submission.getAssignmentId());
        } else if (!submission.getStudentId().equals(actorId)) {
            throw new BusinessException("SUBMISSION_ACCESS_DENIED",
                    "Bạn không có quyền xem bài nộp này", HttpStatus.FORBIDDEN);
        }

        if (submission.getFileUrl() == null) {
            throw new BusinessException("SUBMISSION_HAS_NO_FILE",
                    "Bài nộp không có tệp đính kèm", HttpStatus.NOT_FOUND);
        }
        String fileName = submission.getFileUrl().substring(submission.getFileUrl().lastIndexOf('/') + 1);
        return new Download(fileName, storage.load(submission.getFileUrl()));
    }

    private AssignmentSubmission requireMine(UUID assignmentId, UUID studentId) {
        return submissionRepository.findByAssignmentIdAndStudentIdAndDeletedAtIsNull(assignmentId, studentId)
                .orElseThrow(() -> new BusinessException("SUBMISSION_NOT_FOUND",
                        "Bạn chưa nộp bài tập này", HttpStatus.NOT_FOUND));
    }

    private void ensureWindowOpen(Assignment assignment, OffsetDateTime now) {
        boolean open = !now.isAfter(assignment.getDueDate()) || assignment.isLateWindowOpen(now);
        if (!open) {
            throw new BusinessException("SUBMISSION_CLOSED",
                    "Đã quá thời hạn nộp bài", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private void ensureHasContent(SubmitRequest request) {
        boolean hasText = request.getTextContent() != null && !request.getTextContent().isBlank();
        boolean hasFile = request.getFileUrl() != null && !request.getFileUrl().isBlank();
        if (!hasText && !hasFile) {
            throw new BusinessException("SUBMISSION_EMPTY",
                    "Bài nộp phải có nội dung hoặc tệp đính kèm", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
