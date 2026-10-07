# EDUCMIND — API SPECIFICATION (Tổng hợp)

> Chi tiết request/response mẫu đầy đủ từng endpoint: xem file `_FINAL.md` của module tương ứng. Tài liệu này là **danh mục tra cứu nhanh** + quy tắc chung áp dụng cho mọi endpoint.

## 0. Quy tắc chung cho MỌI endpoint

- **Envelope bắt buộc:** `{ "success": bool, "data": ..., "error": {"code","message"}|null, "meta": {...} }`.
- **Auth:** Bearer JWT, trừ các endpoint đánh dấu `No`.
- **Lỗi authorization dùng chung `NOT_COURSE_OWNER` / `FORBIDDEN_*`** theo từng module — không tự sáng tạo mã lỗi mới nếu đã có mã tương đương.

## 1. Ba chuẩn Pagination (bắt buộc dùng đúng, không trộn lẫn)

### CHUẨN 1 — Offset-based (mặc định cho mọi danh sách "duyệt")
```
?page={n}     -- 1-indexed, mặc định 1
&size={n}     -- mặc định 20, tối đa 100 (vượt → tự kẹp về 100, KHÔNG lỗi 422)
```
`meta.pagination`: `{ page, size, total_items, total_pages }`

Áp dụng: `knowledge-gaps`, `recommendations`, `generation-requests` (M7), danh sách **hội thoại** Tutor (M8), `learning-paths` (M9).

### CHUẨN 2 — Cursor before/after theo id (chỉ dùng cho tin nhắn trong 1 hội thoại Tutor)
```
?before_id={message_id}   -- tin nhắn TRƯỚC message này
&after_id={message_id}    -- tin nhắn SAU message này
&limit={n}                -- mặc định 50, tối đa 200
```
`meta.pagination`: `{ limit, has_more_before, has_more_after, oldest_id, newest_id }`

Áp dụng duy nhất: `GET /api/v1/students/me/tutor/conversations/{id}` (M8).

### CHUẨN 3 — Cursor theo sequence_number (chỉ dùng cho polling notification)
```
?since_seq={n}   -- trả bản ghi có sequence_number > since_seq
&limit={n}       -- mặc định 20, tối đa 100
```
`meta.pagination`: `{ limit, latest_seq, has_more }`

Áp dụng duy nhất: `GET /api/v1/me/notifications` (M10).

### KHÔNG áp dụng pagination
`mastery-matrix` (M5 — trả 1 ma trận), `processing-status` (M6 — trả 1 object), báo cáo Phần K (M10 — trả object tổng hợp), `unread-count` (M10).

---

## 2. Danh mục endpoint theo module

### Module 1 — Auth (`module-auth`)
| Method | Path | Auth |
|---|---|---|
| POST | `/api/v1/auth/register` | No |
| POST | `/api/v1/users` | SCHOOL_ADMIN |
| POST | `/api/v1/auth/login` | No |
| POST | `/api/v1/auth/refresh` | No |
| POST | `/api/v1/auth/logout` | Yes |
| POST | `/api/v1/auth/change-password-forced` | Yes (must_change_password) |
| POST | `/api/v1/auth/password-reset/request` | No |
| POST | `/api/v1/auth/password-reset/confirm` | No |
| GET | `/api/v1/users/me` | Yes |
| POST/GET/DELETE | `/api/v1/invite-codes` | TEACHER |

### Module 1 — School/Class (`module-schoolclass`)
| Method | Path | Auth |
|---|---|---|
| POST/GET | `/api/v1/schools` | SUPER_ADMIN |
| POST/GET/PATCH/DELETE | `/api/v1/classes` | TEACHER/SCHOOL_ADMIN |
| POST/DELETE | `/api/v1/classes/{id}/members` | TEACHER (owner)/SCHOOL_ADMIN |

### Module 3 — Course/Lesson/Resource (`module-course`)
| Method | Path | Auth |
|---|---|---|
| POST/GET/PATCH/DELETE | `/api/v1/courses` | TEACHER (owner hoặc đồng giảng dạy, trừ DELETE chỉ owner) |
| POST/DELETE/GET | `/api/v1/courses/{id}/classes` | TEACHER |
| POST/GET/PATCH/DELETE | `/api/v1/courses/{id}/lessons`, `/api/v1/lessons/{id}` | TEACHER |
| PATCH | `/api/v1/lessons/{id}/publish` | TEACHER |
| POST/GET/DELETE | `/api/v1/lessons/{id}/resources`, `/api/v1/courses/{id}/resources`, `/api/v1/resources/{id}` | TEACHER/Yes |

