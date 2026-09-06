# EDUCMIND — DATABASE (Schema tổng hợp)

> DDL đầy đủ (kiểu dữ liệu chính xác từng cột) nằm trong file `_FINAL.md` của từng module gốc — tài liệu này là **bản đồ tổng hợp + thứ tự migration + quan hệ giữa các bảng**, dùng để tra cứu nhanh khi code, không thay thế DDL gốc.

## 0. Quy ước chung

- PK: `id UUID DEFAULT gen_random_uuid()` cho mọi bảng, trừ `notifications.sequence_number` (BIGSERIAL, phụ, không phải PK).
- Soft-delete: cột `deleted_at TIMESTAMPTZ NULL` ở các bảng thực thể chính (users, courses, lessons, resources, assignments, questions, assessments...).
- Timestamp: `created_at`/`updated_at` mặc định `now()`.
- Polymorphic reference (không FK cứng, validate ở application layer): `learning_evidence.source_id`, `notifications.source_entity_id`, `ai_observability_logs.request_context_id`.

---

## V1 — module-auth + module-schoolclass

| Bảng | Cột đáng chú ý | Ghi chú |
|---|---|---|
| `users` | `email UNIQUE`, `role`, `school_id NULL`, `must_change_password` | `role`: SUPER_ADMIN/SCHOOL_ADMIN/TEACHER/STUDENT |
| `invite_codes` | `class_id`, `school_id`, `max_uses`, `used_count`, `created_by` (phải TEACHER) | Chỉ TEACHER tạo được |
| `refresh_tokens` | `token_hash` (không lưu plaintext), `expires_at`, `revoked_at` | |
| `password_reset_tokens` | `token_hash`, `expires_at` = **15 phút** `[DECIDED]`, `used_at` | 1 lần dùng |
| `schools` | `name`, `status` | |
| `classes` | `school_id`, `owner_teacher_id`, `academic_year` | 1 owner + nhiều đồng giảng dạy qua `class_members` |
| `class_members` | `class_id`, `user_id`, `member_role` (TEACHER/STUDENT), `UNIQUE(class_id,user_id)` | |

**Còn mở:** TTL access/refresh token (số phút cụ thể) — ⚪ P3.

---

## V2 — module-course

| Bảng | Cột đáng chú ý | Ghi chú |
|---|---|---|
| `courses` | `school_id`, `created_by`, `status` | |
| `course_class` | `course_id`, `class_id`, `linked_by`, `unlinked_at` | N-N, `UNIQUE(course_id,class_id)` |
| `lessons` | `course_id`, `learning_objectives JSONB` (vd `[{"code":"LO01","text":"..."}]`), `publish_status`, `order_index` | Nguồn học đối tượng cho toàn hệ thống |
| `resources` | `course_id`, `lesson_id NULL`, `size_bytes` (≤52428800), `storage_key`, `processing_status` | `processing_status` do Module 6 cập nhật ngược |

---

## V3 — module-assignment + module-assessment

