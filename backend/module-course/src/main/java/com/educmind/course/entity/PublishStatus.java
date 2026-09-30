package com.educmind.course.entity;

/**
 * Trạng thái xuất bản của Lesson — Module03_Course_Lesson_Resource_FINAL.md, mục C.5.
 * Khớp với CHECK (publish_status IN ('DRAFT', 'PUBLISHED')) trong V2__module_course.sql.
 *
 * DRAFT: chỉ giáo viên (owner/đồng giảng dạy) thấy được.
 * PUBLISHED: học sinh thuộc class đã link course mới thấy được (xem business rule).
 */
public enum PublishStatus {
    DRAFT,
    PUBLISHED
}
