package com.educmind.course.controller;

import com.educmind.common.envelope.ApiResponse;
import com.educmind.course.dto.ObjectiveRequest;
import com.educmind.course.dto.ObjectiveResponse;
import com.educmind.course.service.ObjectiveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Mục tiêu học tập của bài học — khoá tham chiếu cho câu hỏi / bằng chứng / mastery. */
@RestController
public class ObjectiveController {

    private final ObjectiveService objectiveService;

    public ObjectiveController(ObjectiveService objectiveService) {
        this.objectiveService = objectiveService;
    }

    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    @GetMapping("/api/v1/lessons/{lessonId}/objectives")
    public ResponseEntity<ApiResponse<List<ObjectiveResponse>>> list(Authentication authentication,
                                                                     @PathVariable UUID lessonId) {
        return ResponseEntity.ok(ApiResponse.success(objectiveService.list(
                Actor.id(authentication), Actor.role(authentication), lessonId)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/api/v1/lessons/{lessonId}/objectives")
    public ResponseEntity<ApiResponse<ObjectiveResponse>> create(Authentication authentication,
                                                                 @PathVariable UUID lessonId,
                                                                 @Valid @RequestBody ObjectiveRequest request) {
        ObjectiveResponse response = objectiveService.create(Actor.id(authentication), lessonId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/api/v1/objectives/{id}")
    public ResponseEntity<ApiResponse<ObjectiveResponse>> update(Authentication authentication,
                                                                 @PathVariable UUID id,
                                                                 @Valid @RequestBody ObjectiveRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                objectiveService.update(Actor.id(authentication), id, request)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/api/v1/objectives/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(Authentication authentication, @PathVariable UUID id) {
        objectiveService.delete(Actor.id(authentication), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
