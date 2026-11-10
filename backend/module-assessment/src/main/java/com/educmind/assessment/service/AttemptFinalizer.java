package com.educmind.assessment.service;

import com.educmind.assessment.entity.Assessment;
import com.educmind.assessment.entity.AssessmentAttempt;
import com.educmind.assessment.entity.AssessmentQuestion;
import com.educmind.assessment.entity.Question;
import com.educmind.assessment.entity.QuestionObjective;
import com.educmind.assessment.entity.QuestionType;
import com.educmind.assessment.entity.SubmissionAnswer;
import com.educmind.assessment.repository.AssessmentQuestionRepository;
import com.educmind.assessment.repository.QuestionObjectiveRepository;
import com.educmind.assessment.repository.QuestionRepository;
import com.educmind.assessment.repository.SubmissionAnswerRepository;
import com.educmind.common.port.LearningEvidencePort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Chấm tự động + chốt lượt làm bài kiểm tra.
 *
 * AC-S03.3: mọi câu đều có dòng trả lời (kể cả bỏ trống) để chấm và tính điểm.
 * AC-S03.4: trắc nghiệm chấm tự động; trả lời ngắn/tự luận chờ giáo viên.
 * AC-S03.5/6/7: khi lượt làm được chấm xong mới ghi bằng chứng, chia đều điểm câu
 *            gắn nhiều mục tiêu, bỏ qua câu không gắn mục tiêu.
 */
@Component
public class AttemptFinalizer {

    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionObjectiveRepository questionObjectiveRepository;
    private final SubmissionAnswerRepository answerRepository;
    private final LearningEvidencePort evidencePort;
    private final JsonCodec codec;

    public AttemptFinalizer(AssessmentQuestionRepository assessmentQuestionRepository,
                            QuestionRepository questionRepository,
                            QuestionObjectiveRepository questionObjectiveRepository,
                            SubmissionAnswerRepository answerRepository,
                            LearningEvidencePort evidencePort,
                            JsonCodec codec) {
        this.assessmentQuestionRepository = assessmentQuestionRepository;
        this.questionRepository = questionRepository;
        this.questionObjectiveRepository = questionObjectiveRepository;
        this.answerRepository = answerRepository;
        this.evidencePort = evidencePort;
        this.codec = codec;
    }

    /** Bảo đảm mỗi câu của bài kiểm tra có đúng một dòng trả lời. */
    public void ensureAnswersExist(UUID attemptId, UUID assessmentId) {
        Set<UUID> existing = new HashSet<>();
        for (SubmissionAnswer answer : answerRepository.findByAttemptIdOrderByQuestionIdAsc(attemptId)) {
            existing.add(answer.getQuestionId());
        }
        for (AssessmentQuestion link : assessmentQuestionRepository.findByAssessmentIdOrderByIdAsc(assessmentId)) {
            if (!existing.contains(link.getQuestionId())) {
                answerRepository.save(new SubmissionAnswer(attemptId, link.getQuestionId(), null));
            }
        }
    }

    /** Chấm tự động các câu trắc nghiệm (chưa chấm). */
    public void autoGradeChoiceAnswers(UUID attemptId, UUID assessmentId) {
        Map<UUID, BigDecimal> weights = new HashMap<>();
        for (AssessmentQuestion link : assessmentQuestionRepository.findByAssessmentIdOrderByIdAsc(assessmentId)) {
            weights.put(link.getQuestionId(), link.getWeight());
        }

        for (SubmissionAnswer answer : answerRepository.findByAttemptIdOrderByQuestionIdAsc(attemptId)) {
            if (answer.isGraded()) {
                continue;
            }
            Question question = questionRepository.findById(answer.getQuestionId()).orElse(null);
            if (question == null || !question.getType().isChoice()) {
                continue;
            }
            BigDecimal weight = weights.getOrDefault(answer.getQuestionId(), BigDecimal.ZERO);
            boolean correct = isCorrect(question, answer.getAnswerContent());
            answer.autoGrade(correct ? weight : BigDecimal.ZERO);
            answerRepository.save(answer);
        }
    }

