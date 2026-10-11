package com.educmind.teacherassistant.repository;

import com.educmind.teacherassistant.entity.AiGenerationOutput;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiGenerationOutputRepository extends JpaRepository<AiGenerationOutput, UUID> {

    List<AiGenerationOutput> findByRequestId(UUID requestId);

    /** Khóa dòng khi duyệt, để 2 lần duyệt đồng thời không tạo câu hỏi trùng. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from AiGenerationOutput o where o.id = :id")
    Optional<AiGenerationOutput> findByIdForUpdate(@Param("id") UUID id);
}
