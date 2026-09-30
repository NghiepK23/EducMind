package com.educmind.auth.entity;

/**
 * 4 vai trò của toàn hệ thống — theo Module01_Auth_SchoolClass_FINAL.md, mục A.2.
 *
 * [Giải thích cho người mới] Đây là "role hệ thống" (system-wide role), khác với
 * MemberRole ở module-schoolclass (vai trò trong PHẠM VI 1 lớp học, chỉ có
 * TEACHER/STUDENT). Ví dụ: 1 user có role=TEACHER ở đây, nhưng trong bảng
 * class_members của từng lớp cụ thể, họ có thể là "TEACHER" (đồng giảng dạy)
 * ở lớp A, và hoàn toàn không xuất hiện trong lớp B.
 */
public enum Role {
    SUPER_ADMIN,
    SCHOOL_ADMIN,
    TEACHER,
    STUDENT
}
