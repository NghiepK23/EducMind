package com.educmind.assessment.controller;

import com.educmind.assessment.dto.AddQuestionToAssessmentRequest;
import com.educmind.assessment.dto.AssessmentAttemptDetailResponse;
import com.educmind.assessment.dto.AssessmentAttemptResponse;
import com.educmind.assessment.dto.AssessmentQuestionResponse;
import com.educmind.assessment.dto.AssessmentResponse;
import com.educmind.assessment.dto.CreateAssessmentRequest;
import com.educmind.assessment.dto.GradeAnswerRequest;
import com.educmind.assessment.dto.SaveAnswerRequest;
import com.educmind.assessment.dto.SubmissionAnswerResponse;
import com.educmind.assessment.service.AssessmentService;
import com.educmind.common.envelope.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Module04_Assignment_Assessment_FINAL.md, PHẦN E, mục E.6 (Assessment/Attempt/Grading).
 *
 * Không dùng @RequestMapping ở class — cùng lý do như AssignmentController/QuestionController.
 *
 * Không có endpoint POST /api/v1/assessments/{id}/classes (gắn đề với lớp) — kiến trúc đã
 * lệch khỏi spec gốc từ Module 3: không tồn tại bảng assignment_class/assessment_class,
 * publish áp dụng cho toàn bộ course_members (STUDENT, left_at IS NULL) của course, xem
 * decisions.md mục "Domain concepts".
 *
 * getAttempt dùng hasAnyRole('TEACHER','STUDENT') vì actor có thể là chủ attempt (STUDENT)
 * hoặc giáo viên owner/đồng giảng dạy — AssessmentService.getAttempt tự phân biệt 2 trường
 * hợp này ở tầng nghiệp vụ.
 */
@RestController
public class AssessmentController {

    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/api/v1/courses/{courseId}/assessments")
    public ResponseEntity<ApiResponse<AssessmentResponse>> createAssessment(
            Authentication authentication,
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateAssessmentRequest request
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        AssessmentResponse response = assessmentService.createAssessment(actorId, courseId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/api/v1/assessments/{id}/questions")
    public ResponseEntity<ApiResponse<AssessmentQuestionResponse>> addQuestion(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody AddQuestionToAssessmentRequest request
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        AssessmentQuestionResponse response = assessmentService.addQuestion(actorId, id, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PatchMapping("/api/v1/assessments/{id}/publish")
    public ResponseEntity<ApiResponse<AssessmentResponse>> publishAssessment(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        AssessmentResponse response = assessmentService.publishAssessment(actorId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/api/v1/assessments/{id}/attempts")
    public ResponseEntity<ApiResponse<AssessmentAttemptResponse>> startAttempt(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID studentId = UUID.fromString(authentication.getName());
        AssessmentAttemptResponse response = assessmentService.startAttempt(studentId, id);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PatchMapping("/api/v1/attempts/{id}/answers")
    public ResponseEntity<ApiResponse<SubmissionAnswerResponse>> saveAnswer(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody SaveAnswerRequest request
    ) {
        UUID studentId = UUID.fromString(authentication.getName());
        SubmissionAnswerResponse response = assessmentService.saveAnswer(studentId, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/api/v1/attempts/{id}/submit")
    public ResponseEntity<ApiResponse<AssessmentAttemptResponse>> submitAttempt(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID studentId = UUID.fromString(authentication.getName());
        AssessmentAttemptResponse response = assessmentService.submitAttempt(studentId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    @GetMapping("/api/v1/attempts/{id}")
    public ResponseEntity<ApiResponse<AssessmentAttemptDetailResponse>> getAttempt(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        AssessmentAttemptDetailResponse response = assessmentService.getAttempt(actorId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PatchMapping("/api/v1/attempts/{id}/grade")
    public ResponseEntity<ApiResponse<AssessmentAttemptResponse>> gradeAnswer(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody GradeAnswerRequest request
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        AssessmentAttemptResponse response = assessmentService.gradeAnswer(actorId, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
