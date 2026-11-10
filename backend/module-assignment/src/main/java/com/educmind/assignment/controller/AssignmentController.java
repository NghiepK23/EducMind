package com.educmind.assignment.controller;

import com.educmind.assignment.dto.AssignmentResponse;
import com.educmind.assignment.dto.CreateAssignmentRequest;
import com.educmind.assignment.dto.GradeRequest;
import com.educmind.assignment.dto.RosterItem;
import com.educmind.assignment.dto.SubmissionResponse;
import com.educmind.assignment.dto.UpdateAssignmentRequest;
import com.educmind.assignment.service.AssignmentService;
import com.educmind.assignment.service.GradingService;
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

/** US-T03 (bài tập), US-T04 (chấm bài). */
@RestController
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final GradingService gradingService;

    public AssignmentController(AssignmentService assignmentService, GradingService gradingService) {
        this.assignmentService = assignmentService;
        this.gradingService = gradingService;
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/api/v1/courses/{courseId}/assignments")
    public ResponseEntity<ApiResponse<AssignmentResponse>> create(
            Authentication authentication, @PathVariable UUID courseId,
            @Valid @RequestBody CreateAssignmentRequest request) {
        AssignmentResponse response = assignmentService.create(Actor.id(authentication), courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    @GetMapping("/api/v1/courses/{courseId}/assignments")
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> list(
            Authentication authentication, @PathVariable UUID courseId) {
        return ResponseEntity.ok(ApiResponse.success(assignmentService.list(
                Actor.id(authentication), Actor.role(authentication), courseId)));
    }

    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    @GetMapping("/api/v1/assignments/{id}")
    public ResponseEntity<ApiResponse<AssignmentResponse>> get(Authentication authentication,
                                                              @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(assignmentService.get(
                Actor.id(authentication), Actor.role(authentication), id)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PatchMapping("/api/v1/assignments/{id}")
    public ResponseEntity<ApiResponse<AssignmentResponse>> update(
            Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody UpdateAssignmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                assignmentService.update(Actor.id(authentication), id, request)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/api/v1/assignments/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(Authentication authentication, @PathVariable UUID id) {
        assignmentService.delete(Actor.id(authentication), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/api/v1/assignments/{id}/publish")
    public ResponseEntity<ApiResponse<AssignmentResponse>> publish(Authentication authentication,
                                                                  @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                assignmentService.publish(Actor.id(authentication), id)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/api/v1/assignments/{id}/unpublish")
    public ResponseEntity<ApiResponse<AssignmentResponse>> unpublish(Authentication authentication,
                                                                    @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                assignmentService.unpublish(Actor.id(authentication), id)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/api/v1/assignments/{id}/submissions")
    public ResponseEntity<ApiResponse<List<RosterItem>>> roster(Authentication authentication,
                                                               @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                gradingService.roster(Actor.id(authentication), id)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PatchMapping("/api/v1/submissions/{id}/grade")
    public ResponseEntity<ApiResponse<SubmissionResponse>> grade(
            Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody GradeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                gradingService.grade(Actor.id(authentication), id, request)));
    }
}
