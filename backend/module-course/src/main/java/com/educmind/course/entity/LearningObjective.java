package com.educmind.course.entity;

import java.util.UUID;

/**
 * Đại diện 1 mục tiêu học tập bên trong cột learning_objectives (JSONB) của Lesson —
 * Module03_Course_Lesson_Resource_FINAL.md, mục C.4/C.5.
 *
 * QUAN TRỌNG: Đây KHÔNG phải JPA entity (không @Entity, không @Id, không map bảng riêng).
 * Nó là 1 POJO thuần được Jackson serialize/deserialize thành phần tử của mảng JSON lưu
 * trong cột learning_objectives — việc đọc/ghi JSONB do Hibernate 6 xử lý thông qua
 * @JdbcTypeCode(SqlTypes.JSON) khai báo ở field tương ứng trong Lesson.java.
 *
 * Vì Jackson cần tạo object rỗng rồi gọi setter để đổ dữ liệu khi đọc từ DB lên (bean-style
 * deserialization), class này dùng constructor không tham số PUBLIC (khác quy ước
 * "protected no-arg constructor" của JPA entity) và có setter cho mọi field, kể cả id.
 *
 * {@link #id} không phải PK Hibernate sinh — chỉ là UUID để phân biệt từng mục tiêu trong
 * mảng (vd khi sửa 1 mục tiêu cụ thể), do CourseService tự sinh (UUID.randomUUID()) lúc tạo.
 */
public class LearningObjective {

    private UUID id;
    private String text;
    private BloomLevel bloomLevel;

    public LearningObjective() {
    }

    public LearningObjective(UUID id, String text, BloomLevel bloomLevel) {
        this.id = id;
        this.text = text;
        this.bloomLevel = bloomLevel;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public BloomLevel getBloomLevel() {
        return bloomLevel;
    }

    public void setBloomLevel(BloomLevel bloomLevel) {
        this.bloomLevel = bloomLevel;
    }
}
