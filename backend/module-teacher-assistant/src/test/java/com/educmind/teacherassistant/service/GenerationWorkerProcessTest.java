package com.educmind.teacherassistant.service;

import com.educmind.common.event.AiReviewPendingEvent;
import com.educmind.common.port.AIProvider;
import com.educmind.common.port.LessonAccessPort;
import com.educmind.common.port.RetrievalPort;
import com.educmind.teacherassistant.entity.AiGenerationOutput;
import com.educmind.teacherassistant.entity.AiGenerationRequest;
import com.educmind.teacherassistant.entity.AiObservabilityLog;
import com.educmind.teacherassistant.repository.AiGenerationOutputRepository;
import com.educmind.teacherassistant.repository.AiGenerationRequestRepository;
import com.educmind.teacherassistant.repository.AiObservabilityLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GenerationWorkerProcessTest {

    private final UUID requestId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();
    private final UUID lessonId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final UUID objectiveId = UUID.randomUUID();

    private AiGenerationRequestRepository requestRepository;
    private AiGenerationOutputRepository outputRepository;
    private AiObservabilityLogRepository logRepository;
    private AIProvider aiProvider;
    private LessonAccessPort lessonAccessPort;
    private ApplicationEventPublisher eventPublisher;
    private RetrievalPort retrievalPort;
    private GenerationWorker worker;

    @BeforeEach
    void setUp() {
        requestRepository = mock(AiGenerationRequestRepository.class);
        outputRepository = mock(AiGenerationOutputRepository.class);
        logRepository = mock(AiObservabilityLogRepository.class);
        aiProvider = mock(AIProvider.class);
        lessonAccessPort = mock(LessonAccessPort.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        retrievalPort = mock(RetrievalPort.class);
        worker = new GenerationWorker(requestRepository, outputRepository, logRepository, aiProvider,
                lessonAccessPort, new AiCostEstimator(BigDecimal.ONE, BigDecimal.ONE),
                eventPublisher, retrievalPort);
    }

    private AiGenerationRequest request(String type, Map<String, Object> params) {
        AiGenerationRequest req = new AiGenerationRequest(type, courseId, lessonId, teacherId,
                "template_v1", params, "Gemini");
        ReflectionTestUtils.setField(req, "id", requestId);
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(req));
        return req;
    }

    private void lessonOk() {
        when(lessonAccessPort.findLessonSummary(lessonId)).thenReturn(Optional.of(
                new LessonAccessPort.LessonSummary(lessonId, courseId, "Khái niệm dữ liệu", "Mô tả bài")));
        when(lessonAccessPort.findObjectivesByLessonId(lessonId)).thenReturn(List.of(
                new LessonAccessPort.ObjectiveInfo(objectiveId, "Hiểu khái niệm dữ liệu")));
        when(lessonAccessPort.objectiveBelongsToLesson(lessonId, objectiveId)).thenReturn(true);
    }

    private void aiReturns(String json) {
        when(aiProvider.generate(any())).thenReturn(
                new AIProvider.AIResult(json, "Gemini", "gemini-test", 100, 200, 50L));
    }

    private String questionSetJson() {
        return "{\"questions\":[{\"question_text\":\"Dữ liệu là gì?\",\"learning_objective_id\":\""
                + objectiveId + "\"}]}";
    }

    @Test
    void questionSet_thanhCong_luuChunkIds_phatEvent_ghiLog() {
        AiGenerationRequest req = request(AiGenerationRequest.TYPE_QUESTION_SET, Map.of("count", 1));
        lessonOk();
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        when(retrievalPort.search(any(), any(), anyString())).thenReturn(List.of(
                new RetrievalPort.RetrievedChunk(c1, UUID.randomUUID(), lessonId, 0, "đoạn 1", 0.9),
                new RetrievalPort.RetrievedChunk(c2, UUID.randomUUID(), lessonId, 1, "đoạn 2", 0.8)));
        aiReturns(questionSetJson());

        worker.process(requestId);

        assertThat(req.getStatus()).isEqualTo(AiGenerationRequest.STATUS_COMPLETED);
        ArgumentCaptor<AiGenerationOutput> outCaptor = ArgumentCaptor.forClass(AiGenerationOutput.class);
        verify(outputRepository).save(outCaptor.capture());
        assertThat(outCaptor.getValue().getRetrievedChunkIds()).containsExactly(c1, c2);
        assertThat(outCaptor.getValue().getRequestId()).isEqualTo(requestId);

        ArgumentCaptor<AiObservabilityLog> logCaptor = ArgumentCaptor.forClass(AiObservabilityLog.class);
        verify(logRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(AiObservabilityLog.STATUS_SUCCESS);
        assertThat(logCaptor.getValue().getEstimatedCost()).isNotNull();

        verify(eventPublisher).publishEvent(isA(AiReviewPendingEvent.class));
    }

    @Test
    void truyXuatLoi_vanSinhNoiDung_chunkIdsRong() {
        AiGenerationRequest req = request(AiGenerationRequest.TYPE_QUESTION_SET, Map.of("count", 1));
        lessonOk();
        when(retrievalPort.search(any(), any(), anyString())).thenThrow(new RuntimeException("embedding lỗi"));
        aiReturns(questionSetJson());

        worker.process(requestId);

        assertThat(req.getStatus()).isEqualTo(AiGenerationRequest.STATUS_COMPLETED);
        verify(aiProvider, times(1)).generate(any());
        ArgumentCaptor<AiGenerationOutput> outCaptor = ArgumentCaptor.forClass(AiGenerationOutput.class);
        verify(outputRepository).save(outCaptor.capture());
        assertThat(outCaptor.getValue().getRetrievedChunkIds()).isEmpty();
    }

    @Test
    void maTranSaiTong_failed_khongLuuOutput_khongPhatEvent() {
        AiGenerationRequest req = request(AiGenerationRequest.TYPE_ASSESSMENT_MATRIX,
                Map.of("total_questions", 10));
        lessonOk();
        when(retrievalPort.search(any(), any(), anyString())).thenReturn(List.of());
        aiReturns("{\"distribution\":[{\"learning_objective_id\":\"" + objectiveId
                + "\",\"cognitive_level\":\"UNDERSTAND\",\"count\":4},"
                + "{\"learning_objective_id\":\"" + objectiveId
                + "\",\"cognitive_level\":\"APPLY\",\"count\":3}]}");

        worker.process(requestId);

        assertThat(req.getStatus()).isEqualTo(AiGenerationRequest.STATUS_FAILED);
        assertThat(req.getErrorMessage()).contains("AI_BAD_RESPONSE").contains("7").contains("10");
        verify(outputRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void jsonHong_failed() {
        AiGenerationRequest req = request(AiGenerationRequest.TYPE_QUESTION_SET, Map.of("count", 1));
        lessonOk();
        when(retrievalPort.search(any(), any(), anyString())).thenReturn(List.of());
        aiReturns("đây không phải JSON");

        worker.process(requestId);

        assertThat(req.getStatus()).isEqualTo(AiGenerationRequest.STATUS_FAILED);
        assertThat(req.getErrorMessage()).contains("AI_BAD_RESPONSE");
        verify(outputRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void objectiveKhongThuocBai_failed() {
        AiGenerationRequest req = request(AiGenerationRequest.TYPE_QUESTION_SET, Map.of("count", 1));
        lessonOk();
        when(lessonAccessPort.objectiveBelongsToLesson(lessonId, objectiveId)).thenReturn(false);
        when(retrievalPort.search(any(), any(), anyString())).thenReturn(List.of());
        aiReturns(questionSetJson());

        worker.process(requestId);

        assertThat(req.getStatus()).isEqualTo(AiGenerationRequest.STATUS_FAILED);
        assertThat(req.getErrorMessage()).contains("không thuộc bài học");
        verify(outputRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void baiHocKhongConTonTai_failed_khongGoiAi() {
        AiGenerationRequest req = request(AiGenerationRequest.TYPE_QUESTION_SET, Map.of("count", 1));
        when(lessonAccessPort.findLessonSummary(lessonId)).thenReturn(Optional.empty());

        worker.process(requestId);

        assertThat(req.getStatus()).isEqualTo(AiGenerationRequest.STATUS_FAILED);
        assertThat(req.getErrorMessage()).contains("Bài học không còn tồn tại");
        verifyNoInteractions(aiProvider, outputRepository, eventPublisher);
    }

    @Test
    void requestKhongTonTai_khongLamGi() {
        when(requestRepository.findById(requestId)).thenReturn(Optional.empty());

        worker.process(requestId);

        verify(requestRepository, never()).save(any());
        verifyNoInteractions(aiProvider, outputRepository, eventPublisher);
    }
}
