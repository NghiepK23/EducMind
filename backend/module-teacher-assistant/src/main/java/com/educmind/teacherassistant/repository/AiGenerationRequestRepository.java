package com.educmind.teacherassistant.repository;

import com.educmind.teacherassistant.entity.AiGenerationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface AiGenerationRequestRepository extends JpaRepository<AiGenerationRequest, UUID> {

    Page<AiGenerationRequest> findByRequestedBy(UUID requestedBy, Pageable pageable);

    long countByRequestedByAndCreatedAtAfter(UUID requestedBy, OffsetDateTime since);
}
