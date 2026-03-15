-- V1__create_enrollment_tables.sql
-- enrollment-service database schema
-- Flyway managed migration

CREATE TABLE enrollments (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id    UUID        NOT NULL,
    exam_id       UUID        NOT NULL,
    status        VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    -- Status flow:
    -- PENDING → PAYMENT_COMPLETED → EXAM_SCHEDULED
    -- → EXAM_SUBMITTED → GRADED → CERTIFIED
    -- → FAILED | CANCELLED | EXPIRED
    amount_paid   DECIMAL(10,2),
    enrolled_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    completed_at  TIMESTAMP
);

CREATE TABLE saga_state (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    enrollment_id   UUID        NOT NULL REFERENCES enrollments(id),
    saga_status     VARCHAR(30) NOT NULL DEFAULT 'STARTED',
    -- STARTED → PAYMENT_COMPLETED → EXAM_SCHEDULED
    -- → EXAM_SUBMITTED → GRADED → COMPLETED | FAILED
    current_step    VARCHAR(50),
    failure_reason  TEXT,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE TABLE outbox_events (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id    UUID        NOT NULL,
    aggregate_type  VARCHAR(100) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB        NOT NULL,
    topic           VARCHAR(200) NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_enrollments_student   ON enrollments(student_id);
CREATE INDEX idx_enrollments_exam      ON enrollments(exam_id);
CREATE INDEX idx_enrollments_status    ON enrollments(status);
CREATE INDEX idx_saga_enrollment       ON saga_state(enrollment_id);
CREATE INDEX idx_outbox_created        ON outbox_events(created_at);
