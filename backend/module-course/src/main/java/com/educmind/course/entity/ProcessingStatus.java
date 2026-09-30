package com.educmind.course.entity;

/**
 * Trạng thái xử lý của Resource sau khi upload — Module03_Course_Lesson_Resource_FINAL.md.
 * Khớp với CHECK (processing_status IN ('PROCESSING', 'READY', 'FAILED')) trong
 * V2__module_course.sql.
 *
 * PROCESSING: vừa upload xong, đang xử lý nền (vd virus scan, tạo thumbnail...).
 * READY: xử lý xong, có thể download/hiển thị bình thường.
 * FAILED: xử lý lỗi — cần hiển thị thông báo lỗi cho người dùng, không cho download.
 */
public enum ProcessingStatus {
    PROCESSING,
    READY,
    FAILED
}
