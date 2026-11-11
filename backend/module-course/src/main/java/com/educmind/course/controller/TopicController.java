package com.educmind.course.controller;

import com.educmind.common.envelope.ApiResponse;
import com.educmind.course.dto.CreateTopicRequest;
import com.educmind.course.dto.TopicResponse;
import com.educmind.course.dto.UpdateTopicRequest;
import com.educmind.course.service.TopicService;
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

/** US-T02A — quản lý chủ đề của khóa học. */
@RestController
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    @GetMapping("/api/v1/courses/{courseId}/topics")
    public ResponseEntity<ApiResponse<List<TopicResponse>>> list(Authentication authentication,
                                                                 @PathVariable UUID courseId) {
        return ResponseEntity.ok(ApiResponse.success(topicService.list(
                Actor.id(authentication), Actor.role(authentication), courseId)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/api/v1/courses/{courseId}/topics")
    public ResponseEntity<ApiResponse<TopicResponse>> create(Authentication authentication,
                                                             @PathVariable UUID courseId,
                                                             @Valid @RequestBody CreateTopicRequest request) {
        TopicResponse response = topicService.create(Actor.id(authentication), courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PatchMapping("/api/v1/topics/{id}")
    public ResponseEntity<ApiResponse<TopicResponse>> update(Authentication authentication,
                                                             @PathVariable UUID id,
                                                             @Valid @RequestBody UpdateTopicRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                topicService.update(Actor.id(authentication), id, request)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/api/v1/topics/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(Authentication authentication, @PathVariable UUID id) {
        topicService.delete(Actor.id(authentication), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/api/v1/topics/{id}/publish")
    public ResponseEntity<ApiResponse<TopicResponse>> publish(Authentication authentication,
                                                              @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                topicService.setPublished(Actor.id(authentication), id, true)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/api/v1/topics/{id}/unpublish")
    public ResponseEntity<ApiResponse<TopicResponse>> unpublish(Authentication authentication,
                                                                @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                topicService.setPublished(Actor.id(authentication), id, false)));
    }
}
