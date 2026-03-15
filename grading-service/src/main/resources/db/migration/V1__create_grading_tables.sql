-- V1__create_grading_tables.sql
-- grading-service database schema
-- Flyway managed migration
-- NOTE: answer_keys are stored ONLY here (not in exam-service) for security

CREATE TABLE answer_keys (
    id              UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id     UUID    NOT NULL UNIQUE,
    exam_id         UUID    NOT NULL,
    correct_option  VARCHAR(1) NOT NULL  -- A, B, C, or D
);

CREATE TABLE grade_results (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    enrollment_id    UUID         NOT NULL UNIQUE,
    slot_id          UUID         NOT NULL,
    student_id       UUID         NOT NULL,
    exam_id          UUID         NOT NULL,
    student_email    VARCHAR(255),
    student_name     VARCHAR(255),
    correct_count    INT          NOT NULL,
    total_questions  INT          NOT NULL,
    score_percentage INT          NOT NULL,  -- 0–100
    passing_score    INT          NOT NULL,  -- threshold from exam
    passed           BOOLEAN      NOT NULL,
    tab_switch_count INT,
    graded_at        TIMESTAMP    NOT NULL DEFAULT NOW()
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

CREATE INDEX idx_answer_keys_exam      ON answer_keys(exam_id);
CREATE INDEX idx_grade_results_student ON grade_results(student_id);
CREATE INDEX idx_grade_results_exam    ON grade_results(exam_id);
