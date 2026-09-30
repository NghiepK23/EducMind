package com.educmind.course.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.UserLookupPort;
import com.educmind.course.dto.CreateSubjectRequest;
import com.educmind.course.dto.SubjectResponse;
import com.educmind.course.dto.UpdateSubjectRequest;
import com.educmind.course.entity.Subject;
import com.educmind.course.repository.CourseRepository;
import com.educmind.course.repository.SubjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Logic nghiệp vụ cho Subject (môn học) — SCHOOL_ADMIN tạo sẵn cho trường mình,
 * mọi role (SCHOOL_ADMIN/TEACHER/STUDENT) đều xem được danh sách.
 *
 * [Giải thích cho người mới] Việc kiểm tra "chỉ SCHOOL_ADMIN mới được tạo/sửa/xóa"
 * KHÔNG lặp lại ở đây — đã có @PreAuthorize chặn ở SubjectController rồi, đúng
 * pattern đã dùng ở CourseService (Service chỉ lo ownership/scoping theo trường,
 * không tự check role hệ thống).
 *
 * Mọi method đều tra schoolId của actor qua UserLookupPort trước tiên, vì Subject
 * luôn phải được scope đúng theo trường của actor — tránh 1 SCHOOL_ADMIN trường A
 * xem/sửa được Subject của trường B.
 */
@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final CourseRepository courseRepository;
    private final UserLookupPort userLookupPort;

    public SubjectService(
            SubjectRepository subjectRepository,
            CourseRepository courseRepository,
            UserLookupPort userLookupPort
    ) {
        this.subjectRepository = subjectRepository;
        this.courseRepository = courseRepository;
        this.userLookupPort = userLookupPort;
    }

    @Transactional
    public SubjectResponse createSubject(UUID actorId, CreateSubjectRequest request) {
        UserLookupPort.UserSummary actor = resolveActor(actorId);

        Subject subject = new Subject(actor.schoolId(), request.getName(), actorId);
        subject.setCode(request.getCode());
        subject.setDescription(request.getDescription());
        subject = subjectRepository.save(subject);

        return toResponse(subject);
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> listSubjects(UUID actorId) {
        UserLookupPort.UserSummary actor = resolveActor(actorId);

        return subjectRepository.findBySchoolIdAndDeletedAtIsNull(actor.schoolId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubjectResponse getSubjectDetail(UUID actorId, UUID subjectId) {
        UserLookupPort.UserSummary actor = resolveActor(actorId);
        Subject subject = loadSubjectOrThrow(subjectId, actor.schoolId());
        return toResponse(subject);
    }

    @Transactional
    public SubjectResponse updateSubject(UUID actorId, UUID subjectId, UpdateSubjectRequest request) {
        UserLookupPort.UserSummary actor = resolveActor(actorId);
        Subject subject = loadSubjectOrThrow(subjectId, actor.schoolId());

        if (request.getName() != null) {
            subject.setName(request.getName());
        }
        if (request.getCode() != null) {
            subject.setCode(request.getCode());
        }
        if (request.getDescription() != null) {
            subject.setDescription(request.getDescription());
        }

        subject = subjectRepository.save(subject);
        return toResponse(subject);
    }

    @Transactional
    public SubjectResponse deleteSubject(UUID actorId, UUID subjectId) {
        UserLookupPort.UserSummary actor = resolveActor(actorId);
        Subject subject = loadSubjectOrThrow(subjectId, actor.schoolId());

        if (courseRepository.existsBySubjectIdAndDeletedAtIsNull(subjectId)) {
            throw new BusinessException(
                    "SUBJECT_IN_USE",
                    "Không thể xóa môn học đang được ít nhất 1 khóa học sử dụng",
                    HttpStatus.CONFLICT
            );
        }

        subject.setDeletedAt(OffsetDateTime.now());
        subject = subjectRepository.save(subject);
        return toResponse(subject);
    }

    /**
     * Tra thông tin actor qua UserLookupPort. Trong thực tế hiếm khi rơi vào nhánh
     * lỗi này (actor đã qua JwtAuthenticationFilter xác thực), nhưng vẫn cần
     * defensive check vì user có thể bị xóa sau khi token đã phát hành.
     */
    private UserLookupPort.UserSummary resolveActor(UUID actorId) {
        return userLookupPort.findUserSummary(actorId)
                .orElseThrow(() -> new BusinessException(
                        "USER_NOT_FOUND",
                        "Không tìm thấy người dùng",
                        HttpStatus.NOT_FOUND
                ));
    }

    /**
     * Nếu subject không tồn tại HOẶC thuộc trường khác actorSchoolId, đều trả cùng
     * 1 lỗi SUBJECT_NOT_FOUND — cố ý không phân biệt 2 trường hợp này ra ngoài,
     * tránh lộ thông tin "trường khác có Subject này hay không" cho người ngoài trường.
     */
    private Subject loadSubjectOrThrow(UUID subjectId, UUID actorSchoolId) {
        Subject subject = subjectRepository.findByIdAndDeletedAtIsNull(subjectId)
                .orElseThrow(() -> new BusinessException(
                        "SUBJECT_NOT_FOUND",
                        "Không tìm thấy môn học",
                        HttpStatus.NOT_FOUND
                ));

        if (!subject.getSchoolId().equals(actorSchoolId)) {
            throw new BusinessException(
                    "SUBJECT_NOT_FOUND",
                    "Không tìm thấy môn học",
                    HttpStatus.NOT_FOUND
            );
        }

        return subject;
    }

    private SubjectResponse toResponse(Subject subject) {
        return new SubjectResponse(
                subject.getId(),
                subject.getSchoolId(),
                subject.getName(),
                subject.getCode(),
                subject.getDescription(),
                subject.getStatus(),
                subject.getCreatedBy(),
                subject.getCreatedAt(),
                subject.getUpdatedAt(),
                subject.getDeletedAt()
        );
    }
}
