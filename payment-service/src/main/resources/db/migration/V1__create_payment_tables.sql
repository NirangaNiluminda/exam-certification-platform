-- V1__create_payment_tables.sql
-- payment-service database schema
-- Flyway managed migration

CREATE TABLE payments (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    enrollment_id   UUID         NOT NULL UNIQUE,
    student_id      UUID         NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    -- PENDING → COMPLETED | FAILED | REFUNDED
    payment_method  VARCHAR(50)  DEFAULT 'MOCK_CARD',
    processed_at    TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Idempotency table: prevents double-processing duplicate Kafka events
CREATE TABLE event_log (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        VARCHAR(255)         NOT NULL UNIQUE,
    event_type      VARCHAR(100),
    processed_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE outbox_events (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id    UUID         NOT NULL,
    aggregate_type  VARCHAR(100) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB        NOT NULL,
    topic           VARCHAR(200) NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_enrollment ON payments(enrollment_id);
CREATE INDEX idx_payments_student    ON payments(student_id);
CREATE INDEX idx_event_log_event_id  ON event_log(event_id);
