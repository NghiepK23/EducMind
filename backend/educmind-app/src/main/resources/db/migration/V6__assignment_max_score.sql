ALTER TABLE assignments
    ADD COLUMN max_score NUMERIC(5,2) NOT NULL DEFAULT 10
    CONSTRAINT chk_assignments_max_score_positive CHECK (max_score > 0);
