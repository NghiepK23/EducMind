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

/**
 * Module03_Course_Lesson_Resource_FINAL.md, mục C.6.
 *
 * Không dùng @RequestMapping ở class (khác CourseController) vì endpoint của Lesson trải
 * trên 2 base path khác nhau: /api/v1/courses/{id}/lessons (tạo/liệt kê, lồng trong Course)
 * và /api/v1/lessons/{id} (thao tác trực tiếp trên 1 lesson cụ thể) — ghi full path ở từng
 * method cho rõ ràng, tránh nhầm lẫn khi 2 base path khác nhau dùng chung 1 prefix.
 */
@RestController
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/api/v1/courses/{courseId}/lessons")
    public ResponseEntity<ApiResponse<LessonResponse>> createLesson(
            Authentication authentication,
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateLessonRequest request
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        LessonResponse response = lessonService.createLesson(actorId, courseId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'STUDENT')")
    @GetMapping("/api/v1/courses/{courseId}/lessons")
    public ResponseEntity<ApiResponse<List<LessonResponse>>> listLessons(
            Authentication authentication,
            @PathVariable UUID courseId
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        List<LessonResponse> response = lessonService.listLessons(actorId, courseId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'STUDENT')")
    @GetMapping("/api/v1/lessons/{id}")
    public ResponseEntity<ApiResponse<LessonResponse>> getLessonDetail(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        LessonResponse response = lessonService.getLessonDetail(actorId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PatchMapping("/api/v1/lessons/{id}")
    public ResponseEntity<ApiResponse<LessonResponse>> updateLesson(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLessonRequest request
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        LessonResponse response = lessonService.updateLesson(actorId, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/api/v1/lessons/{id}")
    public ResponseEntity<ApiResponse<LessonResponse>> deleteLesson(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        LessonResponse response = lessonService.deleteLesson(actorId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PatchMapping("/api/v1/lessons/{id}/publish")
    public ResponseEntity<ApiResponse<LessonResponse>> publishLesson(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        LessonResponse response = lessonService.publishLesson(actorId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PatchMapping("/api/v1/lessons/{id}/unpublish")
    public ResponseEntity<ApiResponse<LessonResponse>> unpublishLesson(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        LessonResponse response = lessonService.unpublishLesson(actorId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
