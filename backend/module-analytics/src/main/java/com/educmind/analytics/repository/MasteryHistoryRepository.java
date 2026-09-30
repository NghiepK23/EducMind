package com.educmind.analytics.repository;

import com.educmind.analytics.entity.MasteryHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MasteryHistoryRepository extends JpaRepository<MasteryHistory, UUID> {
}
