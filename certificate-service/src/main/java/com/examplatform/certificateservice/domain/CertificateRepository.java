package com.examplatform.certificateservice.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {
    Optional<Certificate> findByVerifyCode(UUID verifyCode);
    Optional<Certificate> findByEnrollmentId(UUID enrollmentId);
    boolean existsByEnrollmentId(UUID enrollmentId);
}
