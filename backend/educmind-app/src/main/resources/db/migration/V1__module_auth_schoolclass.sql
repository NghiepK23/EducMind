-- ============================================================================
-- V1__module_auth_schoolclass.sql
-- ============================================================================
-- Nguồn sự thật: EducMind_Module01_Auth_SchoolClass_FINAL.md (Phần A + Phần B)
-- Đây là migration Flyway ĐẦU TIÊN của dự án — tạo toàn bộ bảng cho:
--   - Phần A: Authentication & User (users, invite_codes, refresh_tokens, password_reset_tokens)
--   - Phần B: School / Class (schools, classes, class_members)
--
-- [Giải thích cho người mới] Vì sao 2 "module" (auth + schoolclass) lại chung 1 file migration?
-- Vì theo tài liệu FINAL, đây thực ra là 1 tài liệu nghiệp vụ duy nhất (không có "Module 2"
-- riêng), chỉ được tách thành 2 module Maven (module-auth, module-schoolclass) vì lý do tổ chức
-- code. Bảng của 2 phần này phụ thuộc chéo lẫn nhau (users cần schools, classes cần users) nên
-- phải tạo cùng 1 migration, đúng thứ tự, để tránh lỗi "relation does not exist".
--
-- THỨ TỰ TẠO BẢNG (rất quan trọng vì có FOREIGN KEY):
--   1. schools          (không phụ thuộc bảng nào)
--   2. users            (phụ thuộc schools; tự tham chiếu chính nó qua created_by/updated_by)
--   3. classes          (phụ thuộc schools, users)
--   4. class_members    (phụ thuộc classes, users)
--   5. invite_codes     (phụ thuộc classes, schools, users)
--   6. refresh_tokens   (phụ thuộc users)
--   7. password_reset_tokens (phụ thuộc users)
--
-- Cần bật extension sinh UUID (pgvector image đã có sẵn pgcrypto/uuid-ossp tùy bản, bật cho chắc):
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================================
-- 1) schools  (Module 1, Phần B, mục B.4)
-- ============================================================================
CREATE TABLE schools (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(255) NOT NULL,
    address       VARCHAR(500) NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ NULL
);

-- ============================================================================
-- 2) users  (Module 1, Phần A, mục A.4)
-- ============================================================================
CREATE TABLE users (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email                  VARCHAR(255) NOT NULL UNIQUE,
    password_hash          VARCHAR(255) NOT NULL,
    full_name              VARCHAR(255) NOT NULL,
    role                   VARCHAR(20) NOT NULL,               -- SUPER_ADMIN | SCHOOL_ADMIN | TEACHER | STUDENT
    school_id              UUID NULL REFERENCES schools(id),   -- NULL cho SUPER_ADMIN
    status                 VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE | DISABLED
    must_change_password   BOOLEAN NOT NULL DEFAULT false,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by             UUID NULL REFERENCES users(id),     -- tự tham chiếu chính bảng này, hợp lệ trong PostgreSQL
    updated_by             UUID NULL REFERENCES users(id),
    deleted_at             TIMESTAMPTZ NULL
);

CREATE INDEX idx_users_school_id ON users(school_id);
CREATE INDEX idx_users_role ON users(role);

-- ============================================================================
-- 3) classes  (Module 1, Phần B, mục B.4)
-- ============================================================================
CREATE TABLE classes (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID NOT NULL REFERENCES schools(id),
    name                VARCHAR(255) NOT NULL,       -- vd: "Lớp 7A1"
    grade_level         VARCHAR(20) NULL,
    academic_year       VARCHAR(20) NULL,
    owner_teacher_id    UUID NOT NULL REFERENCES users(id),  -- giáo viên chủ nhiệm/sở hữu lớp
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          UUID NOT NULL REFERENCES users(id),
    deleted_at          TIMESTAMPTZ NULL
);

CREATE INDEX idx_classes_school_id ON classes(school_id);
CREATE INDEX idx_classes_owner_teacher_id ON classes(owner_teacher_id);

-- ============================================================================
-- 4) class_members  (Module 1, Phần B, mục B.4)
-- ============================================================================
CREATE TABLE class_members (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id      UUID NOT NULL REFERENCES classes(id),
    user_id       UUID NOT NULL REFERENCES users(id),
    member_role   VARCHAR(20) NOT NULL,   -- TEACHER | STUDENT (giáo viên hỗ trợ hoặc học sinh)
    joined_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    left_at       TIMESTAMPTZ NULL,
    UNIQUE(class_id, user_id)
);

CREATE INDEX idx_class_members_user_id ON class_members(user_id);
-- idx cho class_id không cần thêm riêng vì UNIQUE(class_id, user_id) ở trên đã tạo sẵn 1 index
-- có class_id ở vị trí đầu, Postgres tận dụng được cho truy vấn theo class_id.

-- ============================================================================
-- 5) invite_codes  (Module 1, Phần A, mục A.4)
-- ============================================================================
CREATE TABLE invite_codes (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code              VARCHAR(32) NOT NULL UNIQUE,
    class_id          UUID NOT NULL REFERENCES classes(id),
    school_id         UUID NOT NULL REFERENCES schools(id),   -- suy ra từ class_id, lưu thêm để truy vấn nhanh
    intended_role     VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    max_uses          INT NOT NULL DEFAULT 1,
    used_count        INT NOT NULL DEFAULT 0,
    expires_at        TIMESTAMPTZ NULL,
    created_by        UUID NOT NULL REFERENCES users(id),     -- bắt buộc phải là user có role = TEACHER (validate ở tầng ứng dụng)
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at        TIMESTAMPTZ NULL
);

CREATE INDEX idx_invite_codes_class_id ON invite_codes(class_id);

-- ============================================================================
-- 6) refresh_tokens  (Module 1, Phần A, mục A.4)
-- ============================================================================
CREATE TABLE refresh_tokens (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users(id),
    token_hash    VARCHAR(255) NOT NULL,   -- chỉ lưu hash, KHÔNG lưu token thô
    expires_at    TIMESTAMPTZ NOT NULL,
    revoked_at    TIMESTAMPTZ NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- ============================================================================
-- 7) password_reset_tokens  (Module 1, Phần A, mục A.4 + A.5.4)
-- ============================================================================
-- [DECIDED — PATCH2_TechnicalGaps.md, mục C.1] TTL = 15 phút, xử lý ở tầng application
-- (cột expires_at chỉ lưu giá trị NOW() + 15 phút do code Java tính khi tạo token,
--  DB không tự trừ thời gian).
CREATE TABLE password_reset_tokens (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users(id),
    token_hash    VARCHAR(255) NOT NULL,
    expires_at    TIMESTAMPTZ NOT NULL,  -- [DECIDED] 15 phút kể từ created_at
    used_at       TIMESTAMPTZ NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);
