package com.examplatform.enrollment.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saga_state")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SagaState {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "saga_status", nullable = false)
    private SagaStatus sagaStatus;

    @Column(name = "current_step")
    private String currentStep;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = this.updatedAt = Instant.now();
        if (this.sagaStatus == null) this.sagaStatus = SagaStatus.STARTED;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
