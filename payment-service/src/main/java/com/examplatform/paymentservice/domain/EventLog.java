package com.examplatform.paymentservice.domain;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "event_log")
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class EventLog {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;
    @Column(name = "event_type")
    private String eventType;
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
    @PrePersist void prePersist() { this.processedAt = Instant.now(); }
}
