-- V2__add_correct_option_to_questions.sql
-- Add correct_option column to questions table
-- Previously, correct answers were only in grading-service.
-- Now also stored in exam-service for admin display (still hidden from student API via @JsonIgnore).

ALTER TABLE questions ADD COLUMN IF NOT EXISTS correct_option VARCHAR(1) NOT NULL DEFAULT 'A';
