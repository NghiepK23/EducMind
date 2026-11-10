#!/usr/bin/env bash
set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TEACHER_EMAIL="${TEACHER_EMAIL:-teacher@educmind.vn}"
TEACHER_PASSWORD="${TEACHER_PASSWORD:-Teacher@123}"
STUDENT_EMAIL="${STUDENT_EMAIL:-student1@educmind.vn}"
STUDENT_PASSWORD="${STUDENT_PASSWORD:-Student@123}"

pass=0
fail=0

check() {
  if [ "$2" = "$3" ]; then
    printf 'PASS  %s\n' "$1"
    pass=$((pass + 1))
  else
    printf 'FAIL  %s  (mong doi: %s, nhan: %s)\n' "$1" "$2" "$3"
    fail=$((fail + 1))
  fi
}

api() {
  local method="$1" token="$2" path="$3" body="${4:-}"
  if [ -n "$body" ]; then
    curl -s -X "$method" -H "Authorization: Bearer $token" \
      -H 'Content-Type: application/json' -d "$body" "$BASE_URL$path"
  else
    curl -s -X "$method" -H "Authorization: Bearer $token" "$BASE_URL$path"
  fi
}

status() {
  local method="$1" token="$2" path="$3" body="${4:-}"
  if [ -n "$body" ]; then
    curl -s -o /dev/null -w '%{http_code}' -X "$method" -H "Authorization: Bearer $token" \
      -H 'Content-Type: application/json' -d "$body" "$BASE_URL$path"
  else
    curl -s -o /dev/null -w '%{http_code}' -X "$method" -H "Authorization: Bearer $token" "$BASE_URL$path"
  fi
}

login() {
  curl -s -X POST "$BASE_URL/api/v1/auth/login" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"$2\"}" \
  | python3 -c 'import json,sys
try:
    d = json.load(sys.stdin)
    print(d["data"]["accessToken"])
except Exception:
    print("")'
}

jlen() {
  python3 -c 'import json,sys
try:
    print(len(json.load(sys.stdin)["data"]))
except Exception:
    print("ERR")'
}

jfield() {
  python3 -c 'import json,sys
try:
    print(json.load(sys.stdin)["data"][sys.argv[1]][sys.argv[2]])
except Exception:
    print("ERR")' "$1" "$2"
}

jsfield() {
  python3 -c 'import json,sys
try:
    print(json.load(sys.stdin)["data"][sys.argv[1]])
except Exception:
    print("ERR")' "$1"
}

echo "== EducMind smoke test — $BASE_URL =="

check "health" "200" "$(status GET '' /api/health)"

TEACHER_TOKEN="$(login "$TEACHER_EMAIL" "$TEACHER_PASSWORD")"
if [ -z "$TEACHER_TOKEN" ]; then
  echo "FAIL  dang nhap giao vien ($TEACHER_EMAIL) — kiem tra EDUCMIND_SEED=true va mat khau"
  exit 1
fi
echo "PASS  dang nhap giao vien"

check "GET /users/me tra role TEACHER" "TEACHER" "$(api GET "$TEACHER_TOKEN" /api/v1/users/me | jsfield role)"
check "giao vien co 1 lop" "1" "$(api GET "$TEACHER_TOKEN" /api/v1/classes | jlen)"
check "giao vien co 1 khoa hoc" "1" "$(api GET "$TEACHER_TOKEN" /api/v1/courses | jlen)"

COURSE_ID="$(api GET "$TEACHER_TOKEN" /api/v1/courses | jfield 0 id)"
TOPICS_TEACHER="$(api GET "$TEACHER_TOKEN" "/api/v1/courses/$COURSE_ID/topics")"
check "giao vien thay 2 chu de" "2" "$(printf '%s' "$TOPICS_TEACHER" | jlen)"

PUBLISHED_TOPIC="$(printf '%s' "$TOPICS_TEACHER" | python3 -c 'import json,sys
try:
    print([t["id"] for t in json.load(sys.stdin)["data"] if t["status"] == "PUBLISHED"][0])
