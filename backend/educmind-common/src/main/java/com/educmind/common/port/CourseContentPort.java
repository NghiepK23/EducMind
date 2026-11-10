package com.educmind.common.port;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port tra cứu cấu trúc nội dung khóa học (chủ đề/bài học/mục tiêu) mà không phụ thuộc
 * Maven vào module-course. Dùng cho module bài tập & bài kiểm tra: xác định phạm vi
 * (khóa học/chủ đề/bài học), trạng thái xuất bản và mục tiêu học tập hợp lệ.
 */
public interface CourseContentPort {

    record ObjectiveRef(UUID id, UUID courseId) {
    }

    /** course_id của chủ đề chưa xóa; empty nếu không tồn tại. */
    Optional<UUID> findTopicCourseId(UUID topicId);

    /** course_id của bài học chưa xóa; empty nếu không tồn tại. */
    Optional<UUID> findLessonCourseId(UUID lessonId);

    /** Chủ đề đã xuất bản và khóa học chưa xóa. */
    boolean isTopicPublished(UUID topicId);

    /** Bài học đã xuất bản (và chủ đề chứa nó cũng đã xuất bản). */
    boolean isLessonPublished(UUID lessonId);

    /** Thông tin các mục tiêu tồn tại (bỏ qua id không tồn tại). */
    List<ObjectiveRef> findObjectives(Collection<UUID> objectiveIds);

    /** Danh sách mục tiêu của một khóa học (qua bài học → chủ đề → khóa học). */
    List<ObjectiveRef> findObjectivesByCourse(UUID courseId);
}
