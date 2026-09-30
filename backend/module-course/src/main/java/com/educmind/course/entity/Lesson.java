package com.educmind.course.entity;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity ứng với bảng `lessons` — Module03_Course_Lesson_Resource_FINAL.md, mục C.4/C.5.
 *
 * {@link #learningObjectives} map cột learning_objectives (JSONB) sang List<LearningObjective>
 * — dùng @JdbcTypeCode(SqlTypes.JSON) (Hibernate 6 native JSON support, KHÔNG cần thư viện
 * hibernate-types ngoài). Hibernate sẽ dùng Jackson (ObjectMapper) để serialize/deserialize
 * 2 chiều: Java List<LearningObjective> <-> JSON array text <-> cột jsonb trong Postgres.
 * [LƯU Ý khi build] Cơ chế này cần jackson-databind có mặt trên classpath của module-course
 * — nếu build lỗi thiếu JsonFormatMapper, cần thêm dependency jackson-databind vào pom.xml.
 *
 * {@link #publishStatus}: KHÔNG set trực tiếp qua setter public — chỉ đổi qua {@link #publish()}
 * / {@link #unpublish()} để đảm bảo rule nghiệp vụ đã chốt với Nghiệp: publishedAt chỉ được
 * set 1 LẦN DUY NHẤT khi publish lần đầu, và GIỮ NGUYÊN khi unpublish (không bị xóa về null).
 */
@Entity
@Table(name = "lessons")
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Same-module reference (courses.id) — bất biến, 1 lesson không "chuyển" sang course khác. */
    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "learning_objectives", columnDefinition = "jsonb")
    private List<LearningObjective> learningObjectives = new ArrayList<>();

    @Column(name = "order_index", nullable = false)
    private int orderIndex = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "publish_status", nullable = false, length = 20)
    private PublishStatus publishStatus = PublishStatus.DRAFT;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    /** Cross-module reference (module-auth.users.id) — bất biến, giống created_by ở Course. */
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Lesson() {
    }

    public Lesson(UUID courseId, String title, UUID createdBy) {
        this.courseId = courseId;
        this.title = title;
        this.createdBy = createdBy;
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

    /**
     * [CHỐT với Nghiệp] Chuyển sang PUBLISHED. published_at chỉ set nếu đang null (lần
     * publish đầu tiên) — nếu lesson đã từng publish rồi bị unpublish, publish lại sẽ
     * KHÔNG ghi đè published_at cũ (giữ mốc publish gốc, đúng tinh thần "audit trail").
     */
    public void publish() {
        this.publishStatus = PublishStatus.PUBLISHED;
        if (this.publishedAt == null) {
            this.publishedAt = OffsetDateTime.now();
        }
    }

    /**
     * [CHỐT với Nghiệp] Chuyển về DRAFT. published_at GIỮ NGUYÊN, không set về null.
     */
    public void unpublish() {
        this.publishStatus = PublishStatus.DRAFT;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<LearningObjective> getLearningObjectives() {
        return learningObjectives;
    }

    public void setLearningObjectives(List<LearningObjective> learningObjectives) {
        this.learningObjectives = learningObjectives;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public PublishStatus getPublishStatus() {
        return publishStatus;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
