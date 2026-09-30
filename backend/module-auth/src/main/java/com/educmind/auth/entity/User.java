package com.educmind.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity `users` — Module01_Auth_SchoolClass_FINAL.md, mục A.4.
 *
 * [QUAN TRỌNG — đọc trước khi thêm quan hệ JPA mới]
 * Trường {@link #schoolId} trỏ tới bảng `schools`, nhưng `schools` thuộc module-schoolclass
 * (1 module Maven KHÁC). Ở đây CỐ TÌNH chỉ khai báo kiểu UUID thô (không @ManyToOne, không
 * @JoinColumn sang entity School) để module-auth KHÔNG phụ thuộc Maven vào module-schoolclass.
 *
 * Muốn lấy thông tin trường học của 1 user, gọi sang module-schoolclass qua Spring bean
 * (SchoolRepository.findById(user.getSchoolId())) ở tầng Service, không join trực tiếp ở tầng Entity.
 *
 * Tương tự, {@link #createdBy}/{@link #updatedBy} tự trỏ về chính bảng users — vẫn dùng UUID thô
 * (không dùng @ManyToOne self-reference) để nhất quán 1 quy ước cho toàn bộ entity trong dự án,
 * tránh vấn đề lazy-loading vòng lặp không cần thiết cho 1 field ít khi thực sự cần load đối tượng đầy đủ.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** Cross-module reference (module-schoolclass.schools.id) — xem giải thích ở đầu class. NULL cho SUPER_ADMIN. */
    @Column(name = "school_id")
    private UUID schoolId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** Same-module self-reference (users.id) — UUID thô, xem giải thích ở đầu class. */
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    /** Constructor rỗng bắt buộc phải có để Hibernate khởi tạo object khi đọc từ DB lên. */
    protected User() {
    }

    /** Constructor dùng khi tạo user mới trong code (Service layer sẽ gọi cái này). */
    public User(String email, String passwordHash, String fullName, Role role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // ------------------------------------------------------------------
    // Getters & setters
    // ------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
