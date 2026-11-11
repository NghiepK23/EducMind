-- ============================================================================
-- V1 — NHÓM 1: NGƯỜI DÙNG VÀ LỚP HỌC  (Spec v2, mục C)
-- ============================================================================
-- Thứ tự bắt buộc theo FK: users -> classes -> class_members
-- Chỉ có 2 vai trò: TEACHER, STUDENT.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TYPE user_role AS ENUM ('TEACHER', 'STUDENT');

-- Bảng 1: users
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name       VARCHAR(150) NOT NULL,
    email           VARCHAR(150) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            user_role NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ
);

CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email)) WHERE deleted_at IS NULL;

-- Bảng 24: password_reset_tokens (đặt ở V1 vì cùng nhóm xác thực)
CREATE TABLE password_reset_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    token_hash  VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_reset_token_hash ON password_reset_tokens(token_hash);
CREATE UNIQUE INDEX uq_active_reset_token ON password_reset_tokens (user_id) WHERE used_at IS NULL;

-- Bảng 2: classes
CREATE TABLE classes (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(100) NOT NULL,
    grade               SMALLINT,
    school_year         VARCHAR(20),
    owner_teacher_id    UUID NOT NULL REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by          UUID REFERENCES users(id),
    deleted_at          TIMESTAMPTZ
);

CREATE INDEX idx_classes_owner ON classes(owner_teacher_id);

-- Bảng 3: class_members (chỉ chứa học sinh)
CREATE TABLE class_members (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id    UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    student_id  UUID NOT NULL REFERENCES users(id),
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_class_student UNIQUE (class_id, student_id)
);

CREATE INDEX idx_class_members_student ON class_members(student_id);
