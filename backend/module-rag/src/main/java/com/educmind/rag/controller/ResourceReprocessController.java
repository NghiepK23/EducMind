package com.educmind.rag.controller;

import com.educmind.common.envelope.ApiResponse;
import com.educmind.rag.dto.ReprocessResponse;
import com.educmind.rag.service.ResourceReprocessService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class ResourceReprocessController {

    private final ResourceReprocessService reprocessService;

    public ResourceReprocessController(ResourceReprocessService reprocessService) {
        this.reprocessService = reprocessService;
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/api/v1/resources/{id}/reprocess")
    public ResponseEntity<ApiResponse<ReprocessResponse>> reprocess(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(reprocessService.reprocess(actorId, id)));
    }
}
