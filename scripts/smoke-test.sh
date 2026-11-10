#!/usr/bin/env bash
set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TEACHER_EMAIL="${TEACHER_EMAIL:-teacher@educmind.vn}"
TEACHER_PASSWORD="${TEACHER_PASSWORD:-Teacher@123}"
STUDENT_EMAIL="${STUDENT_EMAIL:-student1@educmind.vn}"
STUDENT_PASSWORD="${STUDENT_PASSWORD:-Student@123}"

TMP="$(mktemp -d)"
LAST_BODY="$TMP/last_body"
LAST_STATUS="$TMP/last_status"

pass=0
fail=0

check() {
  if [ "$2" = "$3" ]; then
    printf 'PASS  %s\n' "$1"
    pass=$((pass + 1))
  else
    printf 'FAIL  %s  (mong doi: %s, nhan: %s)\n' "$1" "$2" "$3"
    if [ -f "$LAST_STATUS" ]; then
      printf '      HTTP %s  %s\n' "$(cat "$LAST_STATUS")" "$(head -c 400 "$LAST_BODY" | tr -d '\n')"
    fi
    fail=$((fail + 1))
  fi
}

api() {
  local method="$1" token="$2" path="$3" payload="${4:-}"
  if [ -n "$payload" ]; then
    curl -s -o "$LAST_BODY" -w '%{http_code}' -X "$method" -H "Authorization: Bearer $token" \
      -H 'Content-Type: application/json' -d "$payload" "$BASE_URL$path" > "$LAST_STATUS"
  else
    curl -s -o "$LAST_BODY" -w '%{http_code}' -X "$method" -H "Authorization: Bearer $token" \
      "$BASE_URL$path" > "$LAST_STATUS"
  fi
  cat "$LAST_BODY"
}

status() {
  local method="$1" token="$2" path="$3" payload="${4:-}"
  if [ -n "$payload" ]; then
    curl -s -o "$LAST_BODY" -w '%{http_code}' -X "$method" -H "Authorization: Bearer $token" \
      -H 'Content-Type: application/json' -d "$payload" "$BASE_URL$path" > "$LAST_STATUS"
  else
    curl -s -o "$LAST_BODY" -w '%{http_code}' -X "$method" -H "Authorization: Bearer $token" \
      "$BASE_URL$path" > "$LAST_STATUS"
  fi
  cat "$LAST_STATUS"
}

