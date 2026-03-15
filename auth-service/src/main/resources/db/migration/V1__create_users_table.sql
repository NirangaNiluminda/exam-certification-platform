-- V1__create_users_table.sql
-- auth-service database schema
-- Flyway managed migration

CREATE TABLE users (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'STUDENT',
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);

-- Seed default admin account (password: Admin@1234)
INSERT INTO users (id, email, password_hash, full_name, role)
VALUES (
    gen_random_uuid(),
    'admin@examplatform.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQyCMc4cXBaFqnmurxVOEAWYa',
    'Platform Admin',
    'ADMIN'
);
