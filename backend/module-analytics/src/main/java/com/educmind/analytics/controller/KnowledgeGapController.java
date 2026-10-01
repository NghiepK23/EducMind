package com.educmind.analytics.controller;

import com.educmind.analytics.dto.DismissGapRequest;
import com.educmind.analytics.dto.KnowledgeGapResponse;
import com.educmind.analytics.service.KnowledgeGapDismissService;
import com.educmind.common.envelope.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Module05 FINAL mục 5.2: PATCH /api/v1/knowledge-gaps/{id}/dismiss.
 * Phân biệt owner / đồng giảng dạy xử lý ở service (isOwnerOrCoTeacher), @PreAuthorize chỉ chặn role.
 */
@RestController
public class KnowledgeGapController {

    private final KnowledgeGapDismissService dismissService;

    public KnowledgeGapController(KnowledgeGapDismissService dismissService) {
        this.dismissService = dismissService;
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PatchMapping("/api/v1/knowledge-gaps/{id}/dismiss")
    public ResponseEntity<ApiResponse<KnowledgeGapResponse>> dismiss(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody DismissGapRequest request
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        KnowledgeGapResponse response = new KnowledgeGapResponse(
                dismissService.dismiss(actorId, id, request.getReason()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
