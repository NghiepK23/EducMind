package com.educmind.rag.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.ResourceProcessingPort;
import com.educmind.rag.dto.BackfillResponse;
import com.educmind.rag.pipeline.RagWorkerExecutor;
import com.educmind.rag.pipeline.ResourceProcessingPipeline;
import com.educmind.rag.store.BackfillCandidateStore;
import com.educmind.rag.store.ChunkStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagBackfillServiceTest {

    private static final Duration STALE = Duration.ofMinutes(30);

    private BackfillCandidateStore candidates;
    private ResourceProcessingPort port;
    private ChunkStore chunkStore;
    private RagWorkerExecutor executor;
    private ResourceProcessingPipeline pipeline;
    private RagBackfillService service;

    @BeforeEach
    void setUp() {
        candidates = mock(BackfillCandidateStore.class);
        port = mock(ResourceProcessingPort.class);
        chunkStore = mock(ChunkStore.class);
        executor = mock(RagWorkerExecutor.class);
        pipeline = mock(ResourceProcessingPipeline.class);
        service = new RagBackfillService(candidates, port, chunkStore, executor, pipeline, 30, 0);
    }

    private Runnable capturedTask() {
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).submit(task.capture());
        return task.getValue();
    }

    @Test
    void noCandidatesQueuesNothingAndDoesNotLock() {
        when(candidates.findCandidateIds(30)).thenReturn(List.of());

        assertEquals(0, service.start().queued());
        assertEquals(0, service.start().queued());
        verify(executor, never()).submit(any());
    }

    @Test
    void processesOnlyEligibleResourcesInOrder() {
        UUID noChunks = UUID.randomUUID();
        UUID hasChunks = UUID.randomUUID();
        UUID stuck = UUID.randomUUID();
        UUID lostRace = UUID.randomUUID();
        UUID deleted = UUID.randomUUID();
        when(candidates.findCandidateIds(30)).thenReturn(List.of(noChunks, hasChunks, stuck, lostRace, deleted));

        when(port.findProcessingStatus(noChunks)).thenReturn(Optional.of("READY"));
        when(chunkStore.countByResourceId(noChunks)).thenReturn(0);
        when(port.tryBeginReprocess(noChunks, STALE)).thenReturn(true);

        when(port.findProcessingStatus(hasChunks)).thenReturn(Optional.of("READY"));
        when(chunkStore.countByResourceId(hasChunks)).thenReturn(3);

        when(port.findProcessingStatus(stuck)).thenReturn(Optional.of("PROCESSING"));
        when(port.tryBeginReprocess(stuck, STALE)).thenReturn(true);

        when(port.findProcessingStatus(lostRace)).thenReturn(Optional.of("PROCESSING"));
        when(port.tryBeginReprocess(lostRace, STALE)).thenReturn(false);

        when(port.findProcessingStatus(deleted)).thenReturn(Optional.empty());

        BackfillResponse response = service.start();
        assertEquals(5, response.queued());
        capturedTask().run();

        InOrder order = inOrder(pipeline);
        order.verify(pipeline).process(noChunks);
        order.verify(pipeline).process(stuck);
        verify(pipeline, never()).process(hasChunks);
        verify(pipeline, never()).process(lostRace);
        verify(pipeline, never()).process(deleted);
    }

    @Test
    void failureOfOneResourceDoesNotStopTheRest() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(candidates.findCandidateIds(30)).thenReturn(List.of(first, second));
        for (UUID id : List.of(first, second)) {
            when(port.findProcessingStatus(id)).thenReturn(Optional.of("READY"));
            when(chunkStore.countByResourceId(id)).thenReturn(0);
            when(port.tryBeginReprocess(id, STALE)).thenReturn(true);
        }
        doThrow(new IllegalStateException("boom")).when(pipeline).process(first);

        service.start();
        capturedTask().run();

        verify(pipeline).process(first);
        verify(pipeline).process(second);
    }

    @Test
    void secondStartWhileRunningIsConflictThenAllowedAfterFinish() {
        UUID id = UUID.randomUUID();
        when(candidates.findCandidateIds(30)).thenReturn(List.of(id));
        when(port.findProcessingStatus(id)).thenReturn(Optional.of("READY"));
        when(chunkStore.countByResourceId(id)).thenReturn(0);
        when(port.tryBeginReprocess(id, STALE)).thenReturn(true);

        service.start();
        BusinessException e = assertThrows(BusinessException.class, () -> service.start());
        assertEquals("BACKFILL_ALREADY_RUNNING", e.getErrorCode());

        capturedTask().run();

        assertEquals(1, service.start().queued());
        verify(executor, times(2)).submit(any());
    }

    @Test
    void rejectedWorkerResetsLockAndReturnsUnavailable() {
        when(candidates.findCandidateIds(30)).thenReturn(List.of(UUID.randomUUID()));
        doThrow(new RejectedExecutionException()).when(executor).submit(any());

        BusinessException e = assertThrows(BusinessException.class, () -> service.start());
        assertEquals("RAG_WORKER_UNAVAILABLE", e.getErrorCode());

        doThrow(new RejectedExecutionException()).when(executor).submit(any());
        BusinessException again = assertThrows(BusinessException.class, () -> service.start());
        assertEquals("RAG_WORKER_UNAVAILABLE", again.getErrorCode());
    }
}
