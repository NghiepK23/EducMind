package com.educmind.schoolclass.controller;

import com.educmind.common.envelope.ApiResponse;
import com.educmind.schoolclass.dto.AddMemberRequest;
import com.educmind.schoolclass.dto.ClassResponse;
import com.educmind.schoolclass.dto.CreateClassRequest;
import com.educmind.schoolclass.dto.MemberResponse;
import com.educmind.schoolclass.dto.UpdateClassRequest;
import com.educmind.schoolclass.service.ClassService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Module01_Auth_SchoolClass_FINAL.md, mục B.6.
 */
@RestController
@RequestMapping("/api/v1/classes")
public class ClassController {

    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<ClassResponse>> createClass(
            Authentication authentication,
            @Valid @RequestBody CreateClassRequest request
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        ClassResponse response = classService.createClass(actorId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ClassResponse>>> listClasses(
            Authentication authentication,
            @RequestParam(required = false) UUID teacherId
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        List<ClassResponse> response = classService.listClasses(actorId, teacherId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'STUDENT')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClassResponse>> getClassDetail(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        ClassResponse response = classService.getClassDetail(actorId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ClassResponse>> updateClass(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody UpdateClassRequest request
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        ClassResponse response = classService.updateClass(actorId, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<ClassResponse>> deleteClass(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        ClassResponse response = classService.deleteClass(actorId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Thêm thành viên vào lớp — Module01_Auth_SchoolClass_FINAL.md, mục B.6/B.10.
     * Cùng quyền với PATCH/DELETE (chỉ owner/SCHOOL_ADMIN, không cho đồng giảng dạy) —
     * thêm thành viên là hành động quản lý, không phải xem (B.7).
     * @Valid kích hoạt validate @NotNull/@Pattern trong AddMemberRequest trước khi vào Service.
     */
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<MemberResponse>> addMember(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody AddMemberRequest request
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        MemberResponse response = classService.addMember(actorId, id, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * Gỡ thành viên khỏi lớp — Module01_Auth_SchoolClass_FINAL.md, mục B.6/B.10.
     * Cùng quyền với addMember/PATCH/DELETE (chỉ owner/SCHOOL_ADMIN).
     * Soft-remove: set leftAt, không xóa dòng class_members (giữ lịch sử).
     * Trả 200 kèm MemberResponse đã cập nhật (nhất quán cách deleteClass đang làm).
     */
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<MemberResponse>> removeMember(
            Authentication authentication,
            @PathVariable UUID id,
            @PathVariable UUID userId
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        MemberResponse response = classService.removeMember(actorId, id, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
