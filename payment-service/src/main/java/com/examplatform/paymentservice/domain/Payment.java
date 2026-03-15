package com.examplatform.paymentservice.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "payments")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "enrollment_id", nullable = false, unique = true)
    private UUID enrollmentId;
    @Column(name = "student_id", nullable = false)
    private UUID studentId;
    @Column(nullable = false)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
    @Column(name = "processed_at")
    private Instant processedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @PrePersist void prePersist() { this.createdAt = Instant.now(); }
}
