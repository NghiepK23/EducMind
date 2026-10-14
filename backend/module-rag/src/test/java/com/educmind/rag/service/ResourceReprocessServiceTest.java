package com.educmind.rag.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.CourseAccessPort;
import com.educmind.common.port.ResourceProcessingPort;
import com.educmind.common.port.UserLookupPort;
import com.educmind.rag.dto.ReprocessResponse;
import com.educmind.rag.pipeline.RagWorkerExecutor;
import com.educmind.rag.pipeline.ResourceProcessingPipeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceReprocessServiceTest {

    private static final String PDF = "application/pdf";

    private final UUID actorId = UUID.randomUUID();
    private final UUID resourceId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();

    private ResourceProcessingPort port;
    private CourseAccessPort access;
    private UserLookupPort users;
    private RagWorkerExecutor executor;
    private ResourceProcessingPipeline pipeline;
    private ResourceReprocessService service;

    @BeforeEach
    void setUp() {
        port = mock(ResourceProcessingPort.class);
        access = mock(CourseAccessPort.class);
        users = mock(UserLookupPort.class);
        executor = mock(RagWorkerExecutor.class);
        pipeline = mock(ResourceProcessingPipeline.class);
        service = new ResourceReprocessService(port, access, users, executor, pipeline, 30);
    }

    private void actorIs(String role) {
        when(users.findUserSummary(actorId))
                .thenReturn(Optional.of(new UserLookupPort.UserSummary(actorId, role, UUID.randomUUID(), true)));
    }

    private ResourceProcessingPort.ResourceInfo info(String mime) {
        return new ResourceProcessingPort.ResourceInfo(resourceId, courseId, null, "bai.pdf", mime, 1000L);
    }

    private static String codeOf(BusinessException e) {
        return e.getErrorCode();
    }

    @Test
    void teacherWithAccessStartsReprocessAndWorkerRunsPipeline() {
        actorIs("TEACHER");
        when(port.findResource(resourceId)).thenReturn(Optional.of(info(PDF)));
        when(access.isOwnerOrCoTeacher(actorId, courseId)).thenReturn(true);
        when(port.tryBeginReprocess(resourceId, Duration.ofMinutes(30))).thenReturn(true);

        ReprocessResponse response = service.reprocess(actorId, resourceId);

        assertEquals("PROCESSING", response.processingStatus());
        assertEquals(resourceId, response.resourceId());
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).submit(task.capture());
        task.getValue().run();
        verify(pipeline).process(resourceId);
    }

    @Test
    void teacherWithoutAccessIsForbidden() {
        actorIs("TEACHER");
        when(port.findResource(resourceId)).thenReturn(Optional.of(info(PDF)));
        when(access.isOwnerOrCoTeacher(actorId, courseId)).thenReturn(false);

        BusinessException e = assertThrows(BusinessException.class, () -> service.reprocess(actorId, resourceId));

        assertEquals("REPROCESS_NOT_ALLOWED", codeOf(e));
        verify(port, never()).tryBeginReprocess(any(), any());
        verify(executor, never()).submit(any());
    }

    @Test
    void schoolAdminIsForbidden() {
        actorIs("SCHOOL_ADMIN");
        when(port.findResource(resourceId)).thenReturn(Optional.of(info(PDF)));

        BusinessException e = assertThrows(BusinessException.class, () -> service.reprocess(actorId, resourceId));

        assertEquals("REPROCESS_NOT_ALLOWED", codeOf(e));
        verify(port, never()).tryBeginReprocess(any(), any());
    }

    @Test
    void unsupportedFileTypeIsRejected() {
        actorIs("TEACHER");
        when(port.findResource(resourceId)).thenReturn(Optional.of(info("image/png")));
        when(access.isOwnerOrCoTeacher(actorId, courseId)).thenReturn(true);

        BusinessException e = assertThrows(BusinessException.class, () -> service.reprocess(actorId, resourceId));

        assertEquals("UNSUPPORTED_CONTENT_TYPE", codeOf(e));
        verify(port, never()).tryBeginReprocess(any(), any());
    }

    @Test
    void alreadyProcessingIsConflict() {
        actorIs("TEACHER");
        when(port.findResource(resourceId)).thenReturn(Optional.of(info(PDF)));
        when(access.isOwnerOrCoTeacher(actorId, courseId)).thenReturn(true);
        when(port.tryBeginReprocess(resourceId, Duration.ofMinutes(30))).thenReturn(false);

        BusinessException e = assertThrows(BusinessException.class, () -> service.reprocess(actorId, resourceId));

        assertEquals("RESOURCE_ALREADY_PROCESSING", codeOf(e));
        verify(executor, never()).submit(any());
    }

    @Test
    void resourceDeletedBetweenChecksIsNotFound() {
        actorIs("TEACHER");
        when(port.findResource(resourceId)).thenReturn(Optional.of(info(PDF)), Optional.empty());
        when(access.isOwnerOrCoTeacher(actorId, courseId)).thenReturn(true);
        when(port.tryBeginReprocess(resourceId, Duration.ofMinutes(30))).thenReturn(false);

        BusinessException e = assertThrows(BusinessException.class, () -> service.reprocess(actorId, resourceId));

        assertEquals("RESOURCE_NOT_FOUND", codeOf(e));
    }

    @Test
    void missingResourceIsNotFound() {
        actorIs("TEACHER");
        when(port.findResource(resourceId)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class, () -> service.reprocess(actorId, resourceId));

        assertEquals("RESOURCE_NOT_FOUND", codeOf(e));
    }

    @Test
    void rejectedWorkerMarksFailedAndReturnsUnavailable() {
        actorIs("TEACHER");
        when(port.findResource(resourceId)).thenReturn(Optional.of(info(PDF)));
        when(access.isOwnerOrCoTeacher(actorId, courseId)).thenReturn(true);
        when(port.tryBeginReprocess(resourceId, Duration.ofMinutes(30))).thenReturn(true);
        doThrow(new RejectedExecutionException()).when(executor).submit(any());

        BusinessException e = assertThrows(BusinessException.class, () -> service.reprocess(actorId, resourceId));

        assertEquals("RAG_WORKER_UNAVAILABLE", codeOf(e));
        verify(port).markFailed(resourceId);
    }
}