except Exception:
    print("ERR")')"
DRAFT_TOPIC="$(printf '%s' "$TOPICS_TEACHER" | python3 -c 'import json,sys
try:
    print([t["id"] for t in json.load(sys.stdin)["data"] if t["status"] != "PUBLISHED"][0])
except Exception:
    print("ERR")')"

check "chu de 1 co 2 bai hoc" "2" "$(api GET "$TEACHER_TOKEN" "/api/v1/topics/$PUBLISHED_TOPIC/lessons" | jlen)"
check "chu de 2 co 1 bai hoc" "1" "$(api GET "$TEACHER_TOKEN" "/api/v1/topics/$DRAFT_TOPIC/lessons" | jlen)"

PUBLISHED_LESSON="$(api GET "$TEACHER_TOKEN" "/api/v1/topics/$PUBLISHED_TOPIC/lessons" | python3 -c 'import json,sys
try:
    print([l["id"] for l in json.load(sys.stdin)["data"] if l["status"] == "PUBLISHED"][0])
except Exception:
    print("ERR")')"
check "bai hoc da xuat ban co 2 muc tieu" "2" "$(api GET "$TEACHER_TOKEN" "/api/v1/lessons/$PUBLISHED_LESSON/objectives" | jlen)"

check "xoa mem chu de (khong ton tai) tra 404" "404" \
  "$(status DELETE "$TEACHER_TOKEN" /api/v1/topics/00000000-0000-0000-0000-000000000000)"

STUDENT_TOKEN="$(login "$STUDENT_EMAIL" "$STUDENT_PASSWORD")"
if [ -z "$STUDENT_TOKEN" ]; then
  echo "FAIL  dang nhap hoc sinh ($STUDENT_EMAIL)"
  exit 1
fi
echo "PASS  dang nhap hoc sinh"

check "GET /users/me tra role STUDENT" "STUDENT" "$(api GET "$STUDENT_TOKEN" /api/v1/users/me | jsfield role)"
check "hoc sinh co 1 khoa hoc" "1" "$(api GET "$STUDENT_TOKEN" /api/v1/courses | jlen)"
check "hoc sinh chi thay 1 chu de da xuat ban" "1" \
  "$(api GET "$STUDENT_TOKEN" "/api/v1/courses/$COURSE_ID/topics" | jlen)"
check "hoc sinh chi thay bai hoc da xuat ban" "1" \
  "$(api GET "$STUDENT_TOKEN" "/api/v1/topics/$PUBLISHED_TOPIC/lessons" | jlen)"
check "hoc sinh khong xem duoc chu de nhap" "403" \
  "$(status GET "$STUDENT_TOKEN" "/api/v1/topics/$DRAFT_TOPIC/lessons")"
check "hoc sinh khong tao duoc lop" "403" \
  "$(status POST "$STUDENT_TOKEN" /api/v1/classes '{"name":"Hack"}')"
check "hoc sinh khong tao duoc khoa hoc" "403" \
  "$(status POST "$STUDENT_TOKEN" "/api/v1/classes/00000000-0000-0000-0000-000000000000/courses" '{"title":"Hack"}')"
check "goi API khong co token tra 401" "401" \
  "$(curl -s -o /dev/null -w '%{http_code}' "$BASE_URL/api/v1/courses")"
check "sai mat khau tra 401" "401" \
  "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE_URL/api/v1/auth/login" \
      -H 'Content-Type: application/json' -d '{"email":"teacher@educmind.vn","password":"sai-mat-khau"}')"

check "hoc sinh doc duoc thong bao" "0" \
  "$(api GET "$STUDENT_TOKEN" /api/v1/me/notifications/unread-count | jsfield unreadCount)"

echo
echo "Ket qua: $pass PASS, $fail FAIL"
[ "$fail" -eq 0 ]
