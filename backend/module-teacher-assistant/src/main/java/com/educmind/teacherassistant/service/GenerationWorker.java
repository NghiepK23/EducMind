package com.educmind.teacherassistant.service;

import com.educmind.common.event.AiReviewPendingEvent;
import com.educmind.common.port.AIProvider;
import com.educmind.common.port.AIProviderException;
import com.educmind.common.port.LessonAccessPort;
import com.educmind.teacherassistant.entity.AiGenerationOutput;
import com.educmind.teacherassistant.entity.AiGenerationRequest;
import com.educmind.teacherassistant.entity.AiObservabilityLog;
import com.educmind.teacherassistant.repository.AiGenerationOutputRepository;
import com.educmind.teacherassistant.repository.AiGenerationRequestRepository;
import com.educmind.teacherassistant.repository.AiObservabilityLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GenerationWorker {

    private static final Logger log = LoggerFactory.getLogger(GenerationWorker.class);

    private final AiGenerationRequestRepository requestRepository;
    private final AiGenerationOutputRepository outputRepository;
    private final AiObservabilityLogRepository logRepository;
    private final AIProvider aiProvider;
    private final LessonAccessPort lessonAccessPort;
    private final AiCostEstimator costEstimator;
    private final ApplicationEventPublisher eventPublisher;

    public GenerationWorker(AiGenerationRequestRepository requestRepository,
                            AiGenerationOutputRepository outputRepository,
                            AiObservabilityLogRepository logRepository,
                            AIProvider aiProvider,
                            LessonAccessPort lessonAccessPort,
                            AiCostEstimator costEstimator,
                            ApplicationEventPublisher eventPublisher) {
        this.requestRepository = requestRepository;
        this.outputRepository = outputRepository;
        this.logRepository = logRepository;
        this.aiProvider = aiProvider;
        this.lessonAccessPort = lessonAccessPort;
        this.costEstimator = costEstimator;
        this.eventPublisher = eventPublisher;
    }

    @Async
    public void process(UUID requestId) {
        AiGenerationRequest req = requestRepository.findById(requestId).orElse(null);
        if (req == null) {
            log.error("AI generation request {} không tồn tại", requestId);
            return;
        }
        boolean succeeded = false;
        try {
            run(req);
            succeeded = true;
        } catch (Exception e) {
            log.warn("AI generation {} thất bại: {}", requestId, e.getMessage());
            req.markFailed(truncate(e.getMessage(), 1000));
        }
        requestRepository.save(req);
        if (succeeded) {
            publishReviewPending(req);
        }
    }

    private void publishReviewPending(AiGenerationRequest req) {
        try {
            eventPublisher.publishEvent(new AiReviewPendingEvent(
                    req.getId(), req.getRequestedBy(), req.getCourseId(),
                    req.getLessonId(), req.getRequestType()));
        } catch (Exception e) {
            log.warn("Không phát được AiReviewPendingEvent cho request {}: {}", req.getId(), e.getMessage());
        }
    }

    private void run(AiGenerationRequest req) {
        UUID lessonId = req.getLessonId();
        if (lessonId == null) {
            throw new IllegalStateException("Yêu cầu thiếu lesson_id");
        }
        LessonAccessPort.LessonSummary lesson = lessonAccessPort.findLessonSummary(lessonId)
                .orElseThrow(() -> new IllegalStateException("Bài học không còn tồn tại"));
        List<LessonAccessPort.ObjectiveInfo> objectives = lessonAccessPort.findObjectivesByLessonId(lessonId);

        GenerationPromptBuilder.Built built = GenerationPromptBuilder.build(
                req.getRequestType(), lesson, objectives, req.getInputParams());

        long start = System.nanoTime();
        AIProvider.AIResult result;
        try {
            result = aiProvider.generate(new AIProvider.AIRequest(
                    built.systemPrompt(), built.userPrompt(), built.schema()));
        } catch (AIProviderException e) {
            int ms = (int) ((System.nanoTime() - start) / 1_000_000);
            logRepository.save(new AiObservabilityLog(
                    AiObservabilityLog.CONTEXT_TEACHER_ASSISTANT, req.getId(), req.getRequestedBy(),
                    req.getAiProvider(), "unknown", req.getRequestType(), ms, null, null,
                    AiObservabilityLog.STATUS_FAILED, truncate(e.getCode() + ": " + e.getMessage(), 1000)));
            throw new IllegalStateException("AI_GENERATION_FAILED (" + e.getCode() + "): " + e.getMessage(), e);
        }

        AiObservabilityLog okLog = new AiObservabilityLog(
                AiObservabilityLog.CONTEXT_TEACHER_ASSISTANT, req.getId(), req.getRequestedBy(),
                result.provider(), truncate(result.model(), 50), req.getRequestType(),
                (int) result.latencyMs(), result.inputTokens(), result.outputTokens(),
                AiObservabilityLog.STATUS_SUCCESS, null);
        okLog.setEstimatedCost(costEstimator.estimate(result.inputTokens(), result.outputTokens()));
        logRepository.save(okLog);

        Map<String, Object> content;
        try {
            content = JsonParserFactory.getJsonParser().parseMap(result.text());
        } catch (Exception e) {
            throw new IllegalStateException("AI_BAD_RESPONSE: không đọc được JSON AI trả về", e);
        }

        validateContent(req.getRequestType(), content, lessonId);

        outputRepository.save(new AiGenerationOutput(req.getId(), content));
        req.markCompleted();
    }

    private void validateContent(String type, Map<String, Object> content, UUID lessonId) {
        switch (type) {
            case AiGenerationRequest.TYPE_LESSON_PLAN -> {
                if (!(content.get("activities") instanceof List<?> a) || a.isEmpty()) {
                    throw new IllegalStateException("AI_BAD_RESPONSE: giáo án không có hoạt động nào");
                }
            }
            case AiGenerationRequest.TYPE_QUESTION_SET -> {
                List<Map<String, Object>> questions = listOfMaps(content.get("questions"));
                if (questions.isEmpty()) {
                    throw new IllegalStateException("AI_BAD_RESPONSE: không có câu hỏi nào");
                }
                for (Map<String, Object> q : questions) {
                    Object text = q.get("question_text");
                    if (text == null || text.toString().isBlank()) {
                        throw new IllegalStateException("AI_BAD_RESPONSE: có câu hỏi rỗng");
                    }
                    checkObjective(q.get("learning_objective_id"), lessonId);
                }
            }
            case AiGenerationRequest.TYPE_ASSESSMENT_MATRIX -> {
                List<Map<String, Object>> rows = listOfMaps(content.get("distribution"));
                if (rows.isEmpty()) {
                    throw new IllegalStateException("AI_BAD_RESPONSE: ma trận không có dòng nào");
                }
                for (Map<String, Object> row : rows) {
                    checkObjective(row.get("learning_objective_id"), lessonId);
                }
            }
            default -> throw new IllegalStateException("Loại yêu cầu không hợp lệ: " + type);
        }
    }

    private void checkObjective(Object rawId, UUID lessonId) {
        UUID id;
        try {
            id = UUID.fromString(String.valueOf(rawId));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("AI_BAD_RESPONSE: learning_objective_id không phải UUID: " + rawId);
        }
        if (!lessonAccessPort.objectiveBelongsToLesson(lessonId, id)) {
            throw new IllegalStateException(
                    "AI_BAD_RESPONSE: learning_objective_id không thuộc bài học: " + id);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> listOfMaps(Object o) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (o instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    out.add((Map<String, Object>) m);
                }
            }
        }
        return out;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
