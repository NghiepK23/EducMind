package com.educmind.assessment.controller;

import com.educmind.assessment.dto.AttemptDetailResponse;
import com.educmind.assessment.dto.AttemptResponse;
import com.educmind.assessment.dto.GradeAnswerRequest;
import com.educmind.assessment.dto.MyAttemptRow;
import com.educmind.assessment.dto.SaveAnswerRequest;
import com.educmind.assessment.service.AttemptGradingService;
import com.educmind.assessment.service.AttemptService;
import com.educmind.common.envelope.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** US-S03 (làm bài), US-S04 (xem kết quả), US-T15 (chấm tay). */
@RestController
public class AttemptController {

    private final AttemptService attemptService;
    private final AttemptGradingService gradingService;

    public AttemptController(AttemptService attemptService, AttemptGradingService gradingService) {
        this.attemptService = attemptService;
        this.gradingService = gradingService;
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/api/v1/assessments/{assessmentId}/attempts")
    public ResponseEntity<ApiResponse<AttemptResponse>> start(Authentication authentication,
                                                              @PathVariable UUID assessmentId) {
        AttemptResponse response = attemptService.startOrResume(Actor.id(authentication), assessmentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/api/v1/attempts/{id}")
    public ResponseEntity<ApiResponse<AttemptResponse>> get(Authentication authentication,
                                                            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                attemptService.getAttempt(Actor.id(authentication), id)));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PutMapping("/api/v1/attempts/{id}/answers/{questionId}")
    public ResponseEntity<ApiResponse<AttemptResponse>> saveAnswer(
            Authentication authentication, @PathVariable UUID id, @PathVariable UUID questionId,
            @RequestBody SaveAnswerRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                attemptService.saveAnswer(Actor.id(authentication), id, questionId, request)));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/api/v1/attempts/{id}/submit")
    public ResponseEntity<ApiResponse<AttemptResponse>> submit(Authentication authentication,
                                                               @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                attemptService.submit(Actor.id(authentication), id)));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/api/v1/assessments/{id}/my-attempts")
    public ResponseEntity<ApiResponse<List<MyAttemptRow>>> myAttempts(Authentication authentication,
                                                                      @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                attemptService.myAttempts(Actor.id(authentication), id)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/api/v1/attempts/{id}/detail")
    public ResponseEntity<ApiResponse<AttemptDetailResponse>> detail(Authentication authentication,
                                                                     @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                gradingService.attemptDetail(Actor.id(authentication), id)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PatchMapping("/api/v1/attempts/{id}/answers/{questionId}/grade")
    public ResponseEntity<ApiResponse<AttemptDetailResponse>> gradeAnswer(
            Authentication authentication, @PathVariable UUID id, @PathVariable UUID questionId,
            @Valid @RequestBody GradeAnswerRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                gradingService.gradeAnswer(Actor.id(authentication), id, questionId, request)));
    }
}
