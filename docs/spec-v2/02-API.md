# EDUCMIND — API ĐÃ TRIỂN KHAI (Phase 1)

> Chỉ liệt kê endpoint **đang có trong code**. Endpoint của Phase 2/3 (bài tập, bài kiểm tra,
> analytics, AI) sẽ bổ sung sau. Mọi response thành công dùng envelope
> `{ success, data, error, meta }`; riêng tải tệp trả nhị phân.

## 0. Quy ước chung

- Base path: `/api/v1`. Auth: `Authorization: Bearer <accessToken>`.
- **2 vai trò:** `TEACHER`, `STUDENT`. Sai quyền → `403` với `error.code = FORBIDDEN` (hoặc mã cụ thể hơn).
- Danh sách dùng phân trang offset: `?page=1&size=20` (mặc định 20, tối đa 100), `meta.pagination = {page,size,total_items,total_pages}`.
- Validate sai → `422 VALIDATION_ERROR`.

## 1. Xác thực & người dùng

| Method | Path | Quyền | Ghi chú |
|---|---|---|---|
| POST | `/api/v1/auth/login` | công khai | body `{email, password}` → `{accessToken, tokenType, expiresInSeconds, user}` |
| POST | `/api/v1/auth/logout` | đã đăng nhập | client bỏ token; API ghi log |
| POST | `/api/v1/auth/change-password` | đã đăng nhập | `{currentPassword, newPassword}` (US-G04) |
| POST | `/api/v1/auth/password-reset/request` | công khai | `{email}`; luôn trả cùng thông báo (AC-G02.1) |
| POST | `/api/v1/auth/password-reset/confirm` | công khai | `{token, newPassword}`; token dùng 1 lần (AC-G02.2) |
| GET | `/api/v1/users/me` | đã đăng nhập | `{id, fullName, email, role}` |
| GET | `/api/health` | công khai | health check |

## 2. Lớp học (US-T00) — chỉ TEACHER

| Method | Path | Ghi chú |
|---|---|---|
| POST | `/api/v1/classes` | `{name, grade?, schoolYear?}`; người tạo = owner (AC-T00.1) |
| GET | `/api/v1/classes` | lớp mình phụ trách, kèm `studentCount` |
| GET | `/api/v1/classes/{id}` | 403 nếu không phải owner |
| PATCH | `/api/v1/classes/{id}` | sửa name/grade/schoolYear |
| DELETE | `/api/v1/classes/{id}` | xoá mềm |
| GET | `/api/v1/classes/{id}/students` | danh sách học sinh + `joinedAt` |
| POST | `/api/v1/classes/{id}/students` | `{email, fullName?}` → có tài khoản thì thêm, chưa có thì tạo + gửi link đặt mật khẩu (AC-T00.2) |
| DELETE | `/api/v1/classes/{id}/students/{studentId}` | rời lớp, **giữ** kết quả học tập (AC-T00.4) |

## 3. Khóa học / Chủ đề / Bài học / Mục tiêu (US-T01, T02A, T02, T11, T12)

| Method | Path | Quyền | Ghi chú |
|---|---|---|---|
| POST | `/api/v1/classes/{classId}/courses` | TEACHER | AC-T01.1 |
| GET | `/api/v1/classes/{classId}/courses` | TEACHER (owner) / STUDENT (thành viên) | AC-T01.2 |
| GET | `/api/v1/courses` | cả hai | khóa học của chính mình |
| GET | `/api/v1/courses/{id}` | cả hai | |
| PATCH | `/api/v1/courses/{id}` | TEACHER | |
| DELETE | `/api/v1/courses/{id}` | TEACHER | xoá mềm |
| GET | `/api/v1/courses/{courseId}/topics` | cả hai | STUDENT chỉ thấy chủ đề `PUBLISHED` (AC-T02A.2) |
| POST | `/api/v1/courses/{courseId}/topics` | TEACHER | chủ đề mới = `DRAFT` |
| PATCH | `/api/v1/topics/{id}` | TEACHER | sửa tiêu đề/mô tả/thứ tự (AC-T02A.3-4) |
| DELETE | `/api/v1/topics/{id}` | TEACHER | xoá mềm chủ đề + các bài học bên trong (AC-T12.5) |
| POST | `/api/v1/topics/{id}/publish` \| `/unpublish` | TEACHER | AC-T02A.4 |
| GET | `/api/v1/topics/{topicId}/lessons` | cả hai | STUDENT: chủ đề phải `PUBLISHED`, chỉ thấy bài `PUBLISHED` (AC-S01.1/4) |
| GET | `/api/v1/courses/{courseId}/lessons` | cả hai | |
| POST | `/api/v1/topics/{topicId}/lessons` | TEACHER | bài học thuộc đúng chủ đề của khóa học (AC-T02.6) |
| GET | `/api/v1/lessons/{id}` | cả hai | |
| PATCH | `/api/v1/lessons/{id}` | TEACHER | đổi chủ đề phải cùng khóa học |
| DELETE | `/api/v1/lessons/{id}` | TEACHER | xoá mềm, giữ kết quả (AC-T12.1) |
| POST | `/api/v1/lessons/{id}/publish` | TEACHER | **bắt buộc có ≥1 mục tiêu** (AC-T02.4) |
| POST | `/api/v1/lessons/{id}/unpublish` | TEACHER | AC-T02.5 |
| GET | `/api/v1/lessons/{lessonId}/objectives` | cả hai | |
| POST | `/api/v1/lessons/{lessonId}/objectives` | TEACHER | `{code, description}`; trùng mã trong bài → 409 (AC-T02.2) |
| PUT | `/api/v1/objectives/{id}` | TEACHER | chỉ sửa mô tả; **mã bất biến** |
| DELETE | `/api/v1/objectives/{id}` | TEACHER | |

