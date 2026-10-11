package com.educmind.teacherassistant.repository;

import com.educmind.teacherassistant.entity.AiObservabilityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiObservabilityLogRepository extends JpaRepository<AiObservabilityLog, UUID> {
}