login() {
  curl -s -X POST "$BASE_URL/api/v1/auth/login" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"$2\"}" \
  | python3 -c 'import json,sys
try:
    print(json.load(sys.stdin)["data"]["accessToken"])
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
    print(json.load(sys.stdin)["data"][int(sys.argv[1])][sys.argv[2]])
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

jpick() {
  python3 -c 'import json,sys
want = sys.argv[1]
try:
    items = json.load(sys.stdin)["data"]
    print([i["id"] for i in items if i["status"] == want][0])
except Exception:
    print("ERR")' "$1"
}

jpick_not() {
  python3 -c 'import json,sys
want = sys.argv[1]
try:
    items = json.load(sys.stdin)["data"]
    print([i["id"] for i in items if i["status"] != want][0])
except Exception:
    print("ERR")' "$1"
}

jobj() {
  python3 -c 'import json,sys
try:
    print(json.load(sys.stdin)["data"][sys.argv[1]])
except Exception:
    print("ERR")' "$1"
}

jfind() {
  python3 -c 'import json,sys
key, want = sys.argv[1], sys.argv[2]
try:
    items = json.load(sys.stdin)["data"]
    print([i[key] for i in items if i[key] == want][0])
except Exception:
    print("ERR")' "$1" "$2"
}

jpickfield() {
  python3 -c 'import json,sys
field, match_field, match_value = sys.argv[1], sys.argv[2], sys.argv[3]
try:
    items = json.load(sys.stdin)["data"]
    print([i[field] for i in items if i[match_field] == match_value][0])
except Exception:
    print("ERR")' "$1" "$2" "$3"
}

echo "== EducMind smoke test — $BASE_URL =="

api GET "" /api/health > /dev/null
check "health" "200" "$(cat "$LAST_STATUS")"

TEACHER_TOKEN="$(login "$TEACHER_EMAIL" "$TEACHER_PASSWORD")"
if [ -z "$TEACHER_TOKEN" ]; then
  echo "FAIL  dang nhap giao vien ($TEACHER_EMAIL)"
  exit 1
fi
echo "PASS  dang nhap giao vien"

check "GET /users/me tra role TEACHER" "TEACHER" "$(api GET "$TEACHER_TOKEN" /api/v1/users/me | jsfield role)"
check "giao vien co 1 lop" "1" "$(api GET "$TEACHER_TOKEN" /api/v1/classes | jlen)"
check "giao vien co 1 khoa hoc" "1" "$(api GET "$TEACHER_TOKEN" /api/v1/courses | jlen)"

COURSE_ID="$(api GET "$TEACHER_TOKEN" /api/v1/courses | jfield 0 id)"
echo "      courseId = $COURSE_ID"

TOPICS_TEACHER="$(api GET "$TEACHER_TOKEN" "/api/v1/courses/$COURSE_ID/topics")"
check "giao vien thay 2 chu de" "2" "$(printf '%s' "$TOPICS_TEACHER" | jlen)"

PUBLISHED_TOPIC="$(printf '%s' "$TOPICS_TEACHER" | jpick PUBLISHED)"
DRAFT_TOPIC="$(printf '%s' "$TOPICS_TEACHER" | jpick_not PUBLISHED)"
echo "      topicPublished = $PUBLISHED_TOPIC"
echo "      topicDraft     = $DRAFT_TOPIC"

check "chu de 1 co 2 bai hoc" "2" "$(api GET "$TEACHER_TOKEN" "/api/v1/topics/$PUBLISHED_TOPIC/lessons" | jlen)"
check "chu de 2 co 1 bai hoc" "1" "$(api GET "$TEACHER_TOKEN" "/api/v1/topics/$DRAFT_TOPIC/lessons" | jlen)"

PUBLISHED_LESSON="$(api GET "$TEACHER_TOKEN" "/api/v1/topics/$PUBLISHED_TOPIC/lessons" | jpick PUBLISHED)"
check "bai hoc da xuat ban co 2 muc tieu" "2" \
  "$(api GET "$TEACHER_TOKEN" "/api/v1/lessons/$PUBLISHED_LESSON/objectives" | jlen)"

check "chu de khong ton tai tra 404" "404" \
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

curl -s -o /dev/null -w '%{http_code}' "$BASE_URL/api/v1/courses" > "$LAST_STATUS"
check "goi API khong co token tra 401" "401" "$(cat "$LAST_STATUS")"

curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE_URL/api/v1/auth/login" \
  -H 'Content-Type: application/json' -d '{"email":"teacher@educmind.vn","password":"sai"}' > "$LAST_STATUS"
check "sai mat khau tra 401" "401" "$(cat "$LAST_STATUS")"

check "hoc sinh doc duoc thong bao" "0" \
  "$(api GET "$STUDENT_TOKEN" /api/v1/me/notifications/unread-count | jsfield unreadCount)"

echo
echo "== Phase 2: bai tap (US-T03/T04, US-S02/S04/S07) =="

TS="$(date +%s)"
DUE="$(python3 -c 'import datetime;print((datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(days=5)).isoformat())')"
CLOSE="$(python3 -c 'import datetime;print((datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(days=8)).isoformat())')"
OBJECTIVE_ID="$(api GET "$TEACHER_TOKEN" "/api/v1/lessons/$PUBLISHED_LESSON/objectives" | jfield 0 id)"

ASSIGNMENT_BODY="$(printf '{"title":"Bai tap smoke %s","description":"Mo ta bai tap","dueDate":"%s","maxScore":10,"allowLate":true,"lateCloseDate":"%s","objectiveIds":["%s"]}' "$TS" "$DUE" "$CLOSE" "$OBJECTIVE_ID")"

api POST "$TEACHER_TOKEN" "/api/v1/courses/$COURSE_ID/assignments" "$ASSIGNMENT_BODY" > /dev/null
check "giao vien tao bai tap (nhap)" "201" "$(cat "$LAST_STATUS")"

ASSIGNMENT_ID="$(cat "$LAST_BODY" | jobj id)"
api GET "$TEACHER_TOKEN" "/api/v1/assignments/$ASSIGNMENT_ID" > /dev/null
check "bai tap moi o trang thai DRAFT" "DRAFT" "$(cat "$LAST_BODY" | jsfield status)"

api POST "$TEACHER_TOKEN" "/api/v1/assignments/$ASSIGNMENT_ID/publish" "" > /dev/null
check "xuat ban bai tap" "PUBLISHED" "$(cat "$LAST_BODY" | jsfield status)"

api GET "$STUDENT_TOKEN" "/api/v1/courses/$COURSE_ID/assignments" > /dev/null
check "hoc sinh thay bai tap da xuat ban" "1" "$(python3 -c 'import json,sys
try:
    data = json.load(open(sys.argv[1]))["data"]
    print(1 if len([a for a in data if a["status"] == "PUBLISHED"]) >= 1 else 0)
except Exception:
    print("ERR")' "$LAST_BODY")"

api POST "$STUDENT_TOKEN" "/api/v1/assignments/$ASSIGNMENT_ID/submission" '{"textContent":"Bai lam cua hoc sinh"}' > /dev/null
check "hoc sinh nop bai" "201" "$(cat "$LAST_STATUS")"
SUBMISSION_ID="$(cat "$LAST_BODY" | jobj id)"

api POST "$STUDENT_TOKEN" "/api/v1/assignments/$ASSIGNMENT_ID/submission" '{"textContent":"nop lan 2"}' > /dev/null
check "nop lai khi da co bai tra 409" "409" "$(cat "$LAST_STATUS")"

api GET "$TEACHER_TOKEN" "/api/v1/assignments/$ASSIGNMENT_ID/submissions" > /dev/null
check "danh sach cham co du 5 hoc sinh" "5" "$(cat "$LAST_BODY" | jlen)"

STUDENT_SUBMISSION_ID="$(cat "$LAST_BODY" | jpickfield submissionId studentId "$(api GET "$STUDENT_TOKEN" /api/v1/users/me | jobj id)")"

api PATCH "$TEACHER_TOKEN" "/api/v1/submissions/$STUDENT_SUBMISSION_ID/grade" '{"score":8.5,"feedback":"Tot"}' > /dev/null
check "giao vien cham diem" "8.5" "$(cat "$LAST_BODY" | jsfield score)"

api PATCH "$TEACHER_TOKEN" "/api/v1/submissions/$STUDENT_SUBMISSION_ID/grade" '{"score":99}' > /dev/null
check "diem vuot diem toi da tra 422" "422" "$(cat "$LAST_STATUS")"

api GET "$STUDENT_TOKEN" "/api/v1/assignments/$ASSIGNMENT_ID/submission" > /dev/null
check "hoc sinh thay diem da cham (pendingGrade=false)" "False" "$(cat "$LAST_BODY" | jsfield pendingGrade)"

api PATCH "$STUDENT_TOKEN" "/api/v1/assignments/$ASSIGNMENT_ID/submission" '{"textContent":"sua sau khi cham"}' > /dev/null
check "sua bai sau khi da cham tra 409" "409" "$(cat "$LAST_STATUS")"

api GET "$TEACHER_TOKEN" /api/v1/me/notifications/unread-count > /dev/null
check "giao vien nhan thong bao khi co bai nop" "1" "$(python3 -c 'import json,sys
try:
    print(1 if json.load(open(sys.argv[1]))["data"]["unreadCount"] >= 1 else 0)
except Exception:
    print("ERR")' "$LAST_BODY")"

api DELETE "$TEACHER_TOKEN" "/api/v1/assignments/$ASSIGNMENT_ID" > /dev/null
check "giao vien xoa bai tap tra 200" "200" "$(cat "$LAST_STATUS")"

echo
echo "Ket qua: $pass PASS, $fail FAIL"
[ "$fail" -eq 0 ]
