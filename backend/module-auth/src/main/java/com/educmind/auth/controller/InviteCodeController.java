package com.educmind.auth.controller;

import com.educmind.auth.dto.CreateInviteCodeRequest;
import com.educmind.auth.dto.InviteCodeResponse;
import com.educmind.auth.service.InviteCodeService;
import com.educmind.common.envelope.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Module01_Auth_SchoolClass_FINAL.md, mục A.6 + A.5.3.
 * Cả 3 endpoint chỉ dành cho TEACHER, giới hạn ở lớp mình phụ trách/đồng giảng dạy
 * (check quyền cụ thể nằm trong InviteCodeService, dùng ClassAccessPort).
 */
@RestController
@RequestMapping("/api/v1/invite-codes")
@PreAuthorize("hasRole('TEACHER')")
public class InviteCodeController {

    private final InviteCodeService inviteCodeService;

    public InviteCodeController(InviteCodeService inviteCodeService) {
        this.inviteCodeService = inviteCodeService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InviteCodeResponse>> createInviteCode(
            Authentication authentication,
            @Valid @RequestBody CreateInviteCodeRequest request
    ) {
        UUID teacherId = UUID.fromString(authentication.getName());
        InviteCodeResponse response = inviteCodeService.createInviteCode(teacherId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InviteCodeResponse>>> listInviteCodes(
            Authentication authentication
    ) {
        UUID teacherId = UUID.fromString(authentication.getName());
        List<InviteCodeResponse> response = inviteCodeService.listInviteCodes(teacherId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> revokeInviteCode(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID teacherId = UUID.fromString(authentication.getName());
        inviteCodeService.revokeInviteCode(teacherId, id);
        return ResponseEntity.noContent().build();
    }
}
