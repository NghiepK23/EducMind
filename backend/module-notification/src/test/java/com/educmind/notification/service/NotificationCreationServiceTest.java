package com.educmind.notification.service;

import com.educmind.notification.entity.Notification;
import com.educmind.notification.repository.NotificationPreferenceRepository;
import com.educmind.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationCreationServiceTest {

    private static final String TYPE = "ASSIGNMENT_PUBLISHED";

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationPreferenceRepository preferenceRepository;
    @InjectMocks
    private NotificationCreationService service;
    @Captor
    private ArgumentCaptor<List<Notification>> savedCaptor;

    private int create(List<UUID> recipients) {
        return service.createForRecipients(TYPE, TYPE, UUID.randomUUID(), recipients,
                "Tiêu đề", "Nội dung", Map.of("k", "v"));
    }

    @Test
    void emptyRecipients_returnsZero_andTouchesNothing() {
        assertEquals(0, create(List.of()));
        verifyNoInteractions(notificationRepository, preferenceRepository);
    }

    @Test
    void nullRecipients_returnsZero_andTouchesNothing() {
        assertEquals(0, create(null));
        verifyNoInteractions(notificationRepository, preferenceRepository);
    }

    @Test
    void duplicateRecipients_createOnlyOneNotification() {
        UUID a = UUID.randomUUID();
        when(preferenceRepository.findDisabledUserIds(eq(TYPE), any())).thenReturn(List.of());

        assertEquals(1, create(List.of(a, a)));

        verify(notificationRepository).saveAll(savedCaptor.capture());
        assertEquals(1, savedCaptor.getValue().size());
        assertEquals(a, savedCaptor.getValue().get(0).getRecipientId());
    }

    @Test
    void disabledRecipient_isSkipped_othersAreSaved() {
        UUID enabled = UUID.randomUUID();
        UUID disabled = UUID.randomUUID();
        when(preferenceRepository.findDisabledUserIds(eq(TYPE), any())).thenReturn(List.of(disabled));

        assertEquals(1, create(List.of(enabled, disabled)));

        verify(notificationRepository).saveAll(savedCaptor.capture());
        Set<UUID> savedIds = Set.of(savedCaptor.getValue().get(0).getRecipientId());
        assertEquals(Set.of(enabled), savedIds);
        assertTrue(savedCaptor.getValue().stream().noneMatch(n -> n.getRecipientId().equals(disabled)));
    }

    @Test
    void allRecipientsDisabled_returnsZero() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(preferenceRepository.findDisabledUserIds(eq(TYPE), any())).thenReturn(List.of(a, b));

        assertEquals(0, create(List.of(a, b)));

        verify(notificationRepository).saveAll(savedCaptor.capture());
        assertTrue(savedCaptor.getValue().isEmpty());
    }

    @Test
    void createdNotification_carriesGivenFields() {
        UUID a = UUID.randomUUID();
        UUID source = UUID.randomUUID();
        when(preferenceRepository.findDisabledUserIds(eq(TYPE), any())).thenReturn(List.of());

        service.createForRecipients(TYPE, "SRC_EVENT", source, List.of(a), "T", "B", Map.of("k", "v"));

        verify(notificationRepository).saveAll(savedCaptor.capture());
        Notification n = savedCaptor.getValue().get(0);
        assertEquals(TYPE, n.getType());
        assertEquals("T", n.getTitle());
        assertEquals("B", n.getBody());
        assertEquals("SRC_EVENT", n.getSourceEventType());
        assertEquals(source, n.getSourceEntityId());
        assertEquals(Notification.STATUS_UNREAD, n.getStatus());
    }
}
