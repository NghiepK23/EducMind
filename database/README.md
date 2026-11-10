# database/

- `schema_v2.sql` — toàn bộ DDL của Spec v2 (25 bảng, 13 enum, index, trigger) trong **một file**
  để đọc/tra cứu hoặc dựng nhanh một CSDL trống.
- `seed/` — chỗ đặt script seed thủ công (nếu cần). Hiện dữ liệu mẫu dev do
  `DevSeedService` trong backend tạo tự động khi bật `EDUCMIND_SEED=true`.

> Schema thật mà ứng dụng dùng nằm ở `backend/educmind-app/src/main/resources/db/migration/`
> (Flyway `V1..V7`) — đó là **nguồn sự thật duy nhất**. File `schema_v2.sql` ở đây là bản gộp
> cùng nội dung để tiện đọc, không được chỉnh sửa riêng lẻ.
