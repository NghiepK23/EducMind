package com.educmind.rag.repository;

import com.educmind.rag.entity.ResourceProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResourceProcessingJobRepository extends JpaRepository<ResourceProcessingJob, UUID> {

    List<ResourceProcessingJob> findByResourceIdOrderByCreatedAtAsc(UUID resourceId);

    void deleteByResourceId(UUID resourceId);
}
