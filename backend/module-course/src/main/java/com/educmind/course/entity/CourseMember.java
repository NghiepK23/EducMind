package com.educmind.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity ứng với bảng `course_members` — không gian thành viên riêng của
 * TỪNG course (thay thế hoàn toàn vai trò của `course_class` cũ).
 *
 * Đây là entity MỚI, phát sinh từ thay đổi kiến trúc: mỗi Course không còn
 * N-N với Class hành chính, mà tự quản lý danh sách thành viên riêng (giáo
 * viên đồng giảng dạy + học sinh) — xem checkpoint "THAY ĐỔI KIẾN TRÚC LỚN".
 *
 * {@link #userId}, {@link #addedBy} là cross-module reference sang module-auth
 * (bảng users) — raw UUID, không @ManyToOne, đúng nguyên tắc đã chốt.
 * {@link #sourceClassId} là cross-module reference sang module-schoolclass
 * (bảng classes) — chỉ có ý nghĩa khi memberRole=STUDENT (lớp hành chính mà
 * học sinh này được chọn ra khi thêm vào course); NULL khi memberRole=TEACHER
 * (giáo viên đồng giảng dạy được thêm trực tiếp, không có "lớp hành chính
 * nguồn"). Ràng buộc này validate ở tầng ứng dụng (CourseService), không đặt
 * CHECK constraint ở DB để tránh phức tạp hóa DDL — xem ghi chú trong migration
 * V3__module_course_subject_and_members.sql.
 */
@Entity
@Table(name = "course_members")
public class CourseMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    /** Cross-module reference (module-auth.users.id). */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** TEACHER | STUDENT — xem MemberRole ở ClassMember (module-schoolclass) để tham khảo cách đặt tên tương tự. */
    @Column(name = "member_role", nullable = false, length = 20)
    private String memberRole;

    /** Cross-module reference (module-schoolclass.classes.id) — NULL nếu memberRole=TEACHER. */
    @Column(name = "source_class_id")
    private UUID sourceClassId;

    /** Cross-module reference (module-auth.users.id) — luôn là owner course theo rule đã chốt. */
    @Column(name = "added_by", nullable = false)
    private UUID addedBy;

    @Column(name = "added_at", nullable = false)
    private OffsetDateTime addedAt;

    /** null = thành viên đang active; có giá trị = đã bị gỡ (soft-remove, giữ lịch sử). */
    @Column(name = "left_at")
    private OffsetDateTime leftAt;

    protected CourseMember() {
    }

    public CourseMember(UUID courseId, UUID userId, String memberRole, UUID sourceClassId, UUID addedBy) {
        this.courseId = courseId;
        this.userId = userId;
        this.memberRole = memberRole;
        this.sourceClassId = sourceClassId;
        this.addedBy = addedBy;
        this.addedAt = OffsetDateTime.now();
    }

    // ------------------------------------------------------------------
    // Getters & setters
    // ------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getMemberRole() {
        return memberRole;
    }

    public UUID getSourceClassId() {
        return sourceClassId;
    }

    public void setSourceClassId(UUID sourceClassId) {
        this.sourceClassId = sourceClassId;
    }

    public UUID getAddedBy() {
        return addedBy;
    }

    public OffsetDateTime getAddedAt() {
        return addedAt;
    }

    public OffsetDateTime getLeftAt() {
        return leftAt;
    }

    public void setLeftAt(OffsetDateTime leftAt) {
        this.leftAt = leftAt;
    }
}
