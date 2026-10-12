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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerationReviewServiceTest {

    @Mock
    private AiGenerationOutputRepository outputRepository;
    @Mock
    private AiGenerationRequestRepository requestRepository;
    @Mock
    private CourseAccessPort courseAccessPort;
    @Mock
    private LessonAccessPort lessonAccessPort;
    @Mock
    private QuestionBankPort questionBankPort;
    @InjectMocks
    private GenerationReviewService service;

    @Captor
    private ArgumentCaptor<List<AiQuestionDraft>> draftsCaptor;

    private final UUID actorId = UUID.randomUUID();
    private final UUID outputId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();
    private final UUID lessonId = UUID.randomUUID();
    private final UUID objectiveId = UUID.randomUUID();

    // ---------- helpers ----------

    private AiGenerationRequest request(String type) {
        return new AiGenerationRequest(type, courseId, lessonId, UUID.randomUUID(),
                "prompt_v1", Map.of(), "Gemini");
    }

    private AiGenerationOutput stubPending(String type, Map<String, Object> content, boolean actorMayReview) {
        UUID requestId = UUID.randomUUID();
        AiGenerationOutput output = new AiGenerationOutput(requestId, content);
        when(outputRepository.findByIdForUpdate(outputId)).thenReturn(Optional.of(output));
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request(type)));
        when(courseAccessPort.isOwnerOrCoTeacher(actorId, courseId)).thenReturn(actorMayReview);
        return output;
    }

    private Map<String, Object> body(String action) {
        Map<String, Object> b = new HashMap<>();
        b.put("action", action);
        return b;
    }

    private Map<String, Object> question(String text, UUID objective) {
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("question_text", text);
        q.put("question_type", "SINGLE_CHOICE");
        q.put("difficulty", "EASY");
        q.put("cognitive_level", "REMEMBER");
        q.put("learning_objective_id", objective == null ? null : objective.toString());
        q.put("options", List.of(Map.of("id", "A", "text", "x"), Map.of("id", "B", "text", "y")));
        q.put("correct_answer", List.of("A"));
        q.put("explanation", "Giải thích");
        return q;
    }

    private Map<String, Object> questionSet(List<Map<String, Object>> questions) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("questions", questions);
        return c;
    }

    private static void assertBusiness(String code, Executable executable) {
        BusinessException ex = assertThrows(BusinessException.class, executable);
        assertEquals(code, ex.getErrorCode());
    }

    // ---------- kiểm tra đầu vào / quyền / trạng thái ----------

    @Test
    void invalidAction_isRejected() {
        assertBusiness("VALIDATION_ERROR", () -> service.review(actorId, outputId, body("XOA")));
        assertBusiness("VALIDATION_ERROR", () -> service.review(actorId, outputId, null));
        verifyNoInteractions(questionBankPort);
    }

    @Test
    void outputNotFound_isReported() {
        when(outputRepository.findByIdForUpdate(outputId)).thenReturn(Optional.empty());

        assertBusiness("OUTPUT_NOT_FOUND", () -> service.review(actorId, outputId, body("APPROVE")));
    }

    @Test
    void actorNotOwnerNorCoTeacher_isForbidden_andNothingCreated() {
        stubPending(AiGenerationRequest.TYPE_QUESTION_SET,
                questionSet(List.of(question("Câu 1", objectiveId))), false);

        assertBusiness("NOT_REQUEST_OWNER", () -> service.review(actorId, outputId, body("APPROVE")));
        verifyNoInteractions(questionBankPort);
        verify(outputRepository, never()).save(any());
    }

    @Test
    void secondReview_isRejectedAsAlreadyReviewed() {
        AiGenerationOutput output = stubPending(AiGenerationRequest.TYPE_QUESTION_SET,
                questionSet(List.of(question("Câu 1", objectiveId))), true);
        output.reject(UUID.randomUUID(), "đã từ chối trước đó");

        assertBusiness("OUTPUT_ALREADY_REVIEWED", () -> service.review(actorId, outputId, body("APPROVE")));
        verifyNoInteractions(questionBankPort);
    }

    // ---------- REJECT ----------

    @Test
    void reject_createsNoQuestions() {
        AiGenerationOutput output = stubPending(AiGenerationRequest.TYPE_QUESTION_SET,
                questionSet(List.of(question("Câu 1", objectiveId))), true);

        GenerationReviewService.ReviewResult r = service.review(actorId, outputId, body("REJECT"));

        assertEquals(AiGenerationOutput.REVIEW_REJECTED, r.reviewStatus());
        assertEquals(0, r.createdQuestions());
        assertNull(r.appliedAt());
        assertEquals(actorId, output.getReviewedBy());
        verifyNoInteractions(questionBankPort);
        verify(outputRepository).save(output);
    }

    // ---------- APPROVE QUESTION_SET ----------

    @Test
    void approveQuestionSet_createsOneQuestionPerDraft_fromOriginalContent() {
        AiGenerationOutput output = stubPending(AiGenerationRequest.TYPE_QUESTION_SET,
                questionSet(List.of(question("Câu 1", objectiveId), question("Câu 2", objectiveId))), true);
        when(lessonAccessPort.objectiveBelongsToLesson(lessonId, objectiveId)).thenReturn(true);
        when(questionBankPort.createApprovedAiQuestions(eq(actorId), eq(courseId), anyList()))
                .thenReturn(List.of(UUID.randomUUID(), UUID.randomUUID()));

        GenerationReviewService.ReviewResult r = service.review(actorId, outputId, body("APPROVE"));

        assertEquals(AiGenerationOutput.REVIEW_APPROVED, r.reviewStatus());
        assertEquals(2, r.createdQuestions());
        assertNotNull(r.appliedAt());
        assertNotNull(output.getAppliedAt());
        verify(questionBankPort).createApprovedAiQuestions(eq(actorId), eq(courseId), draftsCaptor.capture());
        List<AiQuestionDraft> drafts = draftsCaptor.getValue();
        assertEquals(2, drafts.size());
        assertEquals("Câu 1", drafts.get(0).questionText());
        assertEquals("Câu 2", drafts.get(1).questionText());
        assertEquals(lessonId, drafts.get(0).lessonId());
        assertEquals(objectiveId, drafts.get(0).learningObjectiveId());
        verify(outputRepository).save(output);
    }

    @Test
    void editAndApprove_usesEditedContent_notOriginal() {
        AiGenerationOutput output = stubPending(AiGenerationRequest.TYPE_QUESTION_SET,
                questionSet(List.of(question("Câu gốc", objectiveId))), true);
        when(lessonAccessPort.objectiveBelongsToLesson(lessonId, objectiveId)).thenReturn(true);
        when(questionBankPort.createApprovedAiQuestions(eq(actorId), eq(courseId), anyList()))
                .thenReturn(List.of(UUID.randomUUID()));
        Map<String, Object> edited = questionSet(List.of(question("Câu đã sửa", objectiveId)));
        Map<String, Object> b = body("EDIT_AND_APPROVE");
        b.put("edited_content", edited);
        b.put("notes", "Sửa cho rõ nghĩa");

        GenerationReviewService.ReviewResult r = service.review(actorId, outputId, b);

        assertEquals(AiGenerationOutput.REVIEW_EDITED_AND_APPROVED, r.reviewStatus());
        assertEquals(1, r.createdQuestions());
        assertEquals(edited, output.getEditedContent());
        verify(questionBankPort).createApprovedAiQuestions(eq(actorId), eq(courseId), draftsCaptor.capture());
        assertEquals("Câu đã sửa", draftsCaptor.getValue().get(0).questionText());
    }

    // ---------- INVALID_EDITED_CONTENT ----------

    @Test
    void editAndApprove_missingEditedContent_isRejected() {
        stubPending(AiGenerationRequest.TYPE_QUESTION_SET,
                questionSet(List.of(question("Câu 1", objectiveId))), true);

        assertBusiness("INVALID_EDITED_CONTENT",
                () -> service.review(actorId, outputId, body("EDIT_AND_APPROVE")));
        verifyNoInteractions(questionBankPort);
        verify(outputRepository, never()).save(any());
    }

    @Test
    void editAndApprove_objectiveNotInLesson_isRejected_andNothingCreated() {
        stubPending(AiGenerationRequest.TYPE_QUESTION_SET,
                questionSet(List.of(question("Câu gốc", objectiveId))), true);
        when(lessonAccessPort.objectiveBelongsToLesson(lessonId, objectiveId)).thenReturn(false);
        Map<String, Object> b = body("EDIT_AND_APPROVE");
        b.put("edited_content", questionSet(List.of(question("Câu sửa", objectiveId))));

        assertBusiness("INVALID_EDITED_CONTENT", () -> service.review(actorId, outputId, b));
        verifyNoInteractions(questionBankPort);
        verify(outputRepository, never()).save(any());
    }

    @Test
    void approve_objectiveNotInLesson_isRejected_andNothingCreated() {
        stubPending(AiGenerationRequest.TYPE_QUESTION_SET,
                questionSet(List.of(question("Câu 1", objectiveId))), true);
        when(lessonAccessPort.objectiveBelongsToLesson(lessonId, objectiveId)).thenReturn(false);

        assertBusiness("INVALID_EDITED_CONTENT", () -> service.review(actorId, outputId, body("APPROVE")));
        verifyNoInteractions(questionBankPort);
        verify(outputRepository, never()).save(any());
    }

    @Test
    void editAndApprove_matrixWithNonPositiveCount_isRejected() {
        stubPending(AiGenerationRequest.TYPE_ASSESSMENT_MATRIX, new LinkedHashMap<>(), true);
        when(lessonAccessPort.objectiveBelongsToLesson(lessonId, objectiveId)).thenReturn(true);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("learning_objective_id", objectiveId.toString());
        row.put("count", 0);
        Map<String, Object> edited = new LinkedHashMap<>();
        edited.put("distribution", List.of(row));
        Map<String, Object> b = body("EDIT_AND_APPROVE");
        b.put("edited_content", edited);

        assertBusiness("INVALID_EDITED_CONTENT", () -> service.review(actorId, outputId, b));
        verifyNoInteractions(questionBankPort);
    }

    // ---------- loại khác không đụng ngân hàng câu hỏi ----------

    @Test
    void approveLessonPlan_doesNotTouchQuestionBank() {
        AiGenerationOutput output = stubPending(AiGenerationRequest.TYPE_LESSON_PLAN,
                new LinkedHashMap<>(Map.of("activities", List.of(Map.of("title", "Khởi động")))), true);

        GenerationReviewService.ReviewResult r = service.review(actorId, outputId, body("APPROVE"));

        assertEquals(AiGenerationOutput.REVIEW_APPROVED, r.reviewStatus());
        assertEquals(0, r.createdQuestions());
        assertNull(r.appliedAt());
        verifyNoInteractions(questionBankPort);
        verify(outputRepository).save(output);
    }
}
