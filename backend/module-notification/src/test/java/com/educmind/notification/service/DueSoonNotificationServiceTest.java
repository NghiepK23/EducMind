package com.educmind.notification.service;

import com.educmind.common.port.AssignmentDeadlinePort;
import com.educmind.common.port.AssignmentDeadlinePort.DueAssignment;
import com.educmind.common.port.CourseAccessPort;
import com.educmind.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DueSoonNotificationServiceTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-05T10:00:00Z");

    @Mock
    private AssignmentDeadlinePort deadlinePort;
    @Mock
    private CourseAccessPort courseAccessPort;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationCreationService creationService;
    @InjectMocks
    private DueSoonNotificationService service;
    @Captor
    private ArgumentCaptor<Collection<UUID>> recipientsCaptor;

    private final UUID courseId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();

    private DueAssignment due() {
        return new DueAssignment(assignmentId, courseId, "Bài 1", NOW.plusHours(5));
    }

    private void stubDue() {
        when(deadlinePort.findPublishedDueBetween(NOW, NOW.plusHours(24))).thenReturn(List.of(due()));
    }

    @Test
    void noDueAssignments_createsNothing() {
        when(deadlinePort.findPublishedDueBetween(NOW, NOW.plusHours(24))).thenReturn(List.of());

        assertEquals(0, service.notifyDueSoon(NOW));
        verify(creationService, never()).createForRecipients(anyString(), anyString(), any(),
                anyCollection(), anyString(), anyString(), any());
    }

    @Test
    void excludesSubmittedAndAlreadyNotified() {
        UUID notSubmitted = UUID.randomUUID();
        UUID submitted = UUID.randomUUID();
        UUID notified = UUID.randomUUID();
        stubDue();
        when(courseAccessPort.findActiveStudentIdsByCourse(courseId))
                .thenReturn(List.of(notSubmitted, submitted, notified));
        when(deadlinePort.findSubmittedStudentIds(assignmentId)).thenReturn(Set.of(submitted));
        when(notificationRepository.findRecipientIdsBySource("ASSIGNMENT_DUE_SOON", assignmentId))
                .thenReturn(List.of(notified));
        when(creationService.createForRecipients(eq("ASSIGNMENT_DUE_SOON"), eq("ASSIGNMENT_DUE_SOON"),
                eq(assignmentId), anyCollection(), anyString(), anyString(), any())).thenReturn(1);

        assertEquals(1, service.notifyDueSoon(NOW));

        verify(creationService).createForRecipients(eq("ASSIGNMENT_DUE_SOON"), eq("ASSIGNMENT_DUE_SOON"),
                eq(assignmentId), recipientsCaptor.capture(), anyString(), anyString(), any());
        assertEquals(List.of(notSubmitted), List.copyOf(recipientsCaptor.getValue()));
    }

    @Test
    void everyoneSubmitted_createsNothing() {
        UUID s = UUID.randomUUID();
        stubDue();
        when(courseAccessPort.findActiveStudentIdsByCourse(courseId)).thenReturn(List.of(s));
        when(deadlinePort.findSubmittedStudentIds(assignmentId)).thenReturn(Set.of(s));
        when(notificationRepository.findRecipientIdsBySource("ASSIGNMENT_DUE_SOON", assignmentId))
                .thenReturn(List.of());

        assertEquals(0, service.notifyDueSoon(NOW));
        verify(creationService, never()).createForRecipients(anyString(), anyString(), any(),
                anyCollection(), anyString(), anyString(), any());
    }

    @Test
    void secondRun_afterFirstNotified_createsNothing() {
        UUID s = UUID.randomUUID();
        stubDue();
        when(courseAccessPort.findActiveStudentIdsByCourse(courseId)).thenReturn(List.of(s));
        when(deadlinePort.findSubmittedStudentIds(assignmentId)).thenReturn(Set.of());
        when(notificationRepository.findRecipientIdsBySource("ASSIGNMENT_DUE_SOON", assignmentId))
                .thenReturn(List.of(s));

        assertEquals(0, service.notifyDueSoon(NOW));
        verify(creationService, never()).createForRecipients(anyString(), anyString(), any(),
                anyCollection(), anyString(), anyString(), any());
    }

    @Test
    void payloadHasDueDateAndEntity() {
        UUID s = UUID.randomUUID();
        stubDue();
        when(courseAccessPort.findActiveStudentIdsByCourse(courseId)).thenReturn(List.of(s));
        when(deadlinePort.findSubmittedStudentIds(assignmentId)).thenReturn(Set.of());
        when(notificationRepository.findRecipientIdsBySource("ASSIGNMENT_DUE_SOON", assignmentId))
                .thenReturn(List.of());

        service.notifyDueSoon(NOW);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(creationService).createForRecipients(anyString(), anyString(), any(), anyCollection(),
                anyString(), anyString(), payload.capture());
        assertEquals(assignmentId.toString(), payload.getValue().get("entity_id"));
        assertEquals("assignment", payload.getValue().get("entity_type"));
        assertEquals(NOW.plusHours(5).toString(), payload.getValue().get("due_date"));
    }
}
