package com.examplatform.outbox;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbox event entity — written atomically with domain changes in the same DB transaction.
 * Debezium CDC reads this table via PostgreSQL WAL and publishes to Kafka.
 */
@Entity
@Table(name = "outbox_events")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** ID of the domain aggregate this event belongs to (e.g. enrollmentId, paymentId) */
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    /** Type of aggregate (e.g. "Enrollment", "Payment", "ExamSlot") */
    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    /** Event type (e.g. "ENROLLMENT_INITIATED", "PAYMENT_COMPLETED") */
    @Column(name = "event_type", nullable = false)
    private String eventType;

    /** JSON payload — the full event data consumed by downstream services */
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload;

    /** Kafka topic this event should be routed to by Debezium Outbox Event Router */
    @Column(name = "topic", nullable = false)
    private String topic;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }
}
