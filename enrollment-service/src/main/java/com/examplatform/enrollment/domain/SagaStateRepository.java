package com.examplatform.enrollment.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SagaStateRepository extends JpaRepository<SagaState, UUID> {
    Optional<SagaState> findByEnrollmentId(UUID enrollmentId);
}
