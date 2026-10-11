package com.educmind.teacherassistant.controller;

import com.educmind.common.envelope.ApiResponse;
import com.educmind.teacherassistant.service.GenerationReviewService;
import com.educmind.teacherassistant.service.GenerationReviewService.ReviewResult;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
public class GenerationReviewController {

    private final GenerationReviewService service;

    public GenerationReviewController(GenerationReviewService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/ai/generation-outputs/{id}/review")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ReviewResult>> review(
            Authentication authentication, @PathVariable("id") UUID id,
            @RequestBody Map<String, Object> body) {
        UUID actorId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(service.review(actorId, id, body)));
    }
}
