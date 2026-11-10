package com.educmind.schoolclass.repository;

import com.educmind.schoolclass.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, UUID> {

    Optional<SchoolClass> findByIdAndDeletedAtIsNull(UUID id);

    List<SchoolClass> findByOwnerTeacherIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID ownerTeacherId);

    boolean existsByIdAndDeletedAtIsNull(UUID id);
}
