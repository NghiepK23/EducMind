package com.educmind.course.entity;

/**
 * Mức độ tư duy theo thang Bloom (Bloom's Taxonomy) — dùng để gắn nhãn độ khó/loại tư duy
 * cho từng {@link LearningObjective} trong learning_objectives (JSONB) của Lesson.
 * Thứ tự khai báo dưới đây đi từ mức tư duy thấp -> cao, đúng thứ tự chuẩn của thang Bloom.
 */
public enum BloomLevel {
    REMEMBER,     // Nhớ
    UNDERSTAND,   // Hiểu
    APPLY,        // Vận dụng
    ANALYZE,      // Phân tích
    EVALUATE,     // Đánh giá
    CREATE        // Sáng tạo
}
