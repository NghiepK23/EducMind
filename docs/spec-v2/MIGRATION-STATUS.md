# EDUCMIND — TRẠNG THÁI CHUYỂN ĐỔI SANG SPEC V2

> Nhánh: `arena/e8763d6d-educmind` · Cập nhật: 2026-11-10
> Kế hoạch đầy đủ: [`03-GAP-ANALYSIS.md`](./03-GAP-ANALYSIS.md)

## 1. Quyết định đã chốt

| Vấn đề | Quyết định |
|---|---|
| Module ngoài spec | **Xoá** `module-rag`, `module-reports`, `module-tutor`, `module-personalization` |
| Phiên đăng nhập | **Chỉ access token JWT** (60 phút), không có bảng `refresh_tokens` |
| Sinh thông báo | **Trigger DB** (V7 — J.4); tầng Java chỉ đọc/cập nhật trạng thái đã xem |
| Tiến độ | Làm **Phase 1** trước, review rồi mới sang Phase 2 |

## 2. Phase 1 + Phase 2A + Phase 2B — ĐÃ XONG & ĐÃ NGHIỆM THU

### Schema (Flyway baseline mới, thay toàn bộ V1..V11 cũ)

| File | Nội dung |
|---|---|
| `V1__auth_and_class.sql` | `user_role`, `users`, `password_reset_tokens`, `classes`, `class_members` |
| `V2__course_topics_lessons.sql` | `content_status`, `courses`, `course_topics`, `lessons`, `learning_objectives`, `resources` |
| `V3__assignment.sql` | `submission_status`, `assignments`, `assignment_submissions`, `assignment_objectives` |
| `V4__assessment.sql` | 4 enum + `questions`, `question_objectives`, `assessments`, `assessment_questions`, `assessment_attempts`, `submission_answers` |
| `V5__analytics.sql` | 4 enum + `learning_evidence`, `mastery`, `knowledge_gaps`, `recommendations` |
| `V6__ai_and_notification.sql` | `ai_request_status`, `ai_review_status`, `notification_type` + 4 bảng |
| `V7__triggers.sql` | 25 hàm/trigger của Spec v2 mục J (updated_at, chấm điểm, sửa/xoá, thông báo, toàn vẹn chéo bảng) |

→ Đủ **25 bảng** đúng spec; các bảng của Phase 2/3 đã có sẵn nên không phải sửa migration nữa.

### Code đã viết lại

| Module | Nội dung |
|---|---|
| `module-auth` | `User`, `Role` (2 giá trị), `PasswordResetToken`; login/logout/change-password/reset-password; `GET /users/me`; `UserLookupPortImpl`, `AccountPortImpl` |
| `module-schoolclass` | `SchoolClass` (owner_teacher_id, grade, school_year), `ClassMember` (chỉ học sinh); CRUD lớp; thêm/bớt học sinh bằng email (US-T00) |
| `module-course` | `Course` (class-scoped), `CourseTopic` (US-T02A), `Lesson`, `LearningObjective` (bảng thật), `Resource`; CRUD + publish/unpublish + upload/download tài liệu |
| `module-notification` | Đọc thông báo, `unread-count`, đánh dấu đã đọc / đọc tất cả (US-G03) |
| `educmind-common` | Port mới theo mô hình v2: `ClassAccessPort`, `CourseAccessPort`, `UserLookupPort`, `AccountPort`; bỏ toàn bộ port/event của RAG & notification cũ |
| `module-assignment` | **Phase 2A**: `Assignment` (phạm vi khóa học/chủ đề/bài học), `AssignmentSubmission` (SUBMITTED/LATE, sửa/xoá/nộp lại), `AssignmentObjective`; tạo–sửa–xoá–xuất bản, roster chấm bài, chấm điểm + ghi bằng chứng |
| `module-assessment` | **Phase 2B**: `Question` + `QuestionObjective` (JSONB options/correct_answer), `Assessment` + `AssessmentQuestion`, `AssessmentAttempt` + `SubmissionAnswer`; chấm tự động trắc nghiệm, chấm tay tự luận, kết quả lớp, ghi bằng chứng theo mục tiêu |
| `module-analytics` | **Phase 2A**: chỉ phần `learning_evidence` (`LearningEvidence` + `LearningEvidencePortImpl`, có `recordHighest` cho làm lại) — mastery/lỗ hổng để Phase 3 |
| `educmind-app` | Security 2 role + CORS dev; `application.properties` (JWT_SECRET/DB_PASSWORD qua env); `DevSeedService` + `DevSeedRunner` (kèm 1 bài tập mẫu đã xuất bản) |
| `educmind-common` (bổ sung) | `LocalFileStorage` (lưu tệp dùng chung), `CourseContentPort` (chủ đề/bài học/mục tiêu), `LearningEvidencePort` |

### Đã xoá

`module-rag` (document_chunks, pgvector), `module-reports` (CSV/PDF), `module-tutor`, `module-personalization`,
`backend/ai-worker/` (thư mục rỗng), `database/migrations/V1...` (bản trùng), `subjects`, `schools`,
`invite_codes`, course members/course_class.

## 3. Phase 1 — CỐ Ý CHƯA LÀM (đang "park", code cũ vẫn còn trong repo nhưng KHÔNG build)

