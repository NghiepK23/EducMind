package com.educmind.assignment.controller;

import com.educmind.assignment.dto.AssignmentResponse;
import com.educmind.assignment.dto.AssignmentSubmissionResponse;
import com.educmind.assignment.dto.CreateAssignmentRequest;
import com.educmind.assignment.dto.GradeSubmissionRequest;
import com.educmind.assignment.dto.SubmitAssignmentRequest;
import com.educmind.assignment.dto.UpdateAssignmentRequest;
import com.educmind.assignment.service.AssignmentService;
import com.educmind.common.envelope.ApiResponse;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Module04_Assignment_Assessment_FINAL.md, PHẦN D, mục D.6.
 *
 * Không dùng @RequestMapping ở class, cùng lý do như LessonController/ResourceController —
 * endpoint trải trên nhiều base path khác nhau (/courses/{id}/assignments,
 * /assignments/{id}, /assignments/{id}/submissions, /assignment-submissions/{id}/grade).
 *
 * Chấm điểm (gradeSubmission) dùng hasRole('TEACHER') giống mọi thao tác quản lý khác —
 * việc phân biệt owner hay đồng giảng dạy được AssignmentService xử lý ở tầng nghiệp vụ
 * (isOwnerOrCoTeacher), không phải ở @PreAuthorize.
 */
@RestController
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/api/v1/courses/{courseId}/assignments")
    public ResponseEntity<ApiResponse<AssignmentResponse>> createAssignment(
            Authentication authentication,
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateAssignmentRequest request
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        AssignmentResponse response = assignmentService.createAssignment(actorId, courseId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'STUDENT')")
    @GetMapping("/api/v1/courses/{courseId}/assignments")
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> listAssignments(
            Authentication authentication,
            @PathVariable UUID courseId
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        List<AssignmentResponse> response = assignmentService.listAssignments(actorId, courseId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PatchMapping("/api/v1/assignments/{id}")
    public ResponseEntity<ApiResponse<AssignmentResponse>> updateAssignment(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAssignmentRequest request
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        AssignmentResponse response = assignmentService.updateAssignment(actorId, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PatchMapping("/api/v1/assignments/{id}/publish")
    public ResponseEntity<ApiResponse<AssignmentResponse>> publishAssignment(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        AssignmentResponse response = assignmentService.publishAssignment(actorId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/api/v1/assignments/{id}")
    public ResponseEntity<ApiResponse<AssignmentResponse>> deleteAssignment(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        AssignmentResponse response = assignmentService.deleteAssignment(actorId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/api/v1/assignments/{id}/submissions")
    public ResponseEntity<ApiResponse<AssignmentSubmissionResponse>> submitAssignment(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody SubmitAssignmentRequest request
    ) {
        UUID studentId = UUID.fromString(authentication.getName());
        AssignmentSubmissionResponse response = assignmentService.submitAssignment(studentId, id, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/api/v1/assignments/{id}/submissions")
    public ResponseEntity<ApiResponse<List<AssignmentSubmissionResponse>>> listSubmissions(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        List<AssignmentSubmissionResponse> response = assignmentService.listSubmissions(actorId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/api/v1/assignments/{id}/submissions/me")
    public ResponseEntity<ApiResponse<AssignmentSubmissionResponse>> getMySubmission(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID studentId = UUID.fromString(authentication.getName());
        AssignmentSubmissionResponse response = assignmentService.getMySubmission(studentId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PatchMapping("/api/v1/assignment-submissions/{id}/grade")
    public ResponseEntity<ApiResponse<AssignmentSubmissionResponse>> gradeSubmission(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody GradeSubmissionRequest request
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        AssignmentSubmissionResponse response = assignmentService.gradeSubmission(actorId, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
