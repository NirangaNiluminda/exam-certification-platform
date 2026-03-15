package com.examplatform.examservice.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExamSlotRepository extends JpaRepository<ExamSlot, UUID> {
    Optional<ExamSlot> findByEnrollmentId(UUID enrollmentId);
    boolean existsByEnrollmentId(UUID enrollmentId);
}
