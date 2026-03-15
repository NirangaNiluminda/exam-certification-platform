package com.examplatform.gradingservice.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GradeResultRepository extends JpaRepository<GradeResult, UUID> {
    Optional<GradeResult> findByEnrollmentId(UUID enrollmentId);
    boolean existsByEnrollmentId(UUID enrollmentId);
}
