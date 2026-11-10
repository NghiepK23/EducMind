package com.educmind.assessment.controller;

import com.educmind.assessment.dto.CreateQuestionRequest;
import com.educmind.assessment.dto.QuestionResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** US-T05 — ngân hàng câu hỏi (chỉ giáo viên). */
@RestController
@PreAuthorize("hasRole('TEACHER')")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping("/api/v1/courses/{courseId}/questions")
    public ResponseEntity<ApiResponse<QuestionResponse>> create(
            Authentication authentication, @PathVariable UUID courseId,
            @Valid @RequestBody CreateQuestionRequest request) {
        QuestionResponse response = questionService.create(Actor.id(authentication), courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/api/v1/courses/{courseId}/questions")
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> list(
            Authentication authentication, @PathVariable UUID courseId,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "difficulty", required = false) String difficulty) {
        return ResponseEntity.ok(ApiResponse.success(
                questionService.list(Actor.id(authentication), courseId, type, difficulty)));
    }

    @GetMapping("/api/v1/questions/{id}")
    public ResponseEntity<ApiResponse<QuestionResponse>> get(Authentication authentication,
                                                             @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                questionService.get(Actor.id(authentication), Actor.role(authentication), id)));
    }

    @PatchMapping("/api/v1/questions/{id}")
    public ResponseEntity<ApiResponse<QuestionResponse>> update(
            Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody UpdateQuestionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                questionService.update(Actor.id(authentication), id, request)));
    }

    @DeleteMapping("/api/v1/questions/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(Authentication authentication, @PathVariable UUID id) {
        questionService.delete(Actor.id(authentication), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