| Bảng | Cột đáng chú ý | Ghi chú |
|---|---|---|
| `assignments` | `course_id`, `lesson_id NULL`, `due_date`, `allow_late_submission`, `publish_status` | |
| `assignment_class` | `assignment_id`, `class_id`, `UNIQUE` | |
| `assignment_submissions` | `assignment_id`, `student_id`, `resource_id NULL`, `status`, `score`, `UNIQUE(assignment_id,student_id)` | Chỉ nộp 1 lần, không sửa |
| `questions` | `course_id`, **`lesson_id NULL`** (PATCH — bắt buộc nếu có `learning_objective_code`), `question_type`, `cognitive_level` (Bloom's), `learning_objective_code`, `source` (MANUAL/AI_GENERATED), `review_status` | **Rule:** `learning_objective_code IS NOT NULL → lesson_id IS NOT NULL`. `AI_GENERATED` luôn insert thẳng `review_status='APPROVED'`, không đi qua PENDING |
| `assessments` | `course_id`, `duration_minutes`, `max_attempts` (default 1), `blueprint_config JSONB` | |
| `assessment_questions` | `assessment_id`, `question_id`, `score_weight`, `UNIQUE` | |
| `assessment_class` | `assessment_id`, `class_id`, `opens_at`, `closes_at`, `UNIQUE` | |
| `assessment_attempts` | `assessment_id`, `student_id`, `attempt_number`, `status`, `total_score`, `UNIQUE(assessment_id,student_id,attempt_number)` | |
| `submission_answers` | `attempt_id`, `question_id`, `is_correct NULL` (chờ chấm tay nếu ESSAY/SHORT_ANSWER), `score_awarded`, `UNIQUE(attempt_id,question_id)` | |

---

## V4 — ai-worker (RAG)

| Bảng | Cột đáng chú ý | Ghi chú |
|---|---|---|
| `document_chunks` | `resource_id`, `course_id`, `lesson_id NULL`, `content`, **`embedding VECTOR(?)`** 🔴 P0 chưa chốt chiều, `embedding_model` | Placeholder hiện dùng `VECTOR(1536)` — KHÔNG migrate thật cho tới khi chốt model |
| `resource_processing_jobs` | `resource_id`, `stage` (PARSE/STRUCTURAL_DETECTION/CHUNKING/METADATA_EXTRACTION/EMBEDDING), `status`, `attempt_count` | Audit + retry pipeline |

---

## V5 — module-analytics (Learning Evidence & Analytics)

| Bảng | Cột đáng chú ý | Ghi chú |
|---|---|---|
| `learning_evidence` | `student_id`, `class_id` (suy ra qua rule PATCH C.3), `course_id` (denormalized), `lesson_id NULL`, `learning_objective_code NULL`, `source_type` (ASSIGNMENT/ASSESSMENT/REASSESSMENT), `source_id` (polymorphic), `score`, `updated_at NULL` (chỉ set khi có ngoại lệ update trễ) | **Append-only** trừ 1 ngoại lệ (chấm assignment trễ) |
| `mastery` | `student_id`, `lesson_id`, `learning_objective_code`, `mastery_score`, `evidence_count`, `confidence_level`, `UNIQUE(student_id,lesson_id,learning_objective_code)` | Khóa chuẩn `(lesson_id, code)`, KHÔNG phải `(course_id, code)` |
| `mastery_history` | `mastery_id`, `mastery_score`, `snapshot_at`, `trigger_evidence_id` | |
| `knowledge_gap` | `student_id`, `lesson_id`, `learning_objective_code`, `status` (OPEN/RESOLVED/DISMISSED), `dismissed_by` | |
| `recommendation` | `student_id`, `lesson_id`, `learning_objective_code`, `knowledge_gap_id NULL`, `type` (6 loại, gồm `REATTEMPT_ASSESSMENT`), `generated_by` (RULE_ENGINE/AI_DRAFT), `review_status`, `status`. **KHÔNG có cột `priority`** (đã xóa hẳn) | Sẽ được Module 9 PATCH thêm 6 cột ở V8 |

**Còn mở 🔴 P0:** `mastery_threshold_low` (~0.50), `mastery_threshold_resolve` (~0.65), `half_life_days` (~30), `needs_support_threshold` (0.40), `practice_threshold` (0.70) — dùng cấu hình (`application.yml`), không hard-code.

---

## V6 — module-teacher-assistant

| Bảng | Cột đáng chú ý | Ghi chú |
|---|---|---|
| `ai_generation_requests` | `request_type` (LESSON_PLAN/QUESTION_SET/ASSESSMENT_MATRIX), `course_id`, `lesson_id NULL`, `requested_by`, `ai_provider`, `status` | |
| `ai_generation_outputs` | `request_id`, `content JSONB`, `retrieved_chunk_ids UUID[]`, `review_status`, `applied_at` | Khi `APPROVE`/`EDIT_AND_APPROVE` với `QUESTION_SET` → trigger insert vào `questions` (V3) |

---

## V7 — module-tutor

| Bảng | Cột đáng chú ý | Ghi chú |
|---|---|---|
| `tutor_conversations` | `student_id`, `course_id`, `lesson_id NULL`, `context_type`, `status`, `ended_reason` | Tối đa 3 `ACTIVE` đồng thời/học sinh |
| `tutor_messages` | `conversation_id`, `sender` (STUDENT/AI/SYSTEM), `scaffolding_level` (1-4), `mastery_snapshot`, `knowledge_gap_id NULL`, `flagged` | Index bắt buộc: `(conversation_id, created_at DESC, id)` cho cursor pagination |
| `tutor_message_feedback` | `message_id`, `student_id`, `rating`, `UNIQUE(message_id,student_id)` | |

---

## V8 — module-personalization

| Bảng | Cột đáng chú ý | Ghi chú |
|---|---|---|
| `recommendation` (PATCH thêm cột vào V5) | + `ai_draft_content JSONB`, `retrieved_chunk_ids UUID[]`, `reviewed_by`, `reviewed_at`, `review_notes`, `edited_content` | Gộp chung migration với việc xóa cột `priority` nếu lỡ tạo |
| `learning_path` | `student_id`, `course_id`, `status` (ACTIVE/COMPLETED/ABANDONED), `generation_reason`. Cần **partial unique index** `WHERE status='ACTIVE'` (không phải UNIQUE constraint thường) | 🟠 P1 kỹ thuật thuần túy, chốt ngay lúc viết migration |
| `learning_path_items` | `learning_path_id`, `recommendation_id`, `sequence_order`, `priority_reason` (VARCHAR — khác hẳn cột `priority` đã xóa), `item_status`, `UNIQUE(learning_path_id,recommendation_id)` | |
| `insight_reports` | `scope_type` (STUDENT/CLASS), `student_id NULL`, `class_id NULL`, `course_id`, `period_start/end`, `content JSONB`, `source_data_snapshot JSONB`, `review_status` | Validate application-layer: scope_type khớp student_id/class_id |

---

## V9 — module-notification + module-reports

| Bảng | Cột đáng chú ý | Ghi chú |
|---|---|---|
| `notifications` | `recipient_id`, `type`, `payload JSONB`, `source_event_type`, `source_entity_id` (polymorphic), `status`, **`sequence_number BIGSERIAL`** (cursor, KHÔNG dùng làm PK) | Index: `(recipient_id, status, created_at DESC)` + `(recipient_id, sequence_number DESC)` |
| `notification_preferences` | `user_id`, `notification_type`, `enabled`, `UNIQUE(user_id,notification_type)` | Không có dòng = mặc định enabled |
| `report_export_requests` | `report_type`, `requested_by`, `scope_params JSONB`, `format` (PDF/CSV), `status`, `file_storage_key` | Chỉ dùng cho export file (bất đồng bộ); xem trực tiếp trên dashboard = query on-demand, không cần bảng |

---

## V10 — Cross-cutting Observability

| Bảng | Cột đáng chú ý | Ghi chú |
|---|---|---|
| `ai_observability_logs` | `request_context_type` (TEACHER_ASSISTANT/TUTOR/PERSONALIZATION), `request_context_id` (polymorphic → `ai_generation_requests.id`/`tutor_messages.id`/`recommendation.id`/`insight_reports.id`), `latency_ms`, `estimated_cost`, `status` | Nguồn đếm duy nhất cho rate-limit M7/M8/M9, KHÔNG tạo bảng quota riêng |
| `audit_log` | `actor_id`, `action`, `entity_type`, `entity_id`, `metadata JSONB` | Danh sách `action` bắt buộc log — ⚪ P3 chưa chốt |

---

## Sơ đồ quan hệ rút gọn (foreign key xuyên module)

```
users ──┬── classes.owner_teacher_id
        ├── class_members.user_id
        ├── courses.created_by
        ├── lessons.created_by
        └── ... (mọi *_by / *_id user reference)

schools ── classes.school_id ── class_members ──┐
                                                  ├── course_class ── courses
lessons ── learning_objectives (JSONB) ──────────┘         │
    │                                                        │
    ├── questions.lesson_id (PATCH)                         │
    ├── assignments.lesson_id                               │
    └── resources.lesson_id                                 │
                                                              │
questions.lesson_id + learning_objective_code ───────► learning_evidence
assignment_submissions/assessment_attempts ──────────► learning_evidence (qua rule class_id — PATCH C.3)
learning_evidence ──► mastery ──► knowledge_gap ──► recommendation ──► learning_path_items
recommendation ──► ai_observability_logs (qua request_context_id, khi AI_DRAFT)
```

## Danh sách bảng KHÔNG tồn tại (tránh nhầm khi đọc code cũ/tài liệu cũ)

- ❌ `recommendation_generation_run` — lỗi ghi tên, thực tế dùng `recommendation` (lọc `generated_by='AI_DRAFT'`) + `insight_reports`.
- ❌ `ai_recommendation_explanations` — lỗi ghi tên tương tự, cùng lý do trên.
- ❌ Cột `recommendation.priority` — đã xóa hẳn khỏi schema, không migrate cột này.

## Tài liệu liên quan
[`ARCHITECTURE.md`](./ARCHITECTURE.md) · [`API.md`](./API.md) · [`AI_RAG.md`](./AI_RAG.md)
