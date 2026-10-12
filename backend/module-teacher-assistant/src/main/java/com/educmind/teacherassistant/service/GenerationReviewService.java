package com.educmind.teacherassistant.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.CourseAccessPort;
import com.educmind.common.port.LessonAccessPort;
import com.educmind.common.port.QuestionBankPort;
import com.educmind.common.port.QuestionBankPort.AiQuestionDraft;
import com.educmind.teacherassistant.entity.AiGenerationOutput;
import com.educmind.teacherassistant.entity.AiGenerationRequest;
import com.educmind.teacherassistant.repository.AiGenerationOutputRepository;
import com.educmind.teacherassistant.repository.AiGenerationRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Duyệt output AI (human-in-the-loop). QUESTION_SET được duyệt xong mới INSERT vào questions (PATCH 1).
 */
@Service
public class GenerationReviewService {

    private static final Set<String> ACTIONS = Set.of("APPROVE", "REJECT", "EDIT_AND_APPROVE");

    public record ReviewResult(String reviewStatus, OffsetDateTime appliedAt, int createdQuestions) {
    }

    private final AiGenerationOutputRepository outputRepository;
    private final AiGenerationRequestRepository requestRepository;
    private final CourseAccessPort courseAccessPort;
    private final LessonAccessPort lessonAccessPort;
    private final QuestionBankPort questionBankPort;

    public GenerationReviewService(AiGenerationOutputRepository outputRepository,
                                   AiGenerationRequestRepository requestRepository,
                                   CourseAccessPort courseAccessPort,
                                   LessonAccessPort lessonAccessPort,
                                   QuestionBankPort questionBankPort) {
        this.outputRepository = outputRepository;
        this.requestRepository = requestRepository;
        this.courseAccessPort = courseAccessPort;
        this.lessonAccessPort = lessonAccessPort;
        this.questionBankPort = questionBankPort;
    }

    @Transactional
    public ReviewResult review(UUID actorId, UUID outputId, Map<String, Object> body) {
        Map<String, Object> safe = body == null ? Map.of() : body;
        String action = safe.get("action") == null ? "" : safe.get("action").toString().trim().toUpperCase();
        if (!ACTIONS.contains(action)) {
            throw new BusinessException("VALIDATION_ERROR",
                    "action phải là APPROVE, REJECT hoặc EDIT_AND_APPROVE", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        String notes = safe.get("notes") == null ? null : safe.get("notes").toString();

        AiGenerationOutput output = outputRepository.findByIdForUpdate(outputId).orElseThrow(() ->
                new BusinessException("OUTPUT_NOT_FOUND", "Không tìm thấy output", HttpStatus.NOT_FOUND));
        AiGenerationRequest req = requestRepository.findById(output.getRequestId()).orElseThrow(() ->
                new BusinessException("GENERATION_REQUEST_NOT_FOUND", "Không tìm thấy yêu cầu", HttpStatus.NOT_FOUND));

        if (!courseAccessPort.isOwnerOrCoTeacher(actorId, req.getCourseId())) {
            throw new BusinessException("NOT_REQUEST_OWNER",
                    "Chỉ owner hoặc đồng giảng dạy của course mới được duyệt", HttpStatus.FORBIDDEN);
        }
        if (!AiGenerationOutput.REVIEW_PENDING.equals(output.getReviewStatus())) {
            throw new BusinessException("OUTPUT_ALREADY_REVIEWED",
                    "Output này đã được duyệt hoặc từ chối", HttpStatus.CONFLICT);
        }

        if ("REJECT".equals(action)) {
            output.reject(actorId, notes);
            outputRepository.save(output);
            return new ReviewResult(output.getReviewStatus(), null, 0);
        }

        Map<String, Object> effective;
        if ("EDIT_AND_APPROVE".equals(action)) {
            effective = asMap(safe.get("edited_content"));
            if (effective == null || effective.isEmpty()) {
                throw invalidEdited("Thiếu edited_content");
            }
            validateEditedShape(req, effective);
            output.editAndApprove(actorId, notes, effective);
        } else {
            effective = output.getContent();
            output.approve(actorId, notes);
        }

        int created = 0;
        if (AiGenerationRequest.TYPE_QUESTION_SET.equals(req.getRequestType())) {
            List<AiQuestionDraft> drafts = toDrafts(req, effective);
            created = questionBankPort.createApprovedAiQuestions(actorId, req.getCourseId(), drafts).size();
            output.markApplied();
        }
        outputRepository.save(output);
        return new ReviewResult(output.getReviewStatus(), output.getAppliedAt(), created);
    }

    private void validateEditedShape(AiGenerationRequest req, Map<String, Object> content) {
        switch (req.getRequestType()) {
            case AiGenerationRequest.TYPE_LESSON_PLAN -> {
                if (!(content.get("activities") instanceof List<?> a) || a.isEmpty()) {
                    throw invalidEdited("Giáo án phải có ít nhất 1 hoạt động (activities)");
                }
            }
            case AiGenerationRequest.TYPE_QUESTION_SET -> toDrafts(req, content);
            case AiGenerationRequest.TYPE_ASSESSMENT_MATRIX -> {
                if (!(content.get("distribution") instanceof List<?> rows) || rows.isEmpty()) {
                    throw invalidEdited("Ma trận phải có ít nhất 1 dòng (distribution)");
                }
                for (Object r : rows) {
                    Map<String, Object> row = asMap(r);
                    if (row == null) {
                        throw invalidEdited("Mỗi dòng ma trận phải là một đối tượng");
                    }
                    UUID objectiveId = parseUuid(row.get("learning_objective_id"));
                    if (objectiveId == null
                            || !lessonAccessPort.objectiveBelongsToLesson(req.getLessonId(), objectiveId)) {
                        throw invalidEdited("learning_objective_id không thuộc bài học");
                    }
                    if (!(row.get("count") instanceof Number n) || n.intValue() < 1) {
                        throw invalidEdited("count phải là số nguyên dương");
                    }
                }
            }
            default -> throw invalidEdited("Loại yêu cầu không hợp lệ");
        }
    }

    private List<AiQuestionDraft> toDrafts(AiGenerationRequest req, Map<String, Object> content) {
        if (!(content.get("questions") instanceof List<?> list) || list.isEmpty()) {
            throw invalidEdited("Bộ câu hỏi phải có ít nhất 1 câu (questions)");
        }
        List<AiQuestionDraft> drafts = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> q = asMap(item);
            if (q == null) {
                throw invalidEdited("Mỗi câu hỏi phải là một đối tượng");
            }
            UUID objectiveId = parseUuid(q.get("learning_objective_id"));
            if (objectiveId != null
                    && !lessonAccessPort.objectiveBelongsToLesson(req.getLessonId(), objectiveId)) {
                throw invalidEdited("learning_objective_id không thuộc bài học");
            }
            drafts.add(new AiQuestionDraft(
                    req.getLessonId(),
                    objectiveId,
                    str(q.get("question_text")),
                    str(q.get("question_type")),
                    str(q.get("difficulty")),
                    str(q.get("cognitive_level")),
                    q.get("options"),
                    q.get("correct_answer"),
                    str(q.get("explanation"))));
        }
        return drafts;
    }

    private static UUID parseUuid(Object o) {
        if (o == null || o.toString().isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(o.toString());
        } catch (IllegalArgumentException e) {
            throw invalidEdited("learning_objective_id không phải UUID hợp lệ: " + o);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static BusinessException invalidEdited(String message) {
        return new BusinessException("INVALID_EDITED_CONTENT", message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
