package com.educmind.common.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port cho phép module khác (module-assessment, module-analytics) hỏi module-course
 * về learning objectives của 1 lesson, mà không phụ thuộc Maven trực tiếp vào module-course.
 *
 * Objective nằm trong JSONB của lessons.learning_objectives nên KHÔNG có FK cứng ở DB;
 * port này là chỗ duy nhất kiểm tra "objectiveId có thật sự thuộc lessonId không".
 *
 * Chỉ dùng kiểu nguyên thủy / record trung lập, không expose Entity của module-course.
 */
public interface LessonAccessPort {

    /** Thông tin tối thiểu của 1 objective (id + mô tả). */
    record ObjectiveInfo(UUID id, String text) {
    }

    /** Thông tin tối thiểu của 1 lesson (phục vụ Module 7 dựng prompt). */
    record LessonSummary(UUID id, UUID courseId, String title, String description) {
    }

    /**
     * Danh sách objective của lesson. Trả list rỗng nếu lesson không tồn tại,
     * đã bị xóa mềm, hoặc chưa có objective nào.
     */
    List<ObjectiveInfo> findObjectivesByLessonId(UUID lessonId);

    /**
     * true nếu lesson tồn tại (chưa xóa) VÀ objectiveId là id của 1 objective
     * nằm trong lesson đó. false trong mọi trường hợp còn lại.
     */
    boolean objectiveBelongsToLesson(UUID lessonId, UUID objectiveId);

    /**
     * true nếu lesson tồn tại (chưa xóa) VÀ lesson đó thuộc đúng course có id = courseId.
     * false trong mọi trường hợp còn lại (lesson không tồn tại, đã xóa, hoặc thuộc course khác).
     */
    boolean lessonBelongsToCourse(UUID lessonId, UUID courseId);

    /**
     * course_id của lesson. Optional.empty() nếu lesson không tồn tại hoặc đã bị xóa mềm.
     */
    Optional<UUID> findCourseIdByLessonId(UUID lessonId);

    /**
    * Tiêu đề + mô tả + courseId của lesson. Optional.empty() nếu lesson không tồn tại
    * hoặc đã bị xóa mềm.
    */
    Optional<LessonSummary> findLessonSummary(UUID lessonId);

    /** id resource mới nhất (chưa xóa) của lesson; Optional.empty() nếu lesson chưa có resource nào. */
    Optional<UUID> findLatestResourceIdByLessonId(UUID lessonId);
}
