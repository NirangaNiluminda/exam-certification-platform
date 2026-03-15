package com.examplatform.paymentservice.domain;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByEnrollmentId(UUID enrollmentId);
}
