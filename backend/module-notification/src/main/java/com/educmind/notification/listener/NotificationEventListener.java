package com.educmind.notification.listener;

import com.educmind.common.event.AiReviewPendingEvent;
import com.educmind.common.event.AssessmentGradedEvent;
import com.educmind.common.event.AssignmentGradedEvent;
import com.educmind.common.event.AssignmentPublishedEvent;
import com.educmind.common.event.KnowledgeGapOpenedEvent;
import com.educmind.common.event.RecommendationCreatedEvent;
import com.educmind.common.port.CourseAccessPort;
import com.educmind.notification.entity.NotificationType;
import com.educmind.notification.service.NotificationCreationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lắng nghe event từ Module 4/5. AFTER_COMMIT: chỉ chạy khi nghiệp vụ gốc đã commit;
 * REQUIRES_NEW: ghi notification trong transaction riêng; lỗi ở đây chỉ log, không ảnh
 * hưởng nghiệp vụ nộp/chấm bài.
 */
@Component
public class NotificationEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationCreationService creationService;
    private final CourseAccessPort courseAccessPort;

    public NotificationEventListener(NotificationCreationService creationService,
                                     CourseAccessPort courseAccessPort) {
        this.creationService = creationService;
        this.courseAccessPort = courseAccessPort;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAssignmentPublished(AssignmentPublishedEvent e) {
        try {
            List<UUID> students = courseAccessPort.findActiveStudentIdsByCourse(e.courseId());
            creationService.createForRecipients(NotificationType.ASSIGNMENT_PUBLISHED,
                    "ASSIGNMENT_PUBLISHED", e.assignmentId(), students,
                    "Bài tập mới", "Bài tập \"" + e.title() + "\" vừa được giao.",
                    payload(e.courseId(), "assignment", e.assignmentId(), null));
        } catch (Exception ex) {
            LOG.error("Tạo notification ASSIGNMENT_PUBLISHED thất bại: assignment={}", e.assignmentId(), ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAssignmentGraded(AssignmentGradedEvent e) {
        try {
            creationService.createForRecipients(NotificationType.ASSIGNMENT_GRADED,
                    "ASSIGNMENT_GRADED", e.submissionId(), List.of(e.studentId()),
                    "Bài tập đã được chấm",
                    "Bài \"" + e.assignmentTitle() + "\": " + fmt(e.score()) + "/" + fmt(e.maxScore()) + " điểm.",
                    payload(e.courseId(), "assignment", e.assignmentId(), null));
        } catch (Exception ex) {
            LOG.error("Tạo notification ASSIGNMENT_GRADED thất bại: submission={}", e.submissionId(), ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAssessmentGraded(AssessmentGradedEvent e) {
        try {
            String score = e.totalScore() == null ? "" : " Tổng điểm: " + fmt(e.totalScore()) + ".";
            creationService.createForRecipients(NotificationType.ASSESSMENT_GRADED,
                    "ASSESSMENT_GRADED", e.attemptId(), List.of(e.studentId()),
                    "Bài kiểm tra đã được chấm", "Kết quả bài kiểm tra của bạn đã có." + score,
                    payload(e.courseId(), "assessment_attempt", e.attemptId(), null));
        } catch (Exception ex) {
            LOG.error("Tạo notification ASSESSMENT_GRADED thất bại: attempt={}", e.attemptId(), ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onKnowledgeGapOpened(KnowledgeGapOpenedEvent e) {
        try {
            Map<String, Object> p = payload(e.courseId(), "knowledge_gap", e.gapId(), e.lessonId());
            p.put("learning_objective_id", e.learningObjectiveId().toString());
            creationService.createForRecipients(NotificationType.KNOWLEDGE_GAP_OPENED,
                    "KNOWLEDGE_GAP_OPENED", e.gapId(), List.of(e.studentId()),
                    "Phát hiện phần cần ôn tập", "Hệ thống phát hiện một mục tiêu bài học bạn cần củng cố thêm.", p);
        } catch (Exception ex) {
            LOG.error("Tạo notification KNOWLEDGE_GAP_OPENED thất bại: gap={}", e.gapId(), ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onRecommendationCreated(RecommendationCreatedEvent e) {
        try {
            Map<String, Object> p = payload(e.courseId(), "recommendation", e.recommendationId(), e.lessonId());
            p.put("recommendation_type", e.type());
            creationService.createForRecipients(NotificationType.RECOMMENDATION_NEW,
                    "RECOMMENDATION_NEW", e.recommendationId(), List.of(e.studentId()),
                    "Gợi ý học tập mới", "Bạn có một gợi ý học tập mới.", p);
        } catch (Exception ex) {
            LOG.error("Tạo notification RECOMMENDATION_NEW thất bại: recommendation={}", e.recommendationId(), ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAiReviewPending(AiReviewPendingEvent e) {
        try {
            Map<String, Object> p = payload(e.courseId(), "ai_generation_request", e.requestId(), e.lessonId());
            p.put("request_type", e.requestType());
            creationService.createForRecipients(NotificationType.AI_REVIEW_PENDING,
                    "AI_REVIEW_PENDING", e.requestId(), List.of(e.teacherId()),
                    "Nội dung AI đang chờ bạn duyệt",
                    typeLabel(e.requestType()) + " do AI sinh đã sẵn sàng, vui lòng xem và duyệt.", p);
        } catch (Exception ex) {
            LOG.error("Tạo notification AI_REVIEW_PENDING thất bại: request={}", e.requestId(), ex);
        }
    }

    private static String typeLabel(String requestType) {
        if (requestType == null) {
            return "Nội dung";
        }
        return switch (requestType) {
            case "LESSON_PLAN" -> "Giáo án";
            case "QUESTION_SET" -> "Bộ câu hỏi";
            case "ASSESSMENT_MATRIX" -> "Ma trận đề";
            default -> "Nội dung";
        };
    }

    private static Map<String, Object> payload(UUID courseId, String entityType, UUID entityId, UUID lessonId) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("course_id", courseId.toString());
        p.put("entity_type", entityType);
        p.put("entity_id", entityId.toString());
        if (lessonId != null) {
            p.put("lesson_id", lessonId.toString());
        }
        return p;
    }

    private static String fmt(BigDecimal v) {
        return v == null ? "—" : v.stripTrailingZeros().toPlainString();
    }
}
