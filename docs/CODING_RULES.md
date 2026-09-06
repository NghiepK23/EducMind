# EDUCMIND — CODING RULES (Quy ước code dùng chung)

> Mọi squad **bắt buộc** đọc trước khi code. Vi phạm các quy tắc dưới đây là lý do hợp lệ để reject Pull Request.

## 1. Nguyên tắc ranh giới module

- Mỗi `module-*` chỉ được import trực tiếp (Java) vào: (a) `educmind-common`, (b) entity/repository của **chính module đó**.
- Cấm import Service/Repository của module khác trực tiếp — trừ Repository interface đã được module nguồn **export tường minh** dạng `readonly` (đặt trong package `<module>.api` hoặc tương đương).
- Giao tiếp giữa 2 module nghiệp vụ: **chỉ qua `ApplicationEventPublisher`**. Định nghĩa Event class trong `common.events`, không định nghĩa Event riêng trong từng module.
- Duy nhất 1 ngoại lệ REST nội bộ được phép: gọi `POST /api/v1/internal/retrieval/search` (AI Worker).

## 2. Response Envelope (bắt buộc 100% endpoint)

```java
// common.envelope.ApiResponse<T>
{
  "success": boolean,
  "data": T | null,
  "error": { "code": string, "message": string } | null,
  "meta": { "pagination"?: {...} }
}
```
Không trả response tự do (raw object/list) ở bất kỳ Controller nào — mọi Controller trả `ApiResponse<T>`. Lỗi xử lý tập trung qua `@ControllerAdvice` trong `common.envelope`, không try-catch thủ công để build error response trong từng Controller.

## 3. Pagination — dùng đúng util, không tự viết lại

| Chuẩn | Util bắt buộc dùng | Khi nào dùng |
|---|---|---|
| Offset (page/size) | `common.pagination.OffsetPageUtil` | Danh sách "duyệt" ổn định (mặc định) |
| Cursor before/after | `common.pagination.CursorMessageUtil` | CHỈ tin nhắn Tutor |
| Cursor sequence_number | `common.pagination.CursorSequenceUtil` | CHỈ polling notification |

Không viết `LIMIT/OFFSET` tay trong Repository trừ khi gọi qua 1 trong 3 util trên.

## 4. Authorization — Ownership Model

```java
// common.authorization.CourseAuthorizationService
boolean isOwnerOrCoTeacher(UUID userId, UUID courseId);   // dùng cho Course và mọi nghiệp vụ theo course
boolean isClassOwner(UUID userId, UUID classId);           // dùng RIÊNG cho Class — KHÔNG áp dụng "ngang quyền"
```

**Quy tắc bắt buộc:**
- Mọi endpoint sửa/xóa dữ liệu gắn với `course_id` → check `isOwnerOrCoTeacher()`, TRỪ `DELETE /courses/{id}` (chỉ owner) và đổi `created_by` (chỉ owner).
- Mọi endpoint sửa/xóa dữ liệu gắn với `class_id` (không qua course) → check `isClassOwner()` (đồng giảng dạy cấp Class chỉ xem, không sửa) — **không dùng nhầm hàm `isOwnerOrCoTeacher`** ở đây.
- `SCHOOL_ADMIN` không bao giờ pass qua 2 hàm trên để "sửa" — chỉ dùng `@RoleGuard(SCHOOL_ADMIN)` cho các endpoint đọc tổng hợp hoặc `POST /users`.

## 5. AI Gateway & Rate Limit

```java
// common.aigateway
interface AIProvider {
    AIResponse generate(String prompt, ...);
    AIResponse generateStructured(String prompt, Class<T> schema, ...);
}

class AiQuotaService {
    void checkAndIncrement(UUID actorId, QuotaType type);  // ném RateLimitExceededException nếu vượt
}
```
- Mọi lời gọi AI Gateway **PHẢI** đi qua `AIProvider` của `common.aigateway` — không gọi thẳng SDK Gemini/OpenAI trong module nghiệp vụ.
- Mọi lời gọi **PHẢI** ghi `ai_observability_logs` (thành công lẫn thất bại) — implement sẵn trong `AIProvider`, module nghiệp vụ không tự ghi log này.
- Gọi `AiQuotaService.checkAndIncrement()` **trước khi** gọi AI Gateway (tránh tốn chi phí khi đã biết chắc sẽ bị từ chối).

## 6. Human-in-the-loop Review State Machine

```java
// common.review.ReviewableEntity (generic)
enum ReviewStatus { PENDING_REVIEW, APPROVED, REJECTED, EDITED_AND_APPROVED }
```
Dùng chung cho `ai_generation_outputs` (M7) và `recommendation`/`insight_reports` (M9) — không viết state machine riêng cho từng bảng.

## 7. Migration (Flyway)

- Đặt tên file: `V{n}__{module}_{mô_tả}.sql`, đúng thứ tự đã định ở `DATABASE.md`.
- Chỉ được `ALTER TABLE ADD COLUMN` (nullable) lên bảng của module khác đã "đóng băng" — không `DROP`/`RENAME`/đổi kiểu cột đã có, trừ khi có quyết định `[DECIDED]` tường minh kèm nguồn patch.
- Mỗi PR migration phải nêu rõ: bảng nào, cột nào, tham chiếu tới mục nào trong `DATABASE.md`.

## 8. Testing

```
tests/unit/<module>/         # business logic thuần túy, mock repository
tests/integration/<module>/  # có DB thật (Testcontainers), test theo Test Requirements của từng module FINAL
tests/e2e/                   # luồng xuyên module (vd: nộp bài → evidence → mastery → gap → recommendation)
```
- Mọi endpoint ghi (POST/PATCH/DELETE) **bắt buộc** có ít nhất 1 test authorization (owner/đồng giảng dạy/không liên quan).
- Mọi rule tính toán số (mastery, decay, pagination kẹp size) **bắt buộc** có unit test riêng, không chỉ test qua integration.

## 9. Naming convention

- Package: `com.educmind.<module>.<layer>` (vd `com.educmind.tutor.controller`, `com.educmind.tutor.service`).
- Event class: `<Entity><PastTenseVerb>Event` (vd `AssignmentGradedEvent`, `KnowledgeGapOpenedEvent`) — đặt trong `common.events`.
- Error code: SCREAMING_SNAKE_CASE, đối chiếu bảng mã lỗi dùng chung ở `API.md` trước khi tạo mã mới.

## 10. Xử lý điểm `[TBD]` khi code

- 🔴 P0 (embedding model, ngưỡng mastery, ranh giới Level 2/3): dùng **giá trị cấu hình** (`application.yml`/bảng config), đặt tên biến rõ ràng kèm comment trỏ tới mục tương ứng trong `REQUIREMENTS.md` — không hard-code, không tự đoán giá trị cuối cùng.
- 🟠/🟡/⚪ P1-P3: dùng đúng **giá trị mặc định đã đề xuất** trong file `_FINAL.md` gốc của module (không tự nghĩ giá trị khác) — nếu file gốc không đề xuất giá trị, hỏi Tech Lead trước khi tự chọn.

## Tài liệu liên quan
[`ARCHITECTURE.md`](./ARCHITECTURE.md) · [`DATABASE.md`](./DATABASE.md) · [`API.md`](./API.md) · [`AI_RAG.md`](./AI_RAG.md)
