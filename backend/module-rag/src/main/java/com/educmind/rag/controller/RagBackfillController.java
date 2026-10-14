package com.educmind.rag.controller;

import com.educmind.common.envelope.ApiResponse;
import com.educmind.rag.dto.BackfillResponse;
import com.educmind.rag.service.RagBackfillService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RagBackfillController {

    private final RagBackfillService backfillService;

    public RagBackfillController(RagBackfillService backfillService) {
        this.backfillService = backfillService;
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/api/v1/admin/rag/backfill")
    public ResponseEntity<ApiResponse<BackfillResponse>> backfill() {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(backfillService.start()));
    }
}