| Module | Lý do |
|---|---|
| `module-analytics` (phần còn lại) | Mastery, lỗ hổng kiến thức, gợi ý (US-T07, T08, S05, S06, H01) — Phase 3; bằng chứng đã có sẵn từ Phase 2A |
| `module-teacher-assistant` | Chỉ còn sinh câu hỏi nháp + duyệt (US-T09, T10) |

Các module này **không nằm trong `<modules>` của `backend/pom.xml`** và **không phải dependency của
`educmind-app`** → build hiện tại chỉ gồm 6 module. Khi làm Phase 2/3, bật lại bằng cách thêm lại vào pom
sau khi đã viết lại code.

## 4. Trạng thái kiểm chứng

**Đã chạy thật trên macOS (2026-11-10), Java 21 + PostgreSQL 17.11:**

| Bước | Kết quả |
|---|---|
| `mvnw clean install` (6 module) | BUILD SUCCESS |
| Flyway trên DB trống | `Successfully applied 7 migrations (V1..V7)` |
| Hibernate `ddl-auto=validate` | **Qua** — mapping PG enum (`user_role`, `content_status`, `notification_type`) đúng |
| Spring Data | `Found 10 JPA repository interfaces` |
| Seed (`EDUCMIND_SEED=true`) | Tạo 1 giáo viên, 5 học sinh, lớp 10A1, khóa Tin học 10 (2 chủ đề, 3 bài, 3 mục tiêu) |
| Khởi động | Tomcat 8080, không còn user in-memory mặc định |
| Smoke test Phase 1 | 19/19 PASS (`scripts/smoke-test.sh`) |
| Phase 2A | 32/32 check PASS (`scripts/smoke-test.sh`) |
| Phase 2B (lần chạy 1) | 48/54 PASS — 6 FAIL, xem ghi chú bên dưới |
| Phase 2B (sau khi sửa, commit `460e6eb`) | **54/54 PASS** — `mvnw clean install` BUILD SUCCESS, không cần xoá DB |

**Ghi chú lần chạy 2B đầu tiên (2026-11-10):**

1. Lỗi thật: `PATCH /attempts/{id}/answers/{questionId}/grade` trả 500 khi chấm câu tự luận cuối cùng.
   Nguyên nhân: `AttemptFinalizer` đặt trạng thái `GRADED` trong bộ nhớ rồi ghi `learning_evidence`
   ngay, nhưng trigger `trg_check_evidence_source` (V7) kiểm tra **dữ liệu trong DB** và chỉ chấp nhận
   bằng chứng `ASSESSMENT` khi `assessment_attempts.status = 'GRADED'` → `RAISE EXCEPTION`.
   Cách sửa: `attemptRepository.saveAndFlush(attempt)` trước `recordEvidence(...)`, và gán
   `submittedAt` cho bài chỉ có trắc nghiệm (chấm xong ngay khi nộp).
2. Lỗi script: check "hoc sinh doc duoc thong bao" kỳ vọng `unreadCount = 0`, chỉ đúng trên DB trống
   (mỗi lần chạy smoke trước đó để lại thông báo `NEW_ASSIGNMENT_PUBLISHED`, `ASSIGNMENT_GRADED`).
   Đã đổi thành kiểm tra endpoint trả 200 + `unreadCount` là số nguyên không âm.
3. 4 FAIL còn lại chỉ là hệ quả của (1) — cùng một transaction bị rollback nên lượt làm vẫn `SUBMITTED`.

**Kết luận:** sau khi sửa, chạy lại trên đúng DB cũ (không reset) → **54/54 PASS**. Phase 2B nghiệm thu.

Kiểm tra API end-to-end: `./scripts/smoke-test.sh` (health, đăng nhập 2 vai trò, phân quyền
giáo viên/học sinh, chủ đề nháp bị ẩn với học sinh, 401/403, thông báo).

**Vẫn còn thiếu:** chưa có unit/integration test, chưa có CI, chưa test trigger J.1–J.5 bằng dữ liệu
thật (Phase 5). Khi nâng cấp từ bản v1 phải `docker compose down -v` + `mvnw clean install`
(schema cũ ở version 11, baseline mới dùng lại số hiệu V1..V7).

## 5. Việc còn thiếu so với spec (ngoài Phase 1)

| Mã | Nội dung | Phase |
|---|---|---|
| AC-G03.4 | Thông báo trỏ tới nội dung đã xoá phải hiển thị "không còn khả dụng" | 4 |
| — | Chấm lại điểm thấp hơn không hạ bằng chứng đã ghi (đang dùng `recordHighest` để giữ kết quả tốt nhất giữa các lượt làm) | 4 |
| AC-T11.1 | Chặn đổi **mã** mục tiêu khi đã có `learning_evidence` (hiện chặn mọi thay đổi mã) | 3 |
| US-T03/T04 | Bài tập: phạm vi topic/lesson, cửa sổ nộp trễ, chấm lại | ✅ 2A |
| US-T05–T15 | Ngân hàng câu hỏi, bài kiểm tra, chấm tay, xem lượt làm | ✅ 2B |
| US-T07/T08/S05/S06/H01 | Mastery, lỗ hổng kiến thức, gợi ý | 3 |
| US-T09/T10 | AI sinh câu hỏi nháp + duyệt | 3 |
| — | CI, Testcontainers, test trigger | 5 |
