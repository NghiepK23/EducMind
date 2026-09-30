package com.educmind.assessment.dto;

import com.educmind.assessment.entity.CognitiveLevel;
import com.educmind.assessment.entity.Difficulty;
import com.educmind.assessment.entity.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class CreateQuestionRequest {

    @NotBlank(message = "questionText không được để trống")
    private String questionText;

    @NotNull(message = "questionType không được để trống")
    private QuestionType questionType;

    @NotNull(message = "difficulty không được để trống")
    private Difficulty difficulty;

    private CognitiveLevel cognitiveLevel;

    private UUID learningObjectiveId;

    private UUID lessonId;

    private Object options;

    private Object correctAnswer;

    private String explanation;

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public CognitiveLevel getCognitiveLevel() {
        return cognitiveLevel;
    }

    public void setCognitiveLevel(CognitiveLevel cognitiveLevel) {
        this.cognitiveLevel = cognitiveLevel;
    }

    public UUID getLearningObjectiveId() {
        return learningObjectiveId;
    }

    public void setLearningObjectiveId(UUID learningObjectiveId) {
        this.learningObjectiveId = learningObjectiveId;
    }

    public UUID getLessonId() {
        return lessonId;
    }

    public void setLessonId(UUID lessonId) {
        this.lessonId = lessonId;
    }

    public Object getOptions() {
        return options;
    }

    public void setOptions(Object options) {
        this.options = options;
    }

    public Object getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(Object correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
