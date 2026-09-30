package com.educmind.schoolclass.repository;

import com.educmind.schoolclass.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SchoolRepository extends JpaRepository<School, UUID> {
}
