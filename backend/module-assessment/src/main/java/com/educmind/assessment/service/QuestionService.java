package com.educmind.assessment.service;

import com.educmind.assessment.dto.CreateQuestionRequest;
import com.educmind.assessment.dto.QuestionResponse;
import com.educmind.assessment.dto.UpdateQuestionRequest;
import com.educmind.assessment.entity.DifficultyLevel;
import com.educmind.assessment.entity.Question;
import com.educmind.assessment.entity.QuestionObjective;
import com.educmind.assessment.entity.QuestionType;
import com.educmind.assessment.repository.AssessmentAttemptRepository;
import com.educmind.assessment.repository.AssessmentQuestionRepository;
import com.educmind.assessment.repository.QuestionObjectiveRepository;
import com.educmind.assessment.repository.QuestionRepository;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.CourseContentPort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * US-T05 — ngân hàng câu hỏi gắn mục tiêu học tập.
 * AC-T05.1 đủ phương án + đáp án, AC-T05.2 nguồn "tự soạn", AC-T05.3 mục tiêu cùng khóa,
 * AC-T05.5 không trùng mục tiêu, AC-T05.6 được phép không gắn mục tiêu.
 * AC-T11.4 chặn sửa nội dung câu hỏi đã dùng trong bài kiểm tra có lượt làm.
 */
