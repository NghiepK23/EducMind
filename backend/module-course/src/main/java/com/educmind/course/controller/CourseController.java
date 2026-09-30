package com.educmind.course.controller;

import com.educmind.common.envelope.ApiResponse;
import com.educmind.course.dto.AddStudentsToCourseRequest;
import com.educmind.course.dto.AddTeacherToCourseRequest;
import com.educmind.course.dto.CourseMemberResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Module03_Course_Lesson_Resource_FINAL.md, mục C.6 — đã cập nhật theo kiến trúc mới
 * (Checkpoint #3): bỏ course-class linking (course_class đã xóa), thay bằng course_members
 * (giáo viên đồng giảng dạy + học sinh) quản lý trực tiếp trong Course.
 */
@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(
            Authentication authentication,
            @Valid @RequestBody CreateCourseRequest request
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        CourseResponse response = courseService.createCourse(actorId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /** [TODO - bước Lesson] Thêm 'STUDENT' khi CourseService hỗ trợ xem course có lesson PUBLISHED. */
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseResponse>>> listCourses(
            Authentication authentication
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        List<CourseResponse> response = courseService.listCourses(actorId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** [TODO - bước Lesson] Thêm 'STUDENT' cùng lý do với listCourses ở trên. */
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourseDetail(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        CourseResponse response = courseService.getCourseDetail(actorId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody UpdateCourseRequest request
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        CourseResponse response = courseService.updateCourse(actorId, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        courseService.deleteCourse(actorId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/{id}/members/students")
    public ResponseEntity<ApiResponse<List<CourseMemberResponse>>> addStudentsToCourse(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody AddStudentsToCourseRequest request
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        List<CourseMemberResponse> response = courseService.addStudentsToCourse(actorId, id, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/{id}/members/teachers")
    public ResponseEntity<ApiResponse<List<CourseMemberResponse>>> addTeacherToCourse(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody AddTeacherToCourseRequest request
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        List<CourseMemberResponse> response = courseService.addTeacherToCourse(actorId, id, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            Authentication authentication,
            @PathVariable UUID id,
            @PathVariable UUID userId
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        courseService.removeMember(actorId, id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    @GetMapping("/{id}/members")
    public ResponseEntity<ApiResponse<List<CourseMemberResponse>>> listMembers(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        List<CourseMemberResponse> response = courseService.listMembers(actorId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
