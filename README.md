# EducMind

Nền tảng học tập thích ứng: giáo viên tổ chức lớp → khóa học → chủ đề → bài học → mục tiêu học tập,
giao bài tập/bài kiểm tra, xem mức độ nắm kiến thức và lỗ hổng của từng học sinh.

> **Trạng thái:** repo đang được chuyển sang **Spec v2** (2 vai trò TEACHER/STUDENT, 25 bảng).
> **Phase 1** (auth, lớp học, khóa học/chủ đề/bài học/mục tiêu/tài liệu, thông báo) đã viết xong
> nhưng **chưa chạy `mvn verify`** — xem [`docs/spec-v2/MIGRATION-STATUS.md`](docs/spec-v2/MIGRATION-STATUS.md).

## Stack

| Thành phần | Công nghệ |
|---|---|
| Backend | Java 21, Spring Boot 4.1.1, Spring Security + JWT (JJWT 0.12.6), Hibernate/JPA |
| CSDL | PostgreSQL 17 (+ pgvector cho phase AI), Flyway migration |
| Cache/hàng đợi | Redis 8 (dùng từ Phase 3) |
| AI | Google Gemini (Phase 3) |

Không còn frontend trong repo (đã dọn); API thuần REST, test bằng curl/Postman.

## Cấu trúc

```
backend/
├── pom.xml                    # 6 module đang build
├── educmind-common/           # ApiResponse envelope, exception, phân trang, PORT liên module
├── educmind-app/              # Spring Boot app: SecurityConfig, cấu hình, seed, migration
├── module-auth/               # users, đăng nhập, đặt lại mật khẩu
├── module-schoolclass/        # classes, class_members
├── module-course/             # courses, course_topics, lessons, learning_objectives, resources
├── module-notification/       # đọc thông báo (bản ghi do trigger DB sinh)
├── module-assignment/  …      # ⚠️ code cũ, CHƯA port sang v2 — không nằm trong build
├── module-assessment/  …
├── module-analytics/   …
└── module-teacher-assistant/  …
database/schema_v2.sql         # DDL v2 gộp (đọc/tra cứu; nguồn thật là Flyway V1..V7)
docs/spec-v2/                  # yêu cầu, API, gap analysis, trạng thái chuyển đổi
docker-compose.yml             # postgres + redis
```

## Chạy thử

```bash
# 1. CSDL (xoá volume nếu đã từng chạy bản v1 — xem ghi chú bên dưới)
docker compose down -v
docker compose up -d postgres

# 2. Build SẠCH (clean là bắt buộc: target/classes cũ còn sót migration V1..V11 của bản v1)
cd backend
./mvnw clean install -DskipTests

# 3. Chạy — Flyway tự tạo 25 bảng khi khởi động
EDUCMIND_SEED=true ./mvnw -pl educmind-app spring-boot:run
```

> ⚠️ **Nếu máy bạn đã từng chạy bản v1**: schema cũ đang ở `flyway_schema_history` version 11 và
> dùng lại đúng các số `V1..V7` với nội dung khác → Flyway sẽ báo lỗi checksum hoặc chạy trên schema
> sai. Bắt buộc `docker compose down -v` để xoá volume rồi `up` lại (dữ liệu dev, không cần giữ).
> Tương tự, phải `mvn clean` vì `spring-boot:run` không tự xoá file `.sql` cũ trong `target/classes`.

Khi `EDUCMIND_SEED=true`, hệ thống tạo sẵn:

| Vai trò | Email | Mật khẩu |
|---|---|---|
| Giáo viên | `teacher@educmind.vn` | `Teacher@123` |
| Học sinh (5 em) | `student1@educmind.vn` … `student5@educmind.vn` | `Student@123` |

Seed tạo lớp **10A1**, khóa **Tin học 10** với 2 chủ đề, 3 bài học (1 bài đã xuất bản kèm 2 mục tiêu).

Kiểm tra toàn bộ API Phase 1 (health, đăng nhập 2 vai trò, phân quyền, nội dung):

```bash
./scripts/smoke-test.sh
```

```bash
# Đăng nhập
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"teacher@educmind.vn","password":"Teacher@123"}'

# Dùng accessToken nhận được
curl -s http://localhost:8080/api/v1/courses -H "Authorization: Bearer $TOKEN"
```

> Database dev có thể **xoá và tạo lại** bất cứ lúc nào (`docker compose down -v`) — khoá chính đã
> đổi sang UUID nên không cần convert dữ liệu cũ.

## Biến môi trường

| Biến | Mặc định | Ghi chú |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/educmind` | |
| `DB_USER` / `DB_PASSWORD` | `educmind` / `educmind_dev` | đổi ở production |
| `JWT_SECRET` | chuỗi dev | **bắt buộc đổi ở production**, ≥ 32 byte |
| `JWT_TTL_MINUTES` | `60` | access token, không có refresh token |
| `FRONT_URL` | `http://localhost:5173` | gốc link đặt lại mật khẩu gửi qua email |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | trống | trống thì link đặt mật khẩu chỉ ghi vào log |
| `STORAGE_PATH` | `./storage/resources` | nơi lưu tài liệu tải lên |
| `EDUCMIND_SEED` | `false` | bật seed dữ liệu dev |
| `JPA_SHOW_SQL` | `false` | |

## Tài liệu

| File | Nội dung |
|---|---|
| [`docs/spec-v2/01-YEU-CAU.md`](docs/spec-v2/01-YEU-CAU.md) | Đặc tả v2: 2 vai trò, 29 user story, quy tắc nghiệp vụ |
| [`docs/spec-v2/02-API.md`](docs/spec-v2/02-API.md) | Danh mục endpoint đã triển khai + mã lỗi |
| [`docs/spec-v2/03-GAP-ANALYSIS.md`](docs/spec-v2/03-GAP-ANALYSIS.md) | Chênh lệch repo ↔ spec và lộ trình 5 phase |
| [`docs/spec-v2/MIGRATION-STATUS.md`](docs/spec-v2/MIGRATION-STATUS.md) | Đã làm / đang park / chưa kiểm chứng |
| [`docs/REPO_REVIEW.md`](docs/REPO_REVIEW.md) | Đánh giá repo trước khi chuyển đổi |
| `docs/` (các file còn lại) | **Tài liệu v1 — chỉ để tham khảo lịch sử** |

## Lộ trình

| Phase | Nội dung |
|---|---|
| **1 ✅** | Schema v2 + auth + lớp + khóa học/chủ đề/bài học/mục tiêu/tài liệu + thông báo |
| 2 | Bài tập (US-T03/T04), ngân hàng câu hỏi & bài kiểm tra (US-T05/T06), học sinh làm bài (US-S02/S03/S04/S07) |
| 3 | Chấm tự động & mastery (US-T07/T08, S05/S06, H01), AI sinh câu hỏi nháp (US-T09/T10), gợi ý (US-T13–T15) |
| 4 | Dọn module cũ, bổ sung AC còn thiếu (AC-G03.4, AC-T11.1), tài liệu API đầy đủ |
| 5 | CI + Testcontainers + test trigger DB |
