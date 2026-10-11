package com.educmind.assessment.service;

import com.educmind.assessment.entity.CognitiveLevel;
import com.educmind.assessment.entity.Difficulty;
import com.educmind.assessment.entity.Question;
import com.educmind.assessment.entity.QuestionSource;
import com.educmind.assessment.entity.QuestionType;
import com.educmind.assessment.repository.QuestionRepository;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.LessonAccessPort;
import com.educmind.common.port.QuestionBankPort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Adapter của QuestionBankPort. Module 7 gọi khi giáo viên duyệt QUESTION_SET (PATCH_4Decisions PATCH 1):
 * chỉ lúc này dòng questions mới được INSERT, source=AI_GENERATED, review_status=APPROVED.
 */
@Service
public class QuestionBankPortImpl implements QuestionBankPort {

    private final QuestionRepository questionRepository;
    private final LessonAccessPort lessonAccessPort;

    public QuestionBankPortImpl(QuestionRepository questionRepository, LessonAccessPort lessonAccessPort) {
        this.questionRepository = questionRepository;
        this.lessonAccessPort = lessonAccessPort;
    }

    @Override
    @Transactional
    public List<UUID> createApprovedAiQuestions(UUID actorId, UUID courseId, List<AiQuestionDraft> drafts) {
        if (drafts == null || drafts.isEmpty()) {
            throw invalid("Không có câu hỏi nào để tạo");
        }
        List<Question> toSave = new ArrayList<>();
        for (int i = 0; i < drafts.size(); i++) {
            toSave.add(build(i + 1, actorId, courseId, drafts.get(i)));
        }
        return questionRepository.saveAll(toSave).stream().map(Question::getId).toList();
    }

    private Question build(int no, UUID actorId, UUID courseId, AiQuestionDraft d) {
        String prefix = "Câu " + no + ": ";

        if (d.questionText() == null || d.questionText().isBlank()) {
            throw invalid(prefix + "nội dung câu hỏi trống");
        }
        QuestionType type = parseEnum(QuestionType.class, d.questionType(), prefix + "question_type không hợp lệ");
        if (type != QuestionType.SINGLE_CHOICE && type != QuestionType.MULTIPLE_CHOICE) {
            throw invalid(prefix + "hiện chỉ hỗ trợ SINGLE_CHOICE và MULTIPLE_CHOICE");
        }
        Difficulty difficulty = d.difficulty() == null || d.difficulty().isBlank()
                ? Difficulty.MEDIUM
                : parseEnum(Difficulty.class, d.difficulty(), prefix + "difficulty không hợp lệ");
        CognitiveLevel cognitive = d.cognitiveLevel() == null || d.cognitiveLevel().isBlank()
                ? null
                : parseEnum(CognitiveLevel.class, d.cognitiveLevel(), prefix + "cognitive_level không hợp lệ");

        if (d.learningObjectiveId() != null && d.lessonId() == null) {
            throw invalid(prefix + "có learning_objective_id thì bắt buộc phải có lesson_id");
        }
        if (d.lessonId() != null && !lessonAccessPort.lessonBelongsToCourse(d.lessonId(), courseId)) {
            throw invalid(prefix + "lesson_id không thuộc course này");
        }
        if (d.learningObjectiveId() != null
                && !lessonAccessPort.objectiveBelongsToLesson(d.lessonId(), d.learningObjectiveId())) {
            throw invalid(prefix + "learning_objective_id không thuộc bài học");
        }

        validateChoices(prefix, type, d.options(), d.correctAnswer());

        Question q = new Question(courseId, d.questionText().trim(), type, difficulty, actorId);
        q.setLessonId(d.lessonId());
        q.setCognitiveLevel(cognitive);
        q.setLearningObjectiveId(d.learningObjectiveId());
        q.setOptions(d.options());
        q.setCorrectAnswer(d.correctAnswer());
        q.setExplanation(d.explanation());
        q.setSource(QuestionSource.AI_GENERATED);
        q.approve();
        return q;
    }

    private void validateChoices(String prefix, QuestionType type, Object options, Object correctAnswer) {
        if (!(options instanceof List<?> optList) || optList.size() < 2) {
            throw invalid(prefix + "cần ít nhất 2 lựa chọn trong options");
        }
        Set<String> ids = new HashSet<>();
        for (Object o : optList) {
            if (!(o instanceof Map<?, ?> m)) {
                throw invalid(prefix + "mỗi lựa chọn phải có dạng {id, text}");
            }
            Object id = m.get("id");
            Object text = m.get("text");
            if (id == null || id.toString().isBlank() || text == null || text.toString().isBlank()) {
                throw invalid(prefix + "mỗi lựa chọn phải có id và text không trống");
            }
            if (!ids.add(id.toString())) {
                throw invalid(prefix + "id lựa chọn bị trùng: " + id);
            }
        }
        if (!(correctAnswer instanceof List<?> correct) || correct.isEmpty()) {
            throw invalid(prefix + "correct_answer phải là danh sách id đúng, không trống");
        }
        Set<String> seen = new HashSet<>();
        for (Object c : correct) {
            String id = c == null ? "" : c.toString();
            if (!ids.contains(id)) {
                throw invalid(prefix + "correct_answer chứa id không có trong options: " + id);
            }
            seen.add(id);
        }
        if (type == QuestionType.SINGLE_CHOICE && seen.size() != 1) {
            throw invalid(prefix + "SINGLE_CHOICE phải có đúng 1 đáp án đúng");
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> cls, String value, String message) {
        if (value == null) {
            throw invalid(message);
        }
        try {
            return Enum.valueOf(cls, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw invalid(message);
        }
    }

    private static BusinessException invalid(String message) {
        return new BusinessException("VALIDATION_ERROR", message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
