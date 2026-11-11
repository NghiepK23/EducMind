package com.educmind.course.controller;

import com.educmind.common.envelope.ApiResponse;
import com.educmind.course.dto.CreateLessonRequest;
import com.educmind.course.dto.LessonResponse;
import com.educmind.course.dto.UpdateLessonRequest;
import com.educmind.course.service.LessonService;
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

/** US-T02 (bài học) + US-S01 (học sinh chỉ thấy bài học đã xuất bản). */
@RestController
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    @GetMapping("/api/v1/topics/{topicId}/lessons")
    public ResponseEntity<ApiResponse<List<LessonResponse>>> listByTopic(
            Authentication authentication, @PathVariable UUID topicId) {
        return ResponseEntity.ok(ApiResponse.success(lessonService.listByTopic(
                Actor.id(authentication), Actor.role(authentication), topicId)));
    }

    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    @GetMapping("/api/v1/courses/{courseId}/lessons")
    public ResponseEntity<ApiResponse<List<LessonResponse>>> listByCourse(
            Authentication authentication, @PathVariable UUID courseId) {
        return ResponseEntity.ok(ApiResponse.success(lessonService.listByCourse(
                Actor.id(authentication), Actor.role(authentication), courseId)));
    }

    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    @GetMapping("/api/v1/lessons/{id}")
    public ResponseEntity<ApiResponse<LessonResponse>> get(Authentication authentication,
                                                           @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(lessonService.get(
                Actor.id(authentication), Actor.role(authentication), id)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/api/v1/topics/{topicId}/lessons")
    public ResponseEntity<ApiResponse<LessonResponse>> create(Authentication authentication,
                                                              @PathVariable UUID topicId,
                                                              @Valid @RequestBody CreateLessonRequest request) {
        LessonResponse response = lessonService.create(Actor.id(authentication), topicId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PatchMapping("/api/v1/lessons/{id}")
    public ResponseEntity<ApiResponse<LessonResponse>> update(Authentication authentication,
                                                              @PathVariable UUID id,
                                                              @Valid @RequestBody UpdateLessonRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                lessonService.update(Actor.id(authentication), id, request)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/api/v1/lessons/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(Authentication authentication, @PathVariable UUID id) {
        lessonService.delete(Actor.id(authentication), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/api/v1/lessons/{id}/publish")
    public ResponseEntity<ApiResponse<LessonResponse>> publish(Authentication authentication,
                                                               @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                lessonService.setPublished(Actor.id(authentication), id, true)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/api/v1/lessons/{id}/unpublish")
    public ResponseEntity<ApiResponse<LessonResponse>> unpublish(Authentication authentication,
                                                                 @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                lessonService.setPublished(Actor.id(authentication), id, false)));
    }
}