### Module 4 — Assignment (`module-assignment`)
| Method | Path | Auth |
|---|---|---|
| POST/GET/PATCH | `/api/v1/courses/{id}/assignments`, `/api/v1/assignments/{id}` | TEACHER (owner) |
| PATCH | `/api/v1/assignments/{id}/publish` | TEACHER |
| POST | `/api/v1/assignments/{id}/classes` | TEACHER |
| POST/GET | `/api/v1/assignments/{id}/submissions`, `.../submissions/me` | STUDENT/TEACHER |
| PATCH | `/api/v1/assignment-submissions/{id}/grade` | TEACHER |

### Module 4 — Assessment (`module-assessment`)
| Method | Path | Auth |
|---|---|---|
| POST/GET/PATCH/DELETE | `/api/v1/courses/{id}/questions`, `/api/v1/questions/{id}` | TEACHER |
| PATCH | `/api/v1/questions/{id}/review` | TEACHER (chỉ còn ý nghĩa cho MANUAL) |
| POST/PATCH | `/api/v1/courses/{id}/assessments`, `/api/v1/assessments/{id}/*` | TEACHER |
| POST/PATCH | `/api/v1/attempts/{id}/*` | STUDENT/TEACHER |

### Module 5 — Analytics (`module-analytics`)
| Method | Path | Pagination |
|---|---|---|
| GET | `/api/v1/students/me/mastery?lesson_id\|course_id={id}` | — |
| GET | `/api/v1/students/me/knowledge-gaps?course_id={id}` | CHUẨN 1 |
| GET | `/api/v1/students/me/recommendations?course_id={id}` | CHUẨN 1 |
| POST | `.../recommendations/{id}/accept\|dismiss` | — |
| GET | `/api/v1/lessons/{lesson_id}/analytics/mastery-matrix?class_id={id}` | Không áp dụng |
| GET | `/api/v1/courses/{course_id}/analytics/mastery-overview\|knowledge-gaps` | CHUẨN 1 (gaps) |
| PATCH | `/api/v1/knowledge-gaps/{id}/dismiss` | — |
| GET | `/api/v1/schools/{school_id}/analytics/overview` | SCHOOL_ADMIN, tổng hợp |

### Module 6 — RAG (`ai-worker`, expose qua module-course hoặc gateway riêng)
| Method | Path | Auth |
|---|---|---|
| GET | `/api/v1/resources/{id}/processing-status` | Owner/đồng giảng dạy/SCHOOL_ADMIN |
| POST | `/api/v1/resources/{id}/reprocess` | Owner/đồng giảng dạy |
| POST | `/api/v1/internal/retrieval/search` | Internal only |

### Module 7 — AI Teacher Assistant (`module-teacher-assistant`)
| Method | Path | Pagination |
|---|---|---|
| POST | `/api/v1/ai/lesson-plans/generate`, `.../questions/generate`, `.../assessment-matrix/generate` | — (202 async) |
| GET | `/api/v1/ai/generation-requests/{id}` | — |
| GET | `/api/v1/ai/generation-requests` | CHUẨN 1 |
| POST | `/api/v1/ai/generation-outputs/{id}/review` | — |
| GET | `/api/v1/schools/{school_id}/reports/ai-usage/teacher-assistant` | SCHOOL_ADMIN |

### Module 8 — AI Tutor (`module-tutor`)
| Method | Path | Pagination |
|---|---|---|
| POST | `/api/v1/students/me/tutor/conversations` | — |
| POST | `.../conversations/{id}/messages` | — |
| GET | `/api/v1/students/me/tutor/conversations` | CHUẨN 1 |
| GET | `/api/v1/students/me/tutor/conversations/{id}` | **CHUẨN 2** |
| POST | `.../conversations/{id}/end`, `.../messages/{id}/feedback` | — |
| GET | `/api/v1/teachers/students/{student_id}/tutor/conversations` | TEACHER oversight, mức chi tiết TBD |
| GET | `/api/v1/schools/{school_id}/tutor/usage-overview` | SCHOOL_ADMIN |

### Module 9 — Personalization (`module-personalization`)
| Method | Path | Pagination |
|---|---|---|
| GET | `/api/v1/students/me/learning-path?course_id={id}` | — |
| POST | `/api/v1/ai/personalization/recommendations/{id}/review` | — |
| POST | `/api/v1/ai/insight-reports/generate` | — (202 async) |
| GET/POST | `/api/v1/ai/insight-reports/{id}`, `.../review` | — |
| GET | `/api/v1/courses/{course_id}/analytics/learning-paths?class_id={id}` | CHUẨN 1 |

