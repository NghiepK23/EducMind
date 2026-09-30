package com.educmind.course.controller;

import com.educmind.common.envelope.ApiResponse;
import com.educmind.course.dto.CreateSubjectRequest;
import com.educmind.course.dto.SubjectResponse;
import com.educmind.course.dto.UpdateSubjectRequest;
import com.educmind.course.service.SubjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * CRUD Subject (môn học). Tạo/sửa/xóa chỉ SCHOOL_ADMIN; xem (list/detail) mọi
 * role cùng trường (SCHOOL_ADMIN/TEACHER/STUDENT) — TEACHER cần xem để chọn
 * subjectId lúc tạo Course, STUDENT xem để biết tên môn học của Course mình học.
 */
@RestController
@RequestMapping("/api/v1/subjects")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<SubjectResponse>> createSubject(
            Authentication authentication,
            @Valid @RequestBody CreateSubjectRequest request
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        SubjectResponse response = subjectService.createSubject(actorId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> listSubjects(
            Authentication authentication
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        List<SubjectResponse> response = subjectService.listSubjects(actorId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubjectResponse>> getSubjectDetail(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        SubjectResponse response = subjectService.getSubjectDetail(actorId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<SubjectResponse>> updateSubject(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody UpdateSubjectRequest request
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        SubjectResponse response = subjectService.updateSubject(actorId, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<SubjectResponse>> deleteSubject(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        SubjectResponse response = subjectService.deleteSubject(actorId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
