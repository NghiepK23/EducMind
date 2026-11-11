package com.educmind.assignment.controller;

import com.educmind.assignment.dto.AttachmentResponse;
import com.educmind.assignment.dto.SubmissionResponse;
import com.educmind.assignment.dto.SubmitRequest;
import com.educmind.assignment.service.SubmissionService;
import com.educmind.common.envelope.ApiResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** US-S02 (nộp bài), US-S07 (sửa/xoá/nộp lại), US-S04 (xem kết quả). */
@RestController
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/api/v1/assignments/{assignmentId}/submission")
    public ResponseEntity<ApiResponse<SubmissionResponse>> submit(
            Authentication authentication, @PathVariable UUID assignmentId,
            @RequestBody SubmitRequest request) {
        SubmissionResponse response = submissionService.submit(
                Actor.id(authentication), assignmentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PatchMapping("/api/v1/assignments/{assignmentId}/submission")
    public ResponseEntity<ApiResponse<SubmissionResponse>> edit(
            Authentication authentication, @PathVariable UUID assignmentId,
            @RequestBody SubmitRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                submissionService.edit(Actor.id(authentication), assignmentId, request)));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/api/v1/assignments/{assignmentId}/submission")
    public ResponseEntity<ApiResponse<SubmissionResponse>> getMine(
            Authentication authentication, @PathVariable UUID assignmentId) {
        return ResponseEntity.ok(ApiResponse.success(
                submissionService.getMine(Actor.id(authentication), assignmentId)));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @DeleteMapping("/api/v1/assignments/{assignmentId}/submission")
    public ResponseEntity<ApiResponse<Void>> deleteMine(Authentication authentication,
                                                        @PathVariable UUID assignmentId) {
        submissionService.deleteMine(Actor.id(authentication), assignmentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/api/v1/assignments/{assignmentId}/submission/attachment")
    public ResponseEntity<ApiResponse<AttachmentResponse>> uploadAttachment(
            Authentication authentication, @PathVariable UUID assignmentId,
            @RequestParam("file") MultipartFile file) {
        AttachmentResponse response = submissionService.uploadAttachment(
                Actor.id(authentication), assignmentId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    @GetMapping("/api/v1/submissions/{id}/file")
    public ResponseEntity<FileSystemResource> downloadFile(Authentication authentication,
                                                          @PathVariable UUID id) {
        SubmissionService.Download download = submissionService.download(
                Actor.id(authentication), Actor.role(authentication), id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(download.file());
    }
}
