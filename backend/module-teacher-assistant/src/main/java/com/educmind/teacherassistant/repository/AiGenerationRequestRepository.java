package com.educmind.teacherassistant.repository;

import com.educmind.teacherassistant.entity.AiGenerationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface AiGenerationRequestRepository extends JpaRepository<AiGenerationRequest, UUID> {

    Page<AiGenerationRequest> findByRequestedBy(UUID requestedBy, Pageable pageable);

    long countByRequestedByAndCreatedAtAfter(UUID requestedBy, OffsetDateTime since);

    /**
     * Đánh FAILED mọi request còn PROCESSING mà tạo trước cutoff (bị kẹt do app tắt giữa chừng).
     * Điều kiện status = 'PROCESSING' nằm ngay trong câu UPDATE nên request vừa COMPLETED sẽ không bị ghi đè.
     */
    @Modifying
    @Query("update AiGenerationRequest r set r.status = 'FAILED', r.errorMessage = :message, "
            + "r.completedAt = :now where r.status = 'PROCESSING' and r.createdAt < :cutoff")
    int failStuck(@Param("cutoff") OffsetDateTime cutoff,
                  @Param("now") OffsetDateTime now,
                  @Param("message") String message);
}
