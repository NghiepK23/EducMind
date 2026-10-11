package com.educmind.reports.repository;

import com.educmind.reports.entity.ReportExportRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReportExportRequestRepository extends JpaRepository<ReportExportRequest, UUID> {

    Optional<ReportExportRequest> findByIdAndRequestedBy(UUID id, UUID requestedBy);
}