@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionObjectiveRepository questionObjectiveRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final CourseContentPort contentPort;
    private final AssessmentPermission permission;
    private final JsonCodec codec;

    public QuestionService(QuestionRepository questionRepository,
                           QuestionObjectiveRepository questionObjectiveRepository,
                           AssessmentQuestionRepository assessmentQuestionRepository,
                           AssessmentAttemptRepository attemptRepository,
                           CourseContentPort contentPort,
                           AssessmentPermission permission,
                           JsonCodec codec) {
        this.questionRepository = questionRepository;
        this.questionObjectiveRepository = questionObjectiveRepository;
        this.assessmentQuestionRepository = assessmentQuestionRepository;
        this.attemptRepository = attemptRepository;
        this.contentPort = contentPort;
        this.permission = permission;
        this.codec = codec;
    }

    @Transactional
    public QuestionResponse create(UUID teacherId, UUID courseId, CreateQuestionRequest request) {
        permission.requireManagedCourse(teacherId, courseId);

        QuestionType type = parseType(request.getType());
        DifficultyLevel difficulty = parseDifficulty(request.getDifficulty());
        validateChoice(type, request.getOptions(), request.getCorrectOptionIndexes());

        String optionsJson = type.isChoice() ? codec.write(request.getOptions()) : null;
        String correctJson;
        if (type.isChoice()) {
            correctJson = codec.write(request.getCorrectOptionIndexes());
        } else {
            correctJson = request.getExpectedAnswer() == null || request.getExpectedAnswer().isBlank()
                    ? null
                    : codec.writeText(request.getExpectedAnswer().trim());
        }

        Question question = questionRepository.save(new Question(courseId,
                request.getContent().trim(), type, difficulty,
                blankToNull(request.getBloomLevel()), optionsJson, correctJson, teacherId));

        replaceObjectives(question.getId(), courseId, request.getObjectiveIds());
        return toResponse(question);
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> list(UUID teacherId, UUID courseId, String type, String difficulty) {
        permission.requireManagedCourse(teacherId, courseId);

        List<Question> questions;
        if (type != null && !type.isBlank()) {
            questions = questionRepository.findByCourseIdAndTypeAndDeletedAtIsNullOrderByCreatedAtDesc(
                    courseId, parseType(type));
        } else if (difficulty != null && !difficulty.isBlank()) {
            questions = questionRepository.findByCourseIdAndDifficultyAndDeletedAtIsNullOrderByCreatedAtDesc(
                    courseId, parseDifficulty(difficulty));
        } else {
            questions = questionRepository.findByCourseIdAndDeletedAtIsNullOrderByCreatedAtDesc(courseId);
        }

        List<QuestionResponse> result = new ArrayList<>();
        for (Question question : questions) {
            result.add(toResponse(question));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public QuestionResponse get(UUID actorId, String role, UUID questionId) {
        Question question = requireQuestion(questionId);
        if (AssessmentPermission.isTeacher(role)) {
            permission.requireManagedCourse(actorId, question.getCourseId());
        }
        return toResponse(question);
    }

    @Transactional
    public QuestionResponse update(UUID teacherId, UUID questionId, UpdateQuestionRequest request) {
        Question question = requireQuestion(questionId);
        permission.requireManagedCourse(teacherId, question.getCourseId());

        boolean touchesContent = request.getContent() != null
                || request.getType() != null
                || request.getOptions() != null
                || request.getCorrectOptionIndexes() != null
                || request.getExpectedAnswer() != null;

        if (touchesContent && isUsedInAssessmentWithAttempts(questionId)) {
            throw new BusinessException("QUESTION_LOCKED",
                    "Câu hỏi đã được dùng trong bài kiểm tra có lượt làm, chỉ được sửa độ khó và mức Bloom",
                    HttpStatus.CONFLICT);
        }

        QuestionType type = request.getType() == null ? question.getType() : parseType(request.getType());
        DifficultyLevel difficulty = request.getDifficulty() == null
                ? question.getDifficulty()
                : parseDifficulty(request.getDifficulty());

        if (touchesContent) {
            List<String> options = request.getOptions() != null
                    ? request.getOptions()
                    : codec.readStringList(question.getOptions());
            List<Integer> correct = request.getCorrectOptionIndexes() != null
                    ? request.getCorrectOptionIndexes()
                    : codec.readIntList(question.getCorrectAnswer());
            validateChoice(type, options, correct);

            String content = request.getContent() == null ? question.getContent() : request.getContent().trim();
            String optionsJson = type.isChoice() ? codec.write(options) : null;
            String correctJson;
            if (type.isChoice()) {
                correctJson = codec.write(correct);
            } else {
                String expected = request.getExpectedAnswer();
                correctJson = expected == null || expected.isBlank() ? null : codec.writeText(expected.trim());
            }
            question.update(content, type, difficulty, blankToNull(request.getBloomLevel()),
                    optionsJson, correctJson, teacherId);
        } else {
            question.updateMetadata(difficulty, blankToNull(request.getBloomLevel()), teacherId);
        }

        question = questionRepository.save(question);

        if (request.getObjectiveIds() != null) {
            if (isUsedInAssessmentWithAttempts(questionId)) {
                throw new BusinessException("QUESTION_LOCKED",
                        "Câu hỏi đã được dùng trong bài kiểm tra có lượt làm, không thể đổi mục tiêu",
                        HttpStatus.CONFLICT);
            }
            replaceObjectives(questionId, question.getCourseId(), request.getObjectiveIds());
        }
        return toResponse(question);
    }

    /** AC-T12.2: xoá khỏi ngân hàng không làm mất câu hỏi trong bài kiểm tra đã tạo. */
    @Transactional
    public void delete(UUID teacherId, UUID questionId) {
        Question question = requireQuestion(questionId);
        permission.requireManagedCourse(teacherId, question.getCourseId());
        question.markDeleted(teacherId);
        questionRepository.save(question);
    }

    /** Tiện ích cho module khác: id mục tiêu của một câu hỏi. */
    @Transactional(readOnly = true)
    public List<UUID> objectiveIds(UUID questionId) {
        List<UUID> ids = new ArrayList<>();
        for (QuestionObjective qo : questionObjectiveRepository.findByIdQuestionId(questionId)) {
            ids.add(qo.getObjectiveId());
        }
        return ids;
    }

    Question requireQuestion(UUID questionId) {
        return questionRepository.findByIdAndDeletedAtIsNull(questionId)
                .orElseThrow(() -> permission.notFound("Không tìm thấy câu hỏi"));
    }

    private boolean isUsedInAssessmentWithAttempts(UUID questionId) {
        for (var link : assessmentQuestionRepository.findByQuestionId(questionId)) {
            if (attemptRepository.countByAssessmentId(link.getAssessmentId()) > 0) {
                return true;
            }
        }
        return false;
    }

    private void replaceObjectives(UUID questionId, UUID courseId, List<UUID> objectiveIds) {
        questionObjectiveRepository.deleteByIdQuestionId(questionId);
        if (objectiveIds == null || objectiveIds.isEmpty()) {
            return;
        }
        Set<UUID> unique = new LinkedHashSet<>(objectiveIds);
        Set<UUID> valid = new LinkedHashSet<>();
        for (CourseContentPort.ObjectiveRef ref : contentPort.findObjectives(unique)) {
            if (ref.courseId().equals(courseId)) {
                valid.add(ref.id());
            }
        }
        if (valid.size() != unique.size()) {
            throw new BusinessException("OBJECTIVE_NOT_IN_COURSE",
                    "Có mục tiêu học tập không thuộc khóa học này", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        for (UUID objectiveId : valid) {
            questionObjectiveRepository.save(new QuestionObjective(questionId, objectiveId));
        }
    }

    private void validateChoice(QuestionType type, List<String> options, List<Integer> correct) {
        if (!type.isChoice()) {
            return;
        }
        if (options == null || options.size() < 2) {
            throw new BusinessException("QUESTION_OPTIONS_INVALID",
                    "Câu hỏi trắc nghiệm phải có ít nhất 2 phương án", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (correct == null || correct.isEmpty()) {
            throw new BusinessException("QUESTION_ANSWER_INVALID",
                    "Câu hỏi trắc nghiệm phải có đáp án đúng", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (type == QuestionType.TRUE_FALSE && options.size() != 2) {
            throw new BusinessException("QUESTION_OPTIONS_INVALID",
                    "Câu hỏi đúng/sai phải có đúng 2 phương án", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (type != QuestionType.MULTIPLE_CHOICE && correct.size() != 1) {
            throw new BusinessException("QUESTION_ANSWER_INVALID",
                    "Loại câu hỏi này chỉ được có một đáp án đúng", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        for (Integer index : correct) {
            if (index == null || index < 0 || index >= options.size()) {
                throw new BusinessException("QUESTION_ANSWER_INVALID",
                        "Chỉ số đáp án đúng không hợp lệ", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
    }

    private QuestionType parseType(String value) {
        try {
            return QuestionType.valueOf(value.trim().toUpperCase());
        } catch (RuntimeException e) {
            throw new BusinessException("INVALID_QUESTION_TYPE",
                    "Loại câu hỏi không hợp lệ: " + value, HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private DifficultyLevel parseDifficulty(String value) {
        try {
            return DifficultyLevel.valueOf(value.trim().toUpperCase());
        } catch (RuntimeException e) {
            throw new BusinessException("INVALID_DIFFICULTY",
                    "Độ khó không hợp lệ: " + value, HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    QuestionResponse toResponse(Question question) {
        return new QuestionResponse(question.getId(), question.getCourseId(), question.getContent(),
                question.getType().name(), question.getDifficulty().name(), question.getBloomLevel(),
                question.getSource().name(), codec.readStringList(question.getOptions()),
                codec.readIntList(question.getCorrectAnswer()), codec.readText(question.getCorrectAnswer()),
                objectiveIds(question.getId()), question.getCreatedAt());
    }
}
