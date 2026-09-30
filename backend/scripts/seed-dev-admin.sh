#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# seed-dev-admin.sh
#
# Tạo 1 tài khoản SCHOOL_ADMIN dùng cho DEV/TEST LOCAL — KHÔNG dùng cho production.
# Cần thiết vì luồng chính thức (POST /api/v1/users) chỉ SCHOOL_ADMIN mới gọi được
# (@PreAuthorize("hasRole('SCHOOL_ADMIN')")) — nên tài khoản SCHOOL_ADMIN đầu tiên
# trong hệ thống bắt buộc phải được tạo "ngoài luồng" (bootstrap problem kinh điển).
#
# Idempotent: chạy lại nhiều lần không lỗi, không tạo trùng (ON CONFLICT DO NOTHING).
# An toàn dùng lại sau khi `docker compose down -v` reset DB.
#
# Tài khoản tạo ra:
#   email:    admin1@example.com
#   password: Test1234!   (dùng chung bcrypt hash đã biết với student1@example.com,
#                          xem ghi chú cuối file nếu cần đổi sang hash khác)
#   role:     SCHOOL_ADMIN
#   school_id: 11111111-1111-1111-1111-111111111111 (trùng school_id với student1,
#              để test được luôn các luồng liên quan cùng trường, vd Class CRUD)
# ==============================================================================

CONTAINER_NAME="educmind-postgres"
DB_USER="educmind"
DB_NAME="educmind"

ADMIN_EMAIL="admin1@example.com"
ADMIN_FULL_NAME="Test School Admin"
ADMIN_SCHOOL_ID="11111111-1111-1111-1111-111111111111"
# bcrypt hash của "Test1234!" — cùng hash đã dùng cho student1@example.com trong dự án này
ADMIN_PASSWORD_HASH='$2b$12$lb5AMvpXviA1OjQCrQl4IuEmdU4lu1gifXbq8cJfKO0l6H7qtXnKm'

echo "Seeding SCHOOL_ADMIN dev account (${ADMIN_EMAIL}) vào container ${CONTAINER_NAME}..."

docker exec -i "${CONTAINER_NAME}" psql -U "${DB_USER}" -d "${DB_NAME}" <<SQL
INSERT INTO users (id, email, password_hash, full_name, role, school_id, status, must_change_password, created_at, updated_at)
VALUES (
  gen_random_uuid(),
  '${ADMIN_EMAIL}',
  '${ADMIN_PASSWORD_HASH}',
  '${ADMIN_FULL_NAME}',
  'SCHOOL_ADMIN',
  '${ADMIN_SCHOOL_ID}',
  'ACTIVE',
  false,
  now(),
  now()
)
ON CONFLICT (email) DO NOTHING;
SQL

echo ""
echo "Done. Đăng nhập bằng:"
echo "  email:    ${ADMIN_EMAIL}"
echo "  password: Test1234!"
echo ""
echo "(Nếu tài khoản đã tồn tại từ trước, script không làm gì cả — không báo lỗi.)"
