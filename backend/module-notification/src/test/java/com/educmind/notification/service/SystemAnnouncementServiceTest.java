package com.educmind.notification.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.ClassAccessPort;
import com.educmind.common.port.UserAnnouncementPort;
import com.educmind.notification.dto.AnnouncementRequest;
import com.educmind.notification.dto.AnnouncementResponse;
import com.educmind.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemAnnouncementServiceTest {

    @Mock
    private UserAnnouncementPort userAnnouncementPort;
    @Mock
    private ClassAccessPort classAccessPort;
    @Mock
    private NotificationCreationService notificationCreationService;
    @Mock
    private NotificationRepository notificationRepository;
    @Captor
    private ArgumentCaptor<Collection<UUID>> recipientsCaptor;
    @Captor
    private ArgumentCaptor<Map<String, Object>> payloadCaptor;
    @Captor
    private ArgumentCaptor<UUID> sourceIdCaptor;

    private SystemAnnouncementService service;

    private final UUID actorId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID classId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SystemAnnouncementService(userAnnouncementPort, classAccessPort,
                notificationCreationService, notificationRepository, 10);
    }

    private AnnouncementRequest request() {
        AnnouncementRequest request = new AnnouncementRequest();
        request.setTitle("Bảo trì hệ thống");
        request.setBody("Hệ thống bảo trì lúc 22:00.");
        return request;
    }

    private void stubCreate(int delivered) {
        when(notificationCreationService.createForRecipients(eq("SYSTEM_ANNOUNCEMENT"),
                eq("SYSTEM_ANNOUNCEMENT"), any(), anyCollection(), anyString(), anyString(), any()))
                .thenReturn(delivered);
    }

    private void verifyNoCreate() {
        verify(notificationCreationService, never()).createForRecipients(anyString(), anyString(), any(),
                anyCollection(), anyString(), anyString(), any());
    }

    @Test
    void schoolAdmin_broadcastsOnlyToActiveUsersInOwnSchool() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(userAnnouncementPort.findSchoolIdByUserId(actorId)).thenReturn(Optional.of(schoolId));
        when(userAnnouncementPort.findActiveUserIdsBySchoolId(schoolId)).thenReturn(List.of(first, second));
        stubCreate(2);

        AnnouncementResponse response = service.announce(actorId, false, request());

        assertEquals(2, response.deliveredCount());
        verify(notificationCreationService).createForRecipients(eq("SYSTEM_ANNOUNCEMENT"),
                eq("SYSTEM_ANNOUNCEMENT"), sourceIdCaptor.capture(), recipientsCaptor.capture(),
                eq("Bảo trì hệ thống"), eq("Hệ thống bảo trì lúc 22:00."), payloadCaptor.capture());
        assertEquals(List.of(first, second), List.copyOf(recipientsCaptor.getValue()));
        assertEquals("system_announcement", payloadCaptor.getValue().get("entity_type"));
        assertEquals(response.announcementId(), sourceIdCaptor.getValue());
        assertEquals(response.announcementId().toString(), payloadCaptor.getValue().get("entity_id"));
        assertEquals(actorId.toString(), payloadCaptor.getValue().get("announced_by"));
        assertEquals("SCHOOL_ACTIVE", payloadCaptor.getValue().get("audience"));
    }

    @Test
    void superAdmin_withoutScope_broadcastsToAllActiveUsersWithoutSchoolLookup() {
        UUID recipient = UUID.randomUUID();
        when(userAnnouncementPort.findAllActiveUserIds()).thenReturn(List.of(recipient));
        stubCreate(1);

        AnnouncementResponse response = service.announce(actorId, true, request());

        assertEquals(1, response.deliveredCount());
        verify(userAnnouncementPort, never()).findSchoolIdByUserId(any());
        verify(notificationCreationService).createForRecipients(eq("SYSTEM_ANNOUNCEMENT"),
                eq("SYSTEM_ANNOUNCEMENT"), sourceIdCaptor.capture(), recipientsCaptor.capture(),
                anyString(), anyString(), payloadCaptor.capture());
        assertEquals(List.of(recipient), List.copyOf(recipientsCaptor.getValue()));
        assertEquals("ALL_ACTIVE", payloadCaptor.getValue().get("audience"));
    }

    @Test
    void schoolAdminWithoutSchool_isRejected() {
        when(userAnnouncementPort.findSchoolIdByUserId(actorId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.announce(actorId, false, request()));

        assertEquals("SCHOOL_SCOPE_REQUIRED", ex.getErrorCode());
        verifyNoCreate();
    }

    @Test
    void classScope_sendsToActiveMembersAndOwnerOfClassInOwnSchool() {
        UUID member = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        when(userAnnouncementPort.findSchoolIdByUserId(actorId)).thenReturn(Optional.of(schoolId));
        when(classAccessPort.findSchoolIdOfActiveClass(classId)).thenReturn(Optional.of(schoolId));
        when(classAccessPort.findActiveMemberUserIds(classId)).thenReturn(List.of(owner, member));
        when(userAnnouncementPort.filterActiveUserIds(List.of(owner, member))).thenReturn(List.of(owner, member));
        stubCreate(2);
        AnnouncementRequest req = request();
        req.setScope("class");
        req.setClassId(classId);

        AnnouncementResponse response = service.announce(actorId, false, req);

        assertEquals(2, response.deliveredCount());
        verify(notificationCreationService).createForRecipients(anyString(), anyString(), any(),
                recipientsCaptor.capture(), anyString(), anyString(), payloadCaptor.capture());
        assertEquals(List.of(owner, member), List.copyOf(recipientsCaptor.getValue()));
        assertEquals("CLASS_ACTIVE", payloadCaptor.getValue().get("audience"));
        assertEquals(classId.toString(), payloadCaptor.getValue().get("class_id"));
    }

    @Test
    void classScope_missingClassId_is422() {
        when(userAnnouncementPort.findSchoolIdByUserId(actorId)).thenReturn(Optional.of(schoolId));
        AnnouncementRequest req = request();
        req.setScope("CLASS");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.announce(actorId, false, req));

        assertEquals("INVALID_ANNOUNCEMENT_SCOPE", ex.getErrorCode());
        verifyNoCreate();
    }

    @Test
    void unknownScope_is422() {
        AnnouncementRequest req = request();
        req.setScope("GLOBAL");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.announce(actorId, true, req));

        assertEquals("INVALID_ANNOUNCEMENT_SCOPE", ex.getErrorCode());
        verifyNoCreate();
    }

    @Test
    void classScope_classNotFound_is404() {
        when(userAnnouncementPort.findSchoolIdByUserId(actorId)).thenReturn(Optional.of(schoolId));
        when(classAccessPort.findSchoolIdOfActiveClass(classId)).thenReturn(Optional.empty());
        AnnouncementRequest req = request();
        req.setScope("CLASS");
        req.setClassId(classId);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.announce(actorId, false, req));

        assertEquals("CLASS_NOT_FOUND", ex.getErrorCode());
        verifyNoCreate();
    }

    @Test
    void classScope_classInOtherSchool_isForbiddenForSchoolAdmin() {
        when(userAnnouncementPort.findSchoolIdByUserId(actorId)).thenReturn(Optional.of(schoolId));
        when(classAccessPort.findSchoolIdOfActiveClass(classId)).thenReturn(Optional.of(UUID.randomUUID()));
        AnnouncementRequest req = request();
        req.setScope("CLASS");
        req.setClassId(classId);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.announce(actorId, false, req));

        assertEquals("FORBIDDEN_CLASS_SCOPE", ex.getErrorCode());
        verifyNoCreate();
    }

    @Test
    void classScope_superAdmin_canSendToAnySchoolClass() {
        UUID member = UUID.randomUUID();
        when(classAccessPort.findSchoolIdOfActiveClass(classId)).thenReturn(Optional.of(UUID.randomUUID()));
        when(classAccessPort.findActiveMemberUserIds(classId)).thenReturn(List.of(member));
        when(userAnnouncementPort.filterActiveUserIds(List.of(member))).thenReturn(List.of(member));
        stubCreate(1);
        AnnouncementRequest req = request();
        req.setScope("CLASS");
        req.setClassId(classId);

        assertEquals(1, service.announce(actorId, true, req).deliveredCount());
        verify(userAnnouncementPort, never()).findSchoolIdByUserId(any());
    }

    @Test
    void schoolScope_superAdminWithoutSchoolId_is422() {
        AnnouncementRequest req = request();
        req.setScope("SCHOOL");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.announce(actorId, true, req));

        assertEquals("INVALID_ANNOUNCEMENT_SCOPE", ex.getErrorCode());
    }

    @Test
    void schoolScope_schoolAdminTargetingOtherSchool_isForbidden() {
        when(userAnnouncementPort.findSchoolIdByUserId(actorId)).thenReturn(Optional.of(schoolId));
        AnnouncementRequest req = request();
        req.setScope("SCHOOL");
        req.setSchoolId(UUID.randomUUID());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.announce(actorId, false, req));

        assertEquals("FORBIDDEN_SCHOOL_SCOPE", ex.getErrorCode());
        verifyNoCreate();
    }

    @Test
    void quotaExceeded_is429AndNothingIsSent() {
        when(userAnnouncementPort.findSchoolIdByUserId(actorId)).thenReturn(Optional.of(schoolId));
        when(notificationRepository.countRecentAnnouncementsByActor(eq(actorId.toString()), any())).thenReturn(10L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.announce(actorId, false, request()));

        assertEquals("ANNOUNCEMENT_RATE_LIMITED", ex.getErrorCode());
        verifyNoCreate();
    }
}
