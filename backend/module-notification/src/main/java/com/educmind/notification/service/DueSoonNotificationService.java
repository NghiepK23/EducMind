package com.educmind.notification.service;

import com.educmind.common.port.AssignmentDeadlinePort;
import com.educmind.common.port.AssignmentDeadlinePort.DueAssignment;
import com.educmind.common.port.CourseAccessPort;
import com.educmind.notification.entity.NotificationType;
import com.educmind.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * ASSIGNMENT_DUE_SOON (Module 10 J.5.3): nhắc học sinh chưa nộp khi assignment
 * sắp hết hạn trong 24h. Mỗi cặp (học sinh, assignment) chỉ nhận đúng 1 lần.
 */
@Service
public class DueSoonNotificationService {

    static final long WINDOW_HOURS = 24;

    private final AssignmentDeadlinePort deadlinePort;
    private final CourseAccessPort courseAccessPort;
    private final NotificationRepository notificationRepository;
    private final NotificationCreationService creationService;

    public DueSoonNotificationService(AssignmentDeadlinePort deadlinePort,
                                      CourseAccessPort courseAccessPort,
                                      NotificationRepository notificationRepository,
                                      NotificationCreationService creationService) {
        this.deadlinePort = deadlinePort;
        this.courseAccessPort = courseAccessPort;
        this.notificationRepository = notificationRepository;
        this.creationService = creationService;
    }

    /** @return tổng số notification đã tạo. */
    public int notifyDueSoon(OffsetDateTime now) {
        List<DueAssignment> due = deadlinePort.findPublishedDueBetween(now, now.plusHours(WINDOW_HOURS));
        int created = 0;
        for (DueAssignment a : due) {
            List<UUID> students = courseAccessPort.findActiveStudentIdsByCourse(a.courseId());
            Set<UUID> submitted = deadlinePort.findSubmittedStudentIds(a.assignmentId());
            Set<UUID> alreadyNotified = Set.copyOf(notificationRepository
                    .findRecipientIdsBySource(NotificationType.ASSIGNMENT_DUE_SOON, a.assignmentId()));
            List<UUID> recipients = students.stream()
                    .filter(id -> !submitted.contains(id) && !alreadyNotified.contains(id))
                    .toList();
            if (recipients.isEmpty()) {
                continue;
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("course_id", a.courseId().toString());
            payload.put("entity_type", "assignment");
            payload.put("entity_id", a.assignmentId().toString());
            payload.put("due_date", a.dueDate().toString());
            created += creationService.createForRecipients(NotificationType.ASSIGNMENT_DUE_SOON,
                    "ASSIGNMENT_DUE_SOON", a.assignmentId(), recipients,
                    "Bài tập sắp hết hạn",
                    "Bài \"" + a.title() + "\" sắp hết hạn nộp trong vòng 24 giờ.", payload);
        }
        return created;
    }
}