## 4. Tài liệu (US-T02 AC-T02.3, US-S01 AC-S01.3)

| Method | Path | Quyền | Ghi chú |
|---|---|---|---|
| POST | `/api/v1/lessons/{lessonId}/resources` | TEACHER | multipart `file`; ≤ 50MB; pdf/doc/docx/ppt/pptx/xls/xlsx/txt/md/png/jpg/jpeg/gif/mp4/mp3/zip |
| POST | `/api/v1/courses/{courseId}/resources` | TEACHER | tài liệu chung của khóa học |
| GET | `/api/v1/lessons/{lessonId}/resources` | cả hai | STUDENT chỉ thấy tài liệu của bài đã xuất bản |
| GET | `/api/v1/courses/{courseId}/resources` | cả hai | |
| GET | `/api/v1/resources/{id}/download` | cả hai | trả tệp nhị phân + `Content-Disposition` |
| DELETE | `/api/v1/resources/{id}` | TEACHER | xoá mềm bản ghi + xoá tệp |

## 5. Thông báo (US-G03)

| Method | Path | Ghi chú |
|---|---|---|
| GET | `/api/v1/me/notifications?page=&size=` | chỉ thông báo của chính mình (AC-G03.3) |
| GET | `/api/v1/me/notifications/unread-count` | `{unreadCount}` |
| POST | `/api/v1/me/notifications/{id}/read` | cập nhật `is_read`, `read_at` (AC-G03.2) |
| POST | `/api/v1/me/notifications/read-all` | `{updated}` |

> Bản ghi thông báo **do trigger DB sinh** (V7 — J.4) khi: học sinh nộp bài/nộp lại, bài tập được chấm,
> bài học/bài tập/bài kiểm tra được xuất bản, AI xong/thất bại, có gợi ý mới, có bài kiểm tra cần chấm.

## 6. Mã lỗi đang dùng

| HTTP | `error.code` |
|---|---|
| 400 | `INVALID_FILTER`, `RESET_TOKEN_INVALID`, `CURRENT_PASSWORD_INVALID` |
| 401 | `INVALID_CREDENTIALS`, `TOKEN_EXPIRED` |
| 403 | `FORBIDDEN`, `NOT_CLASS_OWNER`, `NOT_COURSE_OWNER`, `CLASS_ACCESS_DENIED`, `COURSE_ACCESS_DENIED`, `TOPIC_NOT_PUBLISHED`, `LESSON_NOT_PUBLISHED` |
| 404 | `NOT_FOUND`, `CLASS_NOT_FOUND`, `USER_NOT_FOUND`, `STUDENT_NOT_IN_CLASS`, `NOTIFICATION_NOT_FOUND` |
| 405 | `METHOD_NOT_ALLOWED` |
| 409 | `EMAIL_ALREADY_EXISTS`, `EMAIL_BELONGS_TO_TEACHER`, `STUDENT_ALREADY_IN_CLASS`, `DUPLICATE_OBJECTIVE_CODE` |
| 413 | `FILE_TOO_LARGE` |
| 415 | `UNSUPPORTED_FILE_TYPE` |
| 422 | `VALIDATION_ERROR`, `WEAK_PASSWORD`, `EMPTY_FILE`, `LESSON_HAS_NO_OBJECTIVE`, `TOPIC_NOT_IN_SAME_COURSE` |
| 500 | `INTERNAL_ERROR`, `STORAGE_ERROR` |
