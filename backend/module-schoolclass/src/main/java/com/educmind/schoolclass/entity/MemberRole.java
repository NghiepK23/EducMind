package com.educmind.schoolclass.entity;

/**
 * Vai trò của 1 thành viên TRONG PHẠM VI 1 LỚP cụ thể — Module01_Auth_SchoolClass_FINAL.md,
 * mục B.4 (cột class_members.member_role).
 *
 * [Giải thích cho người mới] Đừng nhầm với com.educmind.auth.entity.Role (vai trò hệ thống).
 * Ví dụ: user có Role.TEACHER (hệ thống) có thể xuất hiện trong class_members của lớp 7A1
 * với MemberRole.TEACHER (nghĩa là "đồng giảng dạy" lớp đó) — 2 khái niệm khác tầng nhau.
 */
public enum MemberRole {
    TEACHER,
    STUDENT
}
