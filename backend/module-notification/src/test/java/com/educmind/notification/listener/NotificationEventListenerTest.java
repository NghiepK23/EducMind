package com.educmind.notification.listener;

import com.educmind.common.event.AssessmentGradedEvent;
import com.educmind.common.event.AssignmentGradedEvent;
import com.educmind.common.event.AssignmentPublishedEvent;
import com.educmind.common.event.KnowledgeGapOpenedEvent;
import com.educmind.common.event.RecommendationCreatedEvent;
import com.educmind.common.port.CourseAccessPort;
import com.educmind.notification.service.NotificationCreationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationCreationService creationService;
    @Mock
    private CourseAccessPort courseAccessPort;
    @InjectMocks
    private NotificationEventListener listener;

    @Captor
    private ArgumentCaptor<Map<String, Object>> payloadCaptor;
    @Captor
    private ArgumentCaptor<java.util.Collection<UUID>> recipientsCaptor;
    @Captor
    private ArgumentCaptor<String> bodyCaptor;

    @Test
    void assignmentPublished_notifiesAllActiveStudentsOfCourse() {
        UUID assignmentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        List<UUID> students = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(courseAccessPort.findActiveStudentIdsByCourse(courseId)).thenReturn(students);

        listener.onAssignmentPublished(new AssignmentPublishedEvent(assignmentId, courseId, "Bài 1"));

        verify(creationService).createForRecipients(eq("ASSIGNMENT_PUBLISHED"), eq("ASSIGNMENT_PUBLISHED"),
                eq(assignmentId), recipientsCaptor.capture(), eq("Bài tập mới"), bodyCaptor.capture(),
                payloadCaptor.capture());
        assertEquals(students, List.copyOf(recipientsCaptor.getValue()));
        assertEquals("Bài tập \"Bài 1\" vừa được giao.", bodyCaptor.getValue());
        assertEquals(courseId.toString(), payloadCaptor.getValue().get("course_id"));
        assertEquals("assignment", payloadCaptor.getValue().get("entity_type"));
        assertEquals(assignmentId.toString(), payloadCaptor.getValue().get("entity_id"));
        assertFalse(payloadCaptor.getValue().containsKey("lesson_id"));
    }

    @Test
    void assignmentGraded_notifiesOnlyThatStudent_withScoreInBody() {
        UUID submissionId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        listener.onAssignmentGraded(new AssignmentGradedEvent(submissionId, assignmentId, courseId,
                studentId, "Bài 2", new BigDecimal("8.50"), new BigDecimal("10.00")));

        verify(creationService).createForRecipients(eq("ASSIGNMENT_GRADED"), eq("ASSIGNMENT_GRADED"),
                eq(submissionId), recipientsCaptor.capture(), anyString(), bodyCaptor.capture(),
                payloadCaptor.capture());
        assertEquals(List.of(studentId), List.copyOf(recipientsCaptor.getValue()));
        assertEquals("Bài \"Bài 2\": 8.5/10 điểm.", bodyCaptor.getValue());
        assertEquals(assignmentId.toString(), payloadCaptor.getValue().get("entity_id"));
    }

    @Test
    void assessmentGraded_withScore_includesTotalInBody() {
        UUID attemptId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        listener.onAssessmentGraded(new AssessmentGradedEvent(attemptId, UUID.randomUUID(),
                UUID.randomUUID(), studentId, new BigDecimal("7.0")));

        verify(creationService).createForRecipients(eq("ASSESSMENT_GRADED"), eq("ASSESSMENT_GRADED"),
                eq(attemptId), recipientsCaptor.capture(), anyString(), bodyCaptor.capture(),
                payloadCaptor.capture());
        assertEquals(List.of(studentId), List.copyOf(recipientsCaptor.getValue()));
        assertEquals("Kết quả bài kiểm tra của bạn đã có. Tổng điểm: 7.", bodyCaptor.getValue());
        assertEquals("assessment_attempt", payloadCaptor.getValue().get("entity_type"));
    }

    @Test
    void assessmentGraded_withoutScore_omitsTotalFromBody() {
        listener.onAssessmentGraded(new AssessmentGradedEvent(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), null));

        verify(creationService).createForRecipients(eq("ASSESSMENT_GRADED"), anyString(), any(),
                anyCollection(), anyString(), bodyCaptor.capture(), any());
        assertEquals("Kết quả bài kiểm tra của bạn đã có.", bodyCaptor.getValue());
    }

    @Test
    void knowledgeGapOpened_payloadHasLessonAndObjective_notifiesStudentOnly() {
        UUID gapId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID objectiveId = UUID.randomUUID();

        listener.onKnowledgeGapOpened(new KnowledgeGapOpenedEvent(gapId, studentId, courseId,
                lessonId, objectiveId));

        verify(creationService).createForRecipients(eq("KNOWLEDGE_GAP_OPENED"), eq("KNOWLEDGE_GAP_OPENED"),
                eq(gapId), recipientsCaptor.capture(), anyString(), anyString(), payloadCaptor.capture());
        assertEquals(List.of(studentId), List.copyOf(recipientsCaptor.getValue()));
        assertEquals("knowledge_gap", payloadCaptor.getValue().get("entity_type"));
        assertEquals(lessonId.toString(), payloadCaptor.getValue().get("lesson_id"));
        assertEquals(objectiveId.toString(), payloadCaptor.getValue().get("learning_objective_id"));
    }

    @Test
    void recommendationCreated_payloadHasRecommendationType() {
        UUID recId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        listener.onRecommendationCreated(new RecommendationCreatedEvent(recId, studentId,
                UUID.randomUUID(), lessonId, UUID.randomUUID(), "REVIEW_CONTENT"));

        verify(creationService).createForRecipients(eq("RECOMMENDATION_NEW"), eq("RECOMMENDATION_NEW"),
                eq(recId), recipientsCaptor.capture(), anyString(), anyString(), payloadCaptor.capture());
        assertEquals(List.of(studentId), List.copyOf(recipientsCaptor.getValue()));
        assertEquals("REVIEW_CONTENT", payloadCaptor.getValue().get("recommendation_type"));
        assertEquals(lessonId.toString(), payloadCaptor.getValue().get("lesson_id"));
    }

    @Test
    void creationFailure_isSwallowed_notPropagated() {
        doThrow(new RuntimeException("DB down")).when(creationService)
                .createForRecipients(anyString(), anyString(), any(), anyCollection(), anyString(),
                        anyString(), any());

        assertDoesNotThrow(() -> listener.onAssessmentGraded(new AssessmentGradedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.ONE)));
    }

    @Test
    void portFailure_onPublish_isSwallowed() {
        UUID courseId = UUID.randomUUID();
        when(courseAccessPort.findActiveStudentIdsByCourse(courseId))
                .thenThrow(new RuntimeException("port error"));

        assertDoesNotThrow(() -> listener.onAssignmentPublished(
                new AssignmentPublishedEvent(UUID.randomUUID(), courseId, "X")));
    }
}
