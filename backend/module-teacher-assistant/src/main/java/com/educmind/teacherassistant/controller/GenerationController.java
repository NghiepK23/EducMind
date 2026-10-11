package com.educmind.teacherassistant.controller;

import com.educmind.common.envelope.ApiResponse;
import com.educmind.teacherassistant.entity.AiGenerationOutput;
import com.educmind.teacherassistant.entity.AiGenerationRequest;
import com.educmind.teacherassistant.service.GenerationRequestService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class GenerationController {

    public record AcceptedResponse(UUID requestId, String status) {
    }

    public record RequestResponse(UUID id, String requestType, UUID courseId, UUID lessonId,
                                  String status, String errorMessage,
                                  OffsetDateTime createdAt, OffsetDateTime completedAt) {
        static RequestResponse from(AiGenerationRequest r) {
            return new RequestResponse(r.getId(), r.getRequestType(), r.getCourseId(), r.getLessonId(),
                    r.getStatus(), r.getErrorMessage(), r.getCreatedAt(), r.getCompletedAt());
        }
    }

    public record OutputResponse(UUID id, Map<String, Object> content, String reviewStatus,
                                 Map<String, Object> editedContent) {
        static OutputResponse from(AiGenerationOutput o) {
            return new OutputResponse(o.getId(), o.getContent(), o.getReviewStatus(), o.getEditedContent());
        }
    }

    public record DetailResponse(RequestResponse request, List<OutputResponse> outputs) {
    }

    private final GenerationRequestService service;

    public GenerationController(GenerationRequestService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/ai/lesson-plans/generate")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AcceptedResponse>> generateLessonPlan(
            Authentication authentication, @RequestBody Map<String, Object> body) {
        return accept(authentication, AiGenerationRequest.TYPE_LESSON_PLAN, body);
    }

    @PostMapping("/api/v1/ai/questions/generate")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AcceptedResponse>> generateQuestions(
            Authentication authentication, @RequestBody Map<String, Object> body) {
        return accept(authentication, AiGenerationRequest.TYPE_QUESTION_SET, body);
    }

    @PostMapping("/api/v1/ai/assessment-matrix/generate")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AcceptedResponse>> generateMatrix(
            Authentication authentication, @RequestBody Map<String, Object> body) {
        return accept(authentication, AiGenerationRequest.TYPE_ASSESSMENT_MATRIX, body);
    }

    @GetMapping("/api/v1/ai/generation-requests/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<DetailResponse>> detail(
            Authentication authentication, @PathVariable("id") UUID id) {
        UUID actorId = UUID.fromString(authentication.getName());
        AiGenerationRequest req = service.find(actorId, id);
        List<OutputResponse> outputs = service.outputsOf(id).stream().map(OutputResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(
                new DetailResponse(RequestResponse.from(req), outputs)));
    }

    @GetMapping("/api/v1/ai/generation-requests")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<RequestResponse>>> list(
            Authentication authentication,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        UUID actorId = UUID.fromString(authentication.getName());
        Page<AiGenerationRequest> result = service.list(actorId, page, size);

        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("page", result.getNumber() + 1);
        pagination.put("size", result.getSize());
        pagination.put("total_items", result.getTotalElements());
        pagination.put("total_pages", result.getTotalPages());
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("pagination", pagination);

        List<RequestResponse> data = result.getContent().stream().map(RequestResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(data, meta));
    }

    private ResponseEntity<ApiResponse<AcceptedResponse>> accept(
            Authentication authentication, String type, Map<String, Object> body) {
        UUID actorId = UUID.fromString(authentication.getName());
        AiGenerationRequest req = service.create(actorId, type, body);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(new AcceptedResponse(req.getId(), req.getStatus())));
    }
}
