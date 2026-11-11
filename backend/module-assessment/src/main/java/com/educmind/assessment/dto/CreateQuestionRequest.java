package com.educmind.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** US-T05 (AC-T05.1 … AC-T05.6). */
public class CreateQuestionRequest {

    @NotBlank(message = "Loại câu hỏi không được để trống")
    private String type;

    @NotBlank(message = "Độ khó không được để trống")
    private String difficulty;

    @Size(max = 30, message = "Mức Bloom tối đa 30 ký tự")
    private String bloomLevel;

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    private String content;

    /** Phương án trả lời (chỉ với câu trắc nghiệm). */
    private List<String> options;

    /** Chỉ số các phương án đúng (SINGLE_CHOICE/TRUE_FALSE: đúng 1; MULTIPLE_CHOICE: ≥ 1). */
    private List<Integer> correctOptionIndexes;

    /** Đáp án tham khảo cho câu trả lời ngắn (không tự chấm). */
    private String expectedAnswer;

    private List<UUID> objectiveIds;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getBloomLevel() {
        return bloomLevel;
    }

    public void setBloomLevel(String bloomLevel) {
        this.bloomLevel = bloomLevel;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public List<Integer> getCorrectOptionIndexes() {
        return correctOptionIndexes;
    }

    public void setCorrectOptionIndexes(List<Integer> correctOptionIndexes) {
        this.correctOptionIndexes = correctOptionIndexes;
    }

    public String getExpectedAnswer() {
        return expectedAnswer;
    }

    public void setExpectedAnswer(String expectedAnswer) {
        this.expectedAnswer = expectedAnswer;
    }

    public List<UUID> getObjectiveIds() {
        return objectiveIds;
    }

    public void setObjectiveIds(List<UUID> objectiveIds) {
        this.objectiveIds = objectiveIds;
    }
}