### Module 10 — Notification (`module-notification`)
| Method | Path | Pagination |
|---|---|---|
| GET | `/api/v1/me/notifications` | **CHUẨN 3** |
| GET | `/api/v1/me/notifications/unread-count` | Không áp dụng |
| PATCH | `.../notifications/{id}/read`, `.../read-all` | — |
| GET/PATCH | `/api/v1/me/notification-preferences` | — |
| POST | `/api/v1/admin/notifications/announce` | SCHOOL_ADMIN/SUPER_ADMIN, quota 10 announcement/24h/admin |

`POST /api/v1/admin/notifications/announce` (Module 10, J.6.3) nhận body:

```json
{ "title": "...", "body": "...", "scope": "SCHOOL | CLASS (tùy chọn)", "classId": "UUID (bắt buộc nếu CLASS)", "schoolId": "UUID (chỉ SUPER_ADMIN, với scope=SCHOOL)" }
```

Trả `201 { "announcementId": "UUID", "deliveredCount": n }`.

| `scope` | Người nhận |
|---|---|
| không truyền | `SCHOOL_ADMIN`: mọi tài khoản `ACTIVE` của trường mình. `SUPER_ADMIN`: toàn bộ tài khoản `ACTIVE` |
| `SCHOOL` | Mọi tài khoản `ACTIVE` của trường. `SCHOOL_ADMIN` dùng trường mình (truyền `schoolId` khác → 403); `SUPER_ADMIN` bắt buộc truyền `schoolId` |
| `CLASS` | Thành viên đang active của lớp (`left_at IS NULL`, cả STUDENT lẫn TEACHER) cộng `owner_teacher_id`, chỉ tài khoản `ACTIVE`. `SCHOOL_ADMIN` chỉ gửi lớp thuộc trường mình; `SUPER_ADMIN` gửi mọi lớp |

Loại `SYSTEM_ANNOUNCEMENT` vẫn tôn trọng notification preferences (người đã tắt không nhận, không tính vào `deliveredCount`).

| HTTP | Code | Trường hợp |
|---|---|---|
| 422 | `INVALID_ANNOUNCEMENT_SCOPE` | `scope` lạ; `scope=CLASS` thiếu `classId`; `SUPER_ADMIN` dùng `scope=SCHOOL` thiếu `schoolId` |
| 404 | `CLASS_NOT_FOUND` | Lớp không tồn tại hoặc đã soft-delete |
| 403 | `FORBIDDEN_CLASS_SCOPE` / `FORBIDDEN_SCHOOL_SCOPE` / `SCHOOL_SCOPE_REQUIRED` | `SCHOOL_ADMIN` gửi ngoài trường mình / không thuộc trường nào |
| 429 | `ANNOUNCEMENT_RATE_LIMITED` | Vượt quota (mặc định 10 announcement/24h/admin, cấu hình `educmind.notification.announcement-daily-limit`). Đếm từ bảng `notifications` theo `payload->>'announced_by'`; announcement có 0 người nhận không để lại dòng nào nên không tính quota |

### Module 10 — Reports (`module-reports`)
| Method | Path | Sync/Async |
|---|---|---|
| GET | `/api/v1/reports/student-progress`, `.../assessment-result` | Đồng bộ |
| GET | `/api/v1/courses/{course_id}/reports/class-progress` | Đồng bộ |
| POST/GET | `/api/v1/reports/export`, `/api/v1/reports/export/{id}` | Bất đồng bộ (202 + poll) |
| GET | `/api/v1/schools/{school_id}/reports/ai-usage` | SCHOOL_ADMIN, đồng bộ |

## 3. Mã lỗi dùng chung (không tạo trùng)

| Mã | HTTP | Ý nghĩa |
|---|---|---|
| `NOT_COURSE_OWNER` | 403 | Không phải owner/đồng giảng dạy course |
| `FORBIDDEN_NOT_SELF` | 403 | Cố truy cập dữ liệu người khác |
| `RATE_LIMIT_EXCEEDED` | 429 | Vượt quota AI Gateway (dùng chung cơ chế đếm M7/M8/M9) |
| `RESOURCE_NOT_READY` | 409 | Retrieval/Tutor gọi khi resource chưa `COMPLETED` |
| `MUST_CHANGE_PASSWORD` | 403 | Tài khoản chưa đổi mật khẩu tạm |

## Tài liệu liên quan
[`ARCHITECTURE.md`](./ARCHITECTURE.md) · [`DATABASE.md`](./DATABASE.md) · [`AI_RAG.md`](./AI_RAG.md) · [`CODING_RULES.md`](./CODING_RULES.md)
