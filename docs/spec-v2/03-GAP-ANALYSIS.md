# EDUCMIND — ĐỐI CHIẾU SPEC V2 ↔ CODE HIỆN TẠI

> Mục đích: xác định **chính xác** repo phải đổi gì để khớp "Báo cáo" (spec v2), chia thành các phase thi công được, không làm sập build giữa đường.
> Số liệu code lấy tại commit `cb12a22`: 48 entity, 31 repository, 45 service, 27 controller, 104 endpoint, 33 bảng.

## 1. Kết luận ngắn

Spec v2 **không phải** một bản mở rộng của repo hiện tại — nó là **thiết kế lại** ở tầng dữ liệu và phân quyền:

| Khía cạnh | Repo hiện tại | Spec v2 | Mức độ |
|---|---|---|---|
| Vai trò | 4 (SUPER_ADMIN, SCHOOL_ADMIN, TEACHER, STUDENT) | **2** (TEACHER, STUDENT) | 🔴 đổi toàn bộ phân quyền |
| Đơn vị gốc | `schools` → `classes` → `courses` (N-N qua `course_class`) | `classes` (owner_teacher_id) → `courses` (**1-N**) | 🔴 đổi khoá ngoại gốc |
| Nội dung | course → lesson (objectives **JSONB**) | course → **topic** → lesson → **learning_objectives (bảng)** | 🔴 thêm 1 tầng + tách bảng |
| Mục tiêu học tập | chuỗi `learning_objective_code` trong `questions`, `learning_evidence`... | bảng `learning_objectives` + 2 bảng nối `assignment_objectives`, `question_objectives` | 🔴 đổi khoá tham chiếu toàn hệ |
| Module ngoài spec | `module-rag`, `module-reports`, `module-tutor`, `module-personalization` (rỗng), `subjects` | **không có** | 🔴 xoá |
| Ràng buộc nghiệp vụ | đặt ở tầng service | đặt ở **trigger DB** (~25 trigger) | 🟠 chuyển nơi thực thi |
| Thông báo | service + event trong Java | **trigger DB** sinh thông báo | 🟠 đổi kiến trúc |

→ Cần **~150/230 file** bị sửa hoặc xoá. Không thể làm "một lần" mà vẫn giữ repo chạy được; phải chia phase.

## 2. Bảng đối chiếu 25 bảng

| # | Bảng spec v2 | Trạng thái trong repo | Hành động |
|---|---|---|---|
| 1 | `users` | có, nhưng 4 role + `school_id`, `status`, `must_change_password`, `created_by/updated_by` | đổi enum role → 2; bỏ 5 cột; `full_name` 150; unique `lower(email)` |
| 2 | `classes` | có (`school_id`, `academic_year`) | bỏ `school_id`, `academic_year`→`school_year`, thêm `grade` |
| 3 | `class_members` | có (`user_id`, `member_role`, UNIQUE class+user) | đổi `user_id`→`student_id`, bỏ `member_role`; chỉ STUDENT |
| 4 | `courses` | có (`school_id`, `created_by`, `status`) | đổi sang `class_id` (1-N); bỏ `status` |
| 5 | `course_topics` | **không có** | **bảng mới** + entity + CRUD (US-T02A) |
| 6 | `lessons` | có (`learning_objectives` JSONB, `publish_status`, `order_index`) | thêm `topic_id` NOT NULL; bỏ JSONB, `order_index`; `status` |
| 7 | `learning_objectives` | **không có bảng** (JSONB trong lesson) | **bảng mới** `(lesson_id, code, description)` UNIQUE(lesson,code) |
| 8 | `resources` | có (`storage_key`, `size_bytes`, `mime_type`, `processing_status`) | đổi tên cột theo spec; bỏ `processing_status` |
| 9 | `assignments` | có `course_id`, `max_score`, `due_date`, `allow_late` | thêm `topic_id`, `lesson_id`, `late_close_date`; bỏ `class_id` nếu có |
| 10 | `assignment_submissions` | có | thêm `is_edited`, `edited_at`, `deleted_at`; UNIQUE(assignment, student) 1 dòng/học sinh |
| 11 | `assignment_objectives` | **không có** (đang dùng `learning_objective_code` dạng chuỗi) | **bảng mới** |
| 12 | `questions` | có (`learning_objective_code` chuỗi) | bỏ cột chuỗi; thêm JSONB `options`, `correct_answer`, `bloom_level` |
| 13 | `question_objectives` | **không có** | **bảng mới** (n-N) |
| 14 | `assessments` | có | thêm `topic_id`, `lesson_id`, `max_attempts`, `open_at/close_at`, `duration_minutes` |
| 15 | `assessment_questions` | có (`weight`) | giữ |
| 16 | `assessment_attempts` | có | thêm `attempt_number`, `total_score`; bỏ các cột thừa |
| 17 | `submission_answers` | có | `answer_content` JSONB; bỏ `is_correct` nếu có |
| 18 | `learning_evidence` | có (`lesson_id`, `learning_objective_id`, `occurred_at`) | đổi sang `objective_id`, `updated_at`; bỏ `lesson_id` |
| 19 | `mastery` | có (`lesson_id`, `confidence`, ...) | đổi sang `(student_id, objective_id)`, bỏ `confidence`, `lesson_id` |
| 20 | `knowledge_gaps` | có | đổi `objective_id`; giữ dismiss |
| 21 | `recommendations` | có | đổi cột; thêm `related_assessment_id`, `target_entity_*` |
| 22 | `ai_generation_requests` | có, 3 loại (lesson-plan / question-set / matrix) | thu hẹp: chỉ sinh câu hỏi theo `lesson_id`, `quantity`, `params` |
| 23 | `ai_generation_outputs` | có | thêm `objective_id`, `options`, `correct_answer`, `approved_question_id` |
| 24 | `password_reset_tokens` | có | giữ; thêm unique `token_hash`, unique 1 token active/user |
| 25 | `notifications` | có (+ `sequence_number`, `notification_preferences`) | enum 11 loại, `is_read/read_at`, `related_entity_*`; bỏ preferences + sequence |

