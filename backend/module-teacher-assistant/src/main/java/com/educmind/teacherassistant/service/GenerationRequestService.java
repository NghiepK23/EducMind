package com.educmind.teacherassistant.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.CourseAccessPort;
import com.educmind.common.port.LessonAccessPort;
import com.educmind.teacherassistant.config.TeacherAssistantProperties;
import com.educmind.teacherassistant.entity.AiGenerationOutput;
import com.educmind.teacherassistant.entity.AiGenerationRequest;
import com.educmind.teacherassistant.repository.AiGenerationOutputRepository;
import com.educmind.teacherassistant.repository.AiGenerationRequestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GenerationRequestService {

    private static final String PROVIDER = "Gemini";

    private final AiGenerationRequestRepository requestRepository;
    private final AiGenerationOutputRepository outputRepository;
    private final GenerationWorker worker;
    private final CourseAccessPort courseAccessPort;
    private final LessonAccessPort lessonAccessPort;
    private final TeacherAssistantProperties properties;

    public GenerationRequestService(AiGenerationRequestRepository requestRepository,
                                    AiGenerationOutputRepository outputRepository,
                                    GenerationWorker worker,
                                    CourseAccessPort courseAccessPort,
                                    LessonAccessPort lessonAccessPort,
                                    TeacherAssistantProperties properties) {
        this.requestRepository = requestRepository;
        this.outputRepository = outputRepository;
        this.worker = worker;
        this.courseAccessPort = courseAccessPort;
        this.lessonAccessPort = lessonAccessPort;
        this.properties = properties;
    }

    public AiGenerationRequest create(UUID actorId, String type, Map<String, Object> body) {
        Map<String, Object> safeBody = body == null ? Map.of() : body;
        UUID lessonId = parseLessonId(safeBody.get("lesson_id"));

        UUID courseId = lessonAccessPort.findCourseIdByLessonId(lessonId).orElseThrow(() ->
                new BusinessException("LESSON_NOT_FOUND",
                        "Không tìm thấy bài học", HttpStatus.NOT_FOUND));
        if (!courseAccessPort.isOwnerOrCoTeacher(actorId, courseId)) {
            throw new BusinessException("NOT_COURSE_OWNER",
                    "Chỉ owner hoặc đồng giảng dạy của course mới được yêu cầu AI soạn nội dung",
                    HttpStatus.FORBIDDEN);
        }

        long usedLast24h = requestRepository.countByRequestedByAndCreatedAtAfter(
                actorId, OffsetDateTime.now().minusHours(24));
        if (usedLast24h >= properties.getDailyLimit()) {
            throw new BusinessException("RATE_LIMIT_EXCEEDED",
                    "Bạn đã dùng hết " + properties.getDailyLimit()
                            + " lượt yêu cầu AI trong 24 giờ gần nhất, vui lòng thử lại sau",
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        Map<String, Object> params = new LinkedHashMap<>();
        String template;
        switch (type) {
            case AiGenerationRequest.TYPE_LESSON_PLAN -> {
                template = "lesson_plan_v1";
                params.put("duration_minutes", intInRange(safeBody, "duration_minutes", 45, 15, 180));
            }
            case AiGenerationRequest.TYPE_QUESTION_SET -> {
                template = "question_generation_v1";
                params.put("count", intInRange(safeBody, "count", 5, 1, 20));
                Object dist = safeBody.get("cognitive_level_distribution");
                if (dist instanceof Map<?, ?> m && !m.isEmpty()) {
                    params.put("cognitive_level_distribution", m);
                }
            }
            case AiGenerationRequest.TYPE_ASSESSMENT_MATRIX -> {
                template = "assessment_matrix_v1";
                params.put("total_questions", intInRange(safeBody, "total_questions", 20, 1, 100));
            }
            default -> throw new IllegalArgumentException("Loại yêu cầu không hợp lệ: " + type);
        }

        AiGenerationRequest saved = requestRepository.save(new AiGenerationRequest(
                type, courseId, lessonId, actorId, template, params, PROVIDER));
        worker.process(saved.getId());
        return saved;
    }

    public AiGenerationRequest find(UUID actorId, UUID id) {
        AiGenerationRequest req = requestRepository.findById(id).orElseThrow(() ->
                new BusinessException("GENERATION_REQUEST_NOT_FOUND",
                        "Không tìm thấy yêu cầu", HttpStatus.NOT_FOUND));
        if (!req.getRequestedBy().equals(actorId)) {
            throw new BusinessException("NOT_REQUEST_OWNER",
                    "Bạn không phải người tạo yêu cầu này", HttpStatus.FORBIDDEN);
        }
        return req;
    }

    public List<AiGenerationOutput> outputsOf(UUID requestId) {
        return outputRepository.findByRequestId(requestId);
    }

    public Page<AiGenerationRequest> list(UUID actorId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return requestRepository.findByRequestedBy(actorId,
                PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    private static UUID parseLessonId(Object raw) {
        if (raw == null) {
            throw new BusinessException("INVALID_REQUEST_PARAMS",
                    "Thiếu lesson_id", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        try {
            return UUID.fromString(raw.toString());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_REQUEST_PARAMS",
                    "lesson_id không phải UUID hợp lệ", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private static int intInRange(Map<String, Object> body, String key, int def, int min, int max) {
        Object v = body.get(key);
        if (v == null) {
            return def;
        }
        int n;
        try {
            n = v instanceof Number num ? num.intValue() : Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            throw new BusinessException("INVALID_REQUEST_PARAMS",
                    key + " phải là số nguyên", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (n < min || n > max) {
            throw new BusinessException("INVALID_REQUEST_PARAMS",
                    key + " phải trong khoảng " + min + " đến " + max, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return n;
    }
}
