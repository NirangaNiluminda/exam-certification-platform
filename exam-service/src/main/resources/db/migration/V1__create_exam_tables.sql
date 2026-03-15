-- V1__create_exam_tables.sql
-- exam-service database schema
-- Flyway managed migration

CREATE TABLE exams (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    title            VARCHAR(255)  NOT NULL,
    description      TEXT,
    duration_minutes INT           NOT NULL,
    passing_score    INT           NOT NULL DEFAULT 70,  -- percentage
    total_marks      INT           NOT NULL,
    price            DECIMAL(10,2) NOT NULL,
    active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_by       UUID,  -- admin user id
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Questions belong to exams. Correct answers are stored ONLY in grading-service (security)
CREATE TABLE questions (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id         UUID         NOT NULL REFERENCES exams(id),
    question_text   TEXT         NOT NULL,
    option_a        VARCHAR(500) NOT NULL,
    option_b        VARCHAR(500) NOT NULL,
    option_c        VARCHAR(500) NOT NULL,
    option_d        VARCHAR(500) NOT NULL,
    marks           INT          NOT NULL DEFAULT 1,
    sort_order      INT          NOT NULL DEFAULT 0
);

-- Exam slots: one per enrollment, tracks the student's active exam session
CREATE TABLE exam_slots (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    enrollment_id    UUID        NOT NULL UNIQUE,
    exam_id          UUID        NOT NULL REFERENCES exams(id),
    student_id       UUID        NOT NULL,
    student_email    VARCHAR(255),
    student_name     VARCHAR(255),
    status           VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    -- SCHEDULED → ACTIVE → SUBMITTED | EXPIRED
    started_at       TIMESTAMP,
    submitted_at     TIMESTAMP,
    deadline_at      TIMESTAMP,
    tab_switch_count INT         DEFAULT 0,
    created_at       TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- Student answers for each exam attempt
CREATE TABLE answers (
    id               UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    slot_id          UUID      NOT NULL REFERENCES exam_slots(id),
    question_id      UUID      NOT NULL REFERENCES questions(id),
    selected_option  VARCHAR(1),  -- A, B, C, or D
    answered_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (slot_id, question_id)
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

CREATE INDEX idx_questions_exam       ON questions(exam_id);
CREATE INDEX idx_slots_enrollment     ON exam_slots(enrollment_id);
CREATE INDEX idx_slots_student        ON exam_slots(student_id);
CREATE INDEX idx_slots_status         ON exam_slots(status);
CREATE INDEX idx_answers_slot         ON answers(slot_id);
