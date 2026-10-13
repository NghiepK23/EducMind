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

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity ứng với bảng `resources` — Module03_Course_Lesson_Resource_FINAL.md, mục C.4/C.5.
 *
 * {@link #lessonId} có thể null (resource gắn thẳng vào course, không thuộc lesson cụ thể
 * nào — endpoint POST /courses/{id}/resources với lesson_id=NULL).
 *
 * Các field mô tả file vật lý đã upload (filename, mimeType, sizeBytes, storageKey) đều
 * BẤT BIẾN sau khi tạo — sửa nội dung 1 resource đang tồn tại là sai nghiệp vụ; muốn thay
 * file phải xóa resource cũ và upload resource mới, tránh storage_key (đường dẫn thật) lệch
 * khỏi file vật lý.
 *
 * {@link #processingStatus}: KHÔNG set trực tiếp qua setter public — chỉ đổi qua
 * {@link #markReady()} / {@link #markFailed()}, cùng nguyên tắc encapsulation như
 * Lesson.publish()/unpublish().
 */
@Entity
@Table(name = "resources")
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Same-module reference (courses.id) — bất biến. */
    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    /** Same-module reference (lessons.id) — có thể null, bất biến. */
    @Column(name = "lesson_id", updatable = false)
    private UUID lessonId;

    @Column(nullable = false, length = 255, updatable = false)
    private String filename;

    @Column(name = "mime_type", nullable = false, length = 255, updatable = false)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long sizeBytes;

    @Column(name = "storage_key", nullable = false, length = 512, updatable = false)
    private String storageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private ProcessingStatus processingStatus = ProcessingStatus.PROCESSING;

    /** Cross-module reference (module-auth.users.id) — bất biến. */
    @Column(name = "uploaded_by", nullable = false, updatable = false)
    private UUID uploadedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Resource() {
    }

    public Resource(UUID courseId, UUID lessonId, String filename, String mimeType,
                     long sizeBytes, String storageKey, UUID uploadedBy) {
        this.courseId = courseId;
        this.lessonId = lessonId;
        this.filename = filename;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
        this.uploadedBy = uploadedBy;
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

    /** Đưa về PROCESSING — dùng khi xử lý lại (reprocess) tài liệu cho RAG. */
    public void markProcessing() {
        this.processingStatus = ProcessingStatus.PROCESSING;
    }

    /** Đánh dấu xử lý nền xong, file sẵn sàng để download/hiển thị. */
    public void markReady() {
        this.processingStatus = ProcessingStatus.READY;
    }

    /** Đánh dấu xử lý nền thất bại — cần hiển thị lỗi cho người dùng, không cho download. */
    public void markFailed() {
        this.processingStatus = ProcessingStatus.FAILED;
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

    public UUID getLessonId() {
        return lessonId;
    }

    public String getFilename() {
        return filename;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
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
