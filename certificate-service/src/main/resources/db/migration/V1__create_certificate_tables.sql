-- V1__create_certificate_tables.sql
-- certificate-service database schema
-- Flyway managed migration

CREATE TABLE certificates (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    enrollment_id    UUID          NOT NULL UNIQUE,
    student_id       UUID          NOT NULL,
    student_email    VARCHAR(255)  NOT NULL,
    student_name     VARCHAR(255)  NOT NULL,
    exam_id          UUID          NOT NULL,
    exam_title       VARCHAR(255)  NOT NULL,
    score_percentage INT           NOT NULL,
    verify_code      UUID          NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    storage_key      VARCHAR(255),               -- MinIO object key
    download_url     TEXT,                       -- presigned URL (refreshed on demand)
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    issued_at        TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Idempotency table
CREATE TABLE event_log (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id     VARCHAR(255)         NOT NULL UNIQUE,
    event_type   VARCHAR(100),
    processed_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE outbox_events (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id   UUID         NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        JSONB        NOT NULL,
    topic          VARCHAR(200) NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_certificates_student   ON certificates(student_id);
CREATE INDEX idx_certificates_verify    ON certificates(verify_code);
CREATE INDEX idx_certificates_enrollment ON certificates(enrollment_id);
