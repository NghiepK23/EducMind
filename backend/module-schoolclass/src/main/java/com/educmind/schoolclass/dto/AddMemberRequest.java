package com.educmind.schoolclass.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/**
 * POST /api/v1/classes/{id}/members — Module01_Auth_SchoolClass_FINAL.md, mục B.6.
 * memberRole chỉ nhận TEACHER hoặc STUDENT — khớp member_role trong DDL class_members.
 * Service sẽ validate thêm: memberRole này phải khớp đúng role thật của user (users.role),
 * việc đó không thể check ở tầng DTO vì cần truy vấn DB (UserLookupPort).
 */
public class AddMemberRequest {

    @NotNull
    private UUID userId;

    @NotNull
    @Pattern(regexp = "TEACHER|STUDENT", message = "memberRole phải là TEACHER hoặc STUDENT")
    private String memberRole;

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getMemberRole() { return memberRole; }
    public void setMemberRole(String memberRole) { this.memberRole = memberRole; }
}