### Bảng trong repo nhưng **không có** trong spec → xoá

`schools`, `invite_codes`, `refresh_tokens`*, `subjects`, `course_class`, `course_members`, `mastery_history`, `ai_observability_logs`, `document_chunks`, `resource_processing_jobs`, `report_export_requests`, `notification_preferences`.

\* `refresh_tokens`: spec không có bảng này nhưng AC-G01.3 yêu cầu "phiên hết hạn" → cần quyết định (xem §6).

## 3. Tác động theo module

| Module | Trạng thái | Việc phải làm | Ước lượng |
|---|---|---|---|
| `educmind-common` | 26 file | bỏ `RetrievalPort`, `ResourceProcessingPort`, `AssignmentDeadlinePort` (nếu không dùng); port mới cho mục tiêu học tập; sửa `CourseAccessPort` (class-scoped) | 0.5 ngày |
| `module-auth` | 38 file, 2.497 LOC | role 2 giá trị; bỏ `school_id`, `must_change_password`, invite code; thêm "tạo tài khoản + link đặt mật khẩu" (US-T00.2); reset password unique active token | 1.5 ngày |
| `module-schoolclass` | 15 file | bỏ `School`; `ClassMember` chỉ chứa student; lớp do TEACHER sở hữu; thêm `grade`, `school_year` | 1 ngày |
| `module-course` | 41 file | thêm `CourseTopic` (entity+repo+service+controller+DTO+test); tách `LearningObjective` thành entity; `Course.classId`; bỏ `Subject`, `CourseMember`, `CourseClass`; `Resource` đổi cột, bỏ RAG status | 2.5 ngày |
| `module-assignment` | 13 file | thêm scope topic/lesson, `late_close_date`, bảng nối objectives; submission thêm `is_edited/deleted_at`; chấm lại không tạo bản ghi mới | 1.5 ngày |
| `module-assessment` | 36 file | `questions` JSONB options/answer + bảng nối objectives; assessment thêm `max_attempts/open_at/close_at`; attempt `attempt_number/total_score`; auto-grade trắc nghiệm | 2.5 ngày |
| `module-analytics` | 39 file | evidence/mastery/gap/recommendation đổi khoá sang `objective_id`; bỏ `MasteryHistory`; sinh gợi ý theo US-S06/US-H01; "Chưa có dữ liệu" | 2.5 ngày |
| `module-teacher-assistant` | 26 file | chỉ sinh câu hỏi nháp; output gắn `objective_id`; duyệt nhiều bản nháp 1 thao tác (US-T10.5) | 1.5 ngày |
| `module-notification` | 22 file | enum 11 loại; bỏ preferences/sequence; thống nhất: Java service **hoặc** trigger DB (không cả hai) | 1 ngày |
| `module-rag` | 40 file | **xoá** (không có trong spec) | 0.5 ngày |
| `module-reports` | 26 file | **xoá** | 0.5 ngày |
| `module-tutor`, `module-personalization` | rỗng | **xoá** khỏi pom | 0.1 ngày |
| `educmind-app` | 3 file | cấu hình lại ComponentScan/EntityScan; security 2 role; seed; profile dev/prod | 1 ngày |
| Migration | 11 file | **thay toàn bộ** bằng baseline v2 (`V1..V6` theo nhóm A–J) | 1 ngày |
| Test | 28 file | viết lại theo entity mới + test trigger (Testcontainers) | 2 ngày |
| Docs + README | 12 file | cập nhật theo spec v2 | 1 ngày |