    /**
     * Nếu mọi câu đều đã có điểm → chuyển lượt làm sang GRADED, tính tổng điểm và ghi bằng chứng.
     * Trả về true nếu lượt làm đã chấm xong.
     */
    public boolean finalizeIfComplete(AssessmentAttempt attempt, Assessment assessment) {
        List<SubmissionAnswer> answers = answerRepository.findByAttemptIdOrderByQuestionIdAsc(attempt.getId());
        List<AssessmentQuestion> links =
                assessmentQuestionRepository.findByAssessmentIdOrderByIdAsc(assessment.getId());

        boolean allGraded = !links.isEmpty();
        for (AssessmentQuestion link : links) {
            Optional<SubmissionAnswer> answer = answers.stream()
                    .filter(a -> a.getQuestionId().equals(link.getQuestionId()))
                    .findFirst();
            if (answer.isEmpty() || !answer.get().isGraded()) {
                allGraded = false;
                break;
            }
        }

        if (!allGraded) {
            if (attempt.getSubmittedAt() == null) {
                attempt.markSubmitted(OffsetDateTime.now());
            }
            return false;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (SubmissionAnswer answer : answers) {
            if (answer.getScore() != null) {
                total = total.add(answer.getScore());
            }
        }
        attempt.markGraded(total, OffsetDateTime.now());
        recordEvidence(attempt, links, answers);
        return true;
    }

    /** AC-S03.5/6/7. */
    private void recordEvidence(AssessmentAttempt attempt, List<AssessmentQuestion> links,
                                List<SubmissionAnswer> answers) {
        Map<UUID, SubmissionAnswer> byQuestion = new LinkedHashMap<>();
        for (SubmissionAnswer answer : answers) {
            byQuestion.put(answer.getQuestionId(), answer);
        }

        Map<UUID, double[]> accumulator = new LinkedHashMap<>(); // objectiveId -> [điểm đạt, trọng số]

        for (AssessmentQuestion link : links) {
            List<UUID> objectives = new ArrayList<>();
            for (QuestionObjective qo : questionObjectiveRepository.findByIdQuestionId(link.getQuestionId())) {
                objectives.add(qo.getObjectiveId());
            }
            if (objectives.isEmpty()) {
                continue;
            }
            SubmissionAnswer answer = byQuestion.get(link.getQuestionId());
            double score = answer == null || answer.getScore() == null ? 0d : answer.getScore().doubleValue();
            double weight = link.getWeight().doubleValue();
            double share = objectives.size();

            for (UUID objectiveId : objectives) {
                double[] acc = accumulator.computeIfAbsent(objectiveId, k -> new double[]{0d, 0d});
                acc[0] += score / share;
                acc[1] += weight / share;
            }
        }

        for (Map.Entry<UUID, double[]> entry : accumulator.entrySet()) {
            double[] acc = entry.getValue();
            if (acc[1] <= 0) {
                continue;
            }
            double ratio = acc[0] / acc[1];
            evidencePort.recordHighest(attempt.getStudentId(), entry.getKey(),
                    LearningEvidencePort.SourceType.ASSESSMENT, attempt.getAssessmentId(), ratio);
        }
    }

    private boolean isCorrect(Question question, String answerContent) {
        List<Integer> selected = codec.readIntList(answerContent);
        List<Integer> correct = codec.readIntList(question.getCorrectAnswer());
        if (correct.isEmpty()) {
            return false;
        }
        if (question.getType() == QuestionType.MULTIPLE_CHOICE) {
            // AC-T05.4: chỉ đúng khi chọn đúng và đủ tất cả đáp án
            return selected.size() == correct.size() && new HashSet<>(selected).equals(new HashSet<>(correct));
        }
        return selected.size() == 1 && selected.get(0).equals(correct.get(0));
    }
}
