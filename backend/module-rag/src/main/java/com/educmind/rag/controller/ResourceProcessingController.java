package com.educmind.rag.controller;

import com.educmind.common.envelope.ApiResponse;
import com.educmind.rag.dto.ProcessingStatusResponse;
import com.educmind.rag.service.ResourceProcessingStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class ResourceProcessingController {

    private final ResourceProcessingStatusService statusService;

    public ResourceProcessingController(ResourceProcessingStatusService statusService) {
        this.statusService = statusService;
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    @GetMapping("/api/v1/resources/{id}/processing-status")
    public ResponseEntity<ApiResponse<ProcessingStatusResponse>> getProcessingStatus(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(statusService.getStatus(actorId, id)));
    }
}