**Tổng: ~20–22 ngày công** cho một lần realign hoàn chỉnh (khớp với kết luận trong báo cáo rà soát trước: repo cần "chạy được + an toàn" trước khi mở rộng).

## 4. Những lỗi P0 của repo **được spec v2 giải quyết luôn** (nếu làm theo spec)

| Lỗi đã phát hiện | Spec v2 xử lý thế nào |
|---|---|
| SCHOOL_ADMIN tạo được SUPER_ADMIN (leo thang đặc quyền) | Không còn 2 role đó; quyền tạo tài khoản chỉ còn "giáo viên thêm học sinh vào lớp mình" (US-T00), có trigger `fn_check_class_member_role` |
| `users.school_id` không được set → báo cáo theo trường 403 | Bỏ hẳn khái niệm trường; scope theo `classes.owner_teacher_id` |
| DISABLED vẫn đăng nhập | Spec không có `status`; dùng `deleted_at` (xóa mềm) — cần chặn ở login theo `deleted_at` |
| Không có seed/bootstrap | Vẫn cần làm (spec không nói) → phase 0 |
| Không có test/CI | Vẫn cần làm → phase 5 |

## 5. Thứ tự phase đề xuất (mỗi phase repo vẫn build và chạy được)

### Phase 1 — Nền dữ liệu mới (bắt buộc làm trước)
1. Thay `db/migration` bằng baseline v2 (V1 auth+class, V2 course+topic+objective+resource, V3 assignment+assessment, V4 analytics, V5 AI, V6 notification) — lấy từ `database/schema_v2.sql`.
2. Viết lại entity/enum của `module-auth`, `module-schoolclass`, `module-course` theo bảng mới.
3. Xoá: `School`, `InviteCode`, `CourseClass`, `CourseMember`, `Subject`, `LearningObjective` JSONB → bảng thật.
4. Thêm `CourseTopic` + `LearningObjective` (CRUD + DTO + test).
5. Sửa security: 2 role, `@PreAuthorize` cho TEACHER/STUDENT, ownership theo `owner_teacher_id`/`class_members`.
6. Seed dev: 1 giáo viên, 1 lớp, 5 học sinh, 1 khóa học có 2 chủ đề.

### Phase 2 — Bài tập & Bài kiểm tra
- `assignment`: scope, late window, objectives, submission lifecycle (`is_edited/deleted_at`), chấm lại.
- `assessment`: JSONB options/answer, max_attempts, open/close, attempt_number/total_score, auto-grade.

### Phase 3 — Phân tích & AI
- `analytics`: evidence theo `objective_id`, mastery không history, gap/recommendation, US-H01.
- `teacher-assistant`: chỉ sinh câu hỏi, duyệt hàng loạt.
- `notification`: enum mới, bỏ preferences.

### Phase 4 — Dọn & đồng bộ
- Xoá `module-rag`, `module-reports`, `module-tutor`, `module-personalization`; cập nhật pom/ComponentScan.
- Sửa `docs/*` cũ hoặc chuyển hẳn sang `docs/spec-v2/`.
- README mới (chạy được thật).

### Phase 5 — Chất lượng
- CI GitHub Actions; Testcontainers smoke test chạy baseline v2; test trigger quan trọng (`fn_restrict_submission_edit`, `fn_check_evidence_source`, `fn_check_max_attempts`).

## 6. Cần bạn quyết (chặn phase 1)

1. **Xoá hay giữ các module ngoài spec**: `module-rag` (RAG/pgvector), `module-reports` (báo cáo/CSV/PDF), `module-tutor`, `module-personalization`. Spec v2 không có 4 module này. Nếu giữ, phải giữ luôn `document_chunks`, `report_export_requests`… → schema không còn khớp 25 bảng.
2. **Phiên đăng nhập**: spec chỉ có `password_reset_tokens`, không có `refresh_tokens`. Chọn:
   - (a) Bỏ refresh token — chỉ access token JWT 15–60 phút (đúng 25 bảng, đơn giản, nhưng UX phải đăng nhập lại);
   - (b) Giữ `refresh_tokens` như bảng bổ sung ngoài spec (an toàn hơn, lệch 1 bảng).
3. **Thông báo**: spec sinh thông báo bằng **trigger DB** (9 trigger). Repo hiện sinh bằng Java event + service. Chọn một nơi để tránh gửi trùng.
4. **Dữ liệu hiện có**: có cần giữ/convert dữ liệu DB dev không, hay được phép **xoá và tạo lại DB** (khuyến nghị: tạo lại, vì khoá chính thay đổi).
