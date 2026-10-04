package com.educmind.analytics.dto;

import com.educmind.analytics.repository.MasteryRepository;

import java.util.List;
import java.util.UUID;

/**
 * Tổng hợp mastery theo (lesson, objective) cho cả course hoặc 1 lớp (Module05 mục 5.2).
 * Không phân trang. Chưa có "số học sinh dưới ngưỡng" vì ngưỡng sư phạm còn [TBD].
 */
public class MasteryOverviewResponse {

    public static class Item {
        private final UUID lessonId;
        private final UUID learningObjectiveId;
        private final long studentCount;
        private final double avgMastery;
        private final long lowCount;
        private final long mediumCount;
        private final long highCount;

        public Item(MasteryRepository.OverviewRow r) {
            this.lessonId = r.getLessonId();
            this.learningObjectiveId = r.getLearningObjectiveId();
            this.studentCount = r.getStudentCount() == null ? 0L : r.getStudentCount();
            this.avgMastery = r.getAvgMastery() == null ? 0.0 : r.getAvgMastery();
            this.lowCount = r.getLowCount() == null ? 0L : r.getLowCount();
            this.mediumCount = r.getMediumCount() == null ? 0L : r.getMediumCount();
            this.highCount = r.getHighCount() == null ? 0L : r.getHighCount();
        }

        public UUID getLessonId() { return lessonId; }
        public UUID getLearningObjectiveId() { return learningObjectiveId; }
        public long getStudentCount() { return studentCount; }
        public double getAvgMastery() { return avgMastery; }
        public long getLowCount() { return lowCount; }
        public long getMediumCount() { return mediumCount; }
        public long getHighCount() { return highCount; }
    }

    private final UUID courseId;
    private final UUID classId;
    private final List<Item> items;

    public MasteryOverviewResponse(UUID courseId, UUID classId, List<Item> items) {
        this.courseId = courseId;
        this.classId = classId;
        this.items = List.copyOf(items);
    }

    public UUID getCourseId() { return courseId; }
    public UUID getClassId() { return classId; }
    public List<Item> getItems() { return items; }
}
