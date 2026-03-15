package com.examplatform.enrollment.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "enrollments")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "exam_id", nullable = false)
    private UUID examId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status;

    @Column(name = "amount_paid")
    private BigDecimal amountPaid;

    @Column(name = "enrolled_at", nullable = false)
    private Instant enrolledAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    void prePersist() {
        this.enrolledAt = Instant.now();
        if (this.status == null) this.status = EnrollmentStatus.PENDING;
    }
}
