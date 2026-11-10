package com.educmind.assessment.controller;

import com.educmind.assessment.dto.AssessmentResponse;
import com.educmind.assessment.dto.AttemptDetailResponse;
import com.educmind.assessment.dto.CreateAssessmentRequest;
import com.educmind.assessment.dto.StudentResultRow;
import com.educmind.assessment.dto.UpdateAssessmentRequest;
import com.educmind.assessment.service.AssessmentService;
import com.educmind.assessment.service.AttemptGradingService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** US-T06 (tạo bài kiểm tra), US-T13 (xuất bản), US-T14 (kết quả). */
@RestController
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final AttemptGradingService gradingService;

    public AssessmentController(AssessmentService assessmentService,
                                AttemptGradingService gradingService) {
        this.assessmentService = assessmentService;
        this.gradingService = gradingService;
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/api/v1/courses/{courseId}/assessments")
    public ResponseEntity<ApiResponse<AssessmentResponse>> create(
            Authentication authentication, @PathVariable UUID courseId,
            @Valid @RequestBody CreateAssessmentRequest request) {
        AssessmentResponse response = assessmentService.create(Actor.id(authentication), courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    @GetMapping("/api/v1/courses/{courseId}/assessments")
    public ResponseEntity<ApiResponse<List<AssessmentResponse>>> list(
            Authentication authentication, @PathVariable UUID courseId) {
        return ResponseEntity.ok(ApiResponse.success(assessmentService.list(
                Actor.id(authentication), Actor.role(authentication), courseId)));
    }

    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    @GetMapping("/api/v1/assessments/{id}")
    public ResponseEntity<ApiResponse<AssessmentResponse>> get(Authentication authentication,
                                                               @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(assessmentService.get(
                Actor.id(authentication), Actor.role(authentication), id)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PatchMapping("/api/v1/assessments/{id}")
    public ResponseEntity<ApiResponse<AssessmentResponse>> update(
            Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody UpdateAssessmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                assessmentService.update(Actor.id(authentication), id, request)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/api/v1/assessments/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(Authentication authentication, @PathVariable UUID id) {
        assessmentService.delete(Actor.id(authentication), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/api/v1/assessments/{id}/publish")
    public ResponseEntity<ApiResponse<AssessmentResponse>> publish(Authentication authentication,
                                                                   @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                assessmentService.publish(Actor.id(authentication), id)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/api/v1/assessments/{id}/unpublish")
    public ResponseEntity<ApiResponse<AssessmentResponse>> unpublish(Authentication authentication,
                                                                     @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                assessmentService.unpublish(Actor.id(authentication), id)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/api/v1/assessments/{id}/results")
    public ResponseEntity<ApiResponse<List<StudentResultRow>>> results(Authentication authentication,
                                                                       @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                gradingService.results(Actor.id(authentication), id)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/api/v1/assessments/{id}/attempts")
    public ResponseEntity<ApiResponse<List<AttemptDetailResponse>>> attempts(
            Authentication authentication, @PathVariable UUID id,
            @RequestParam(name = "status", required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(
                gradingService.attempts(Actor.id(authentication), id, status)));
    }
}
