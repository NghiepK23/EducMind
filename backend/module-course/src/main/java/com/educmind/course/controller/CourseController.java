package com.educmind.course.controller;

import com.educmind.common.envelope.ApiResponse;
import com.educmind.course.dto.CourseResponse;
import com.educmind.course.dto.CreateCourseRequest;
import com.educmind.course.dto.UpdateCourseRequest;
import com.educmind.course.service.CourseService;
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

/** US-T01 / AC-T01.1-2 — khóa học thuộc lớp do giáo viên phụ trách. */
@RestController
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/api/v1/classes/{classId}/courses")
    public ResponseEntity<ApiResponse<CourseResponse>> create(
            Authentication authentication,
            @PathVariable UUID classId,
            @Valid @RequestBody CreateCourseRequest request) {
        CourseResponse response = courseService.create(Actor.id(authentication), classId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    @GetMapping("/api/v1/classes/{classId}/courses")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> listByClass(
            Authentication authentication,
            @PathVariable UUID classId) {
        return ResponseEntity.ok(ApiResponse.success(
                courseService.listForClass(Actor.id(authentication), Actor.role(authentication), classId)));
    }

    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    @GetMapping("/api/v1/courses")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> listMine(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                courseService.listMine(Actor.id(authentication), Actor.role(authentication))));
    }

    @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
    @GetMapping("/api/v1/courses/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> get(Authentication authentication,
                                                           @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                courseService.get(Actor.id(authentication), Actor.role(authentication), id)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PatchMapping("/api/v1/courses/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> update(Authentication authentication,
                                                              @PathVariable UUID id,
                                                              @Valid @RequestBody UpdateCourseRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                courseService.update(Actor.id(authentication), id, request)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/api/v1/courses/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(Authentication authentication, @PathVariable UUID id) {
        courseService.delete(Actor.id(authentication), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
