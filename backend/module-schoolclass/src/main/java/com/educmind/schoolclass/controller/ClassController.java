package com.educmind.schoolclass.controller;

import com.educmind.common.envelope.ApiResponse;
import com.educmind.schoolclass.dto.AddStudentRequest;
import com.educmind.schoolclass.dto.ClassResponse;
import com.educmind.schoolclass.dto.CreateClassRequest;
import com.educmind.schoolclass.dto.StudentResponse;
import com.educmind.schoolclass.dto.UpdateClassRequest;
import com.educmind.schoolclass.service.ClassService;
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

/** US-T00 — quản lý lớp học và học sinh của lớp. */
@RestController
@RequestMapping("/api/v1/classes")
@PreAuthorize("hasRole('TEACHER')")
public class ClassController {

    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ClassResponse>> create(
            Authentication authentication, @Valid @RequestBody CreateClassRequest request) {
        ClassResponse response = classService.create(UUID.fromString(authentication.getName()), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClassResponse>>> list(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                classService.listForTeacher(UUID.fromString(authentication.getName()))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClassResponse>> get(Authentication authentication,
                                                          @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                classService.get(UUID.fromString(authentication.getName()), id)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ClassResponse>> update(Authentication authentication,
                                                             @PathVariable UUID id,
                                                             @Valid @RequestBody UpdateClassRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                classService.update(UUID.fromString(authentication.getName()), id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(Authentication authentication, @PathVariable UUID id) {
        classService.delete(UUID.fromString(authentication.getName()), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}/students")
    public ResponseEntity<ApiResponse<List<StudentResponse>>> students(Authentication authentication,
                                                                      @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                classService.listStudents(UUID.fromString(authentication.getName()), id)));
    }

    @PostMapping("/{id}/students")
    public ResponseEntity<ApiResponse<StudentResponse>> addStudent(Authentication authentication,
                                                                   @PathVariable UUID id,
                                                                   @Valid @RequestBody AddStudentRequest request) {
        StudentResponse response = classService.addStudent(
                UUID.fromString(authentication.getName()), id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}/students/{studentId}")
    public ResponseEntity<ApiResponse<Void>> removeStudent(Authentication authentication,
                                                           @PathVariable UUID id,
                                                           @PathVariable UUID studentId) {
        classService.removeStudent(UUID.fromString(authentication.getName()), id, studentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
