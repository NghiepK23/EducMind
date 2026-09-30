package com.educmind.assessment.controller;

import com.educmind.assessment.dto.CreateQuestionRequest;
import com.educmind.assessment.dto.QuestionResponse;
import com.educmind.assessment.dto.ReviewQuestionRequest;
import com.educmind.assessment.dto.UpdateQuestionRequest;
import com.educmind.assessment.service.QuestionService;
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
 * Module04_Assignment_Assessment_FINAL.md, PHẦN E, mục E.6 (Question Bank).
 *
 * Không dùng @RequestMapping ở class — cùng lý do như AssignmentController:
 * endpoint trải trên nhiều base path khác nhau (/courses/{id}/questions,
 * /questions/{id}, /questions/{id}/review).
 *
 * Mọi endpoint dùng hasRole('TEACHER') — việc phân biệt owner hay đồng giảng dạy
 * do QuestionService xử lý ở tầng nghiệp vụ (isOwnerOrCoTeacher), không phải
 * ở @PreAuthorize, giống nguyên tắc đã áp dụng ở AssignmentController.
 */
@RestController
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/api/v1/courses/{courseId}/questions")
    public ResponseEntity<ApiResponse<QuestionResponse>> createQuestion(
            Authentication authentication,
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateQuestionRequest request
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        QuestionResponse response = questionService.createQuestion(actorId, courseId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/api/v1/courses/{courseId}/questions")
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> listQuestions(
            Authentication authentication,
            @PathVariable UUID courseId
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        List<QuestionResponse> response = questionService.listQuestions(actorId, courseId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PatchMapping("/api/v1/questions/{id}")
    public ResponseEntity<ApiResponse<QuestionResponse>> updateQuestion(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateQuestionRequest request
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        QuestionResponse response = questionService.updateQuestion(actorId, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PatchMapping("/api/v1/questions/{id}/review")
    public ResponseEntity<ApiResponse<QuestionResponse>> reviewQuestion(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody ReviewQuestionRequest request
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        QuestionResponse response = questionService.reviewQuestion(actorId, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/api/v1/questions/{id}")
    public ResponseEntity<ApiResponse<QuestionResponse>> deleteQuestion(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        QuestionResponse response = questionService.deleteQuestion(actorId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
