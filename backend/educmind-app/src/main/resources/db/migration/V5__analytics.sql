-- ============================================================================
-- V5 — NHÓM 4: BẰNG CHỨNG HỌC TẬP VÀ CÁ NHÂN HÓA  (Spec v2, mục F — bảng 18..21)
-- ============================================================================

CREATE TYPE evidence_source       AS ENUM ('ASSIGNMENT', 'ASSESSMENT');
CREATE TYPE gap_status            AS ENUM ('OPEN', 'RESOLVED', 'DISMISSED');
CREATE TYPE recommendation_type   AS ENUM ('REVIEW_CONTENT', 'PRACTICE_TARGETED', 'ADVANCED_ACTIVITY', 'REATTEMPT_ASSESSMENT');
CREATE TYPE recommendation_status AS ENUM ('ACTIVE', 'ACCEPTED', 'DISMISSED', 'EXPIRED');

-- Bảng 18: learning_evidence
CREATE TABLE learning_evidence (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id    UUID NOT NULL REFERENCES users(id),
    objective_id  UUID NOT NULL REFERENCES learning_objectives(id),
    source_type   evidence_source NOT NULL,
    -- source_id: ASSIGNMENT -> id bài nộp; ASSESSMENT -> id bài kiểm tra.
    source_id     UUID NOT NULL,
    score         NUMERIC(4,3) NOT NULL CHECK (score >= 0 AND score <= 1),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_evidence_source UNIQUE (student_id, objective_id, source_type, source_id)
);

CREATE INDEX idx_evidence_student_objective ON learning_evidence(student_id, objective_id);
CREATE INDEX idx_evidence_source ON learning_evidence(source_type, source_id);
CREATE INDEX idx_evidence_objective ON learning_evidence(objective_id);

-- Bảng 19: mastery
CREATE TABLE mastery (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id      UUID NOT NULL REFERENCES users(id),
    objective_id    UUID NOT NULL REFERENCES learning_objectives(id),
    mastery_score   NUMERIC(4,3) NOT NULL CHECK (mastery_score >= 0 AND mastery_score <= 1),
    evidence_count  INT NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_student_objective_mastery UNIQUE (student_id, objective_id)
);

CREATE INDEX idx_mastery_objective ON mastery(objective_id);

-- Bảng 20: knowledge_gaps
CREATE TABLE knowledge_gaps (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id     UUID NOT NULL REFERENCES users(id),
    objective_id   UUID NOT NULL REFERENCES learning_objectives(id),
    status         gap_status NOT NULL DEFAULT 'OPEN',
    reason         TEXT,
    opened_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at    TIMESTAMPTZ,
    dismissed_by   UUID REFERENCES users(id),
    dismissed_at   TIMESTAMPTZ,
    dismiss_reason TEXT
);

CREATE INDEX idx_gaps_objective ON knowledge_gaps(objective_id);
CREATE INDEX idx_gaps_student_status ON knowledge_gaps(student_id, status);
CREATE UNIQUE INDEX uq_open_gap_per_objective
    ON knowledge_gaps (student_id, objective_id)
    WHERE status = 'OPEN';

-- Bảng 21: recommendations
CREATE TABLE recommendations (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id             UUID NOT NULL REFERENCES users(id),
    objective_id           UUID REFERENCES learning_objectives(id),
    related_assessment_id  UUID REFERENCES assessments(id),
    type                   recommendation_type NOT NULL,
    reason                 TEXT,
    target_entity_type     VARCHAR(50),
    target_entity_id       UUID,
    status                 recommendation_status NOT NULL DEFAULT 'ACTIVE',
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    responded_at           TIMESTAMPTZ
);

CREATE INDEX idx_recommend_objective ON recommendations(objective_id);
CREATE INDEX idx_recommend_student_status ON recommendations(student_id, status);
CREATE UNIQUE INDEX uq_active_recommendation
    ON recommendations (
        student_id,
        type,
        COALESCE(objective_id, '00000000-0000-0000-0000-000000000000'::uuid),
        COALESCE(related_assessment_id, '00000000-0000-0000-0000-000000000000'::uuid)
    )
    WHERE status = 'ACTIVE';
