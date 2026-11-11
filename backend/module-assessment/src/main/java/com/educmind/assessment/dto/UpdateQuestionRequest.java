package com.educmind.assessment.dto;

import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * PATCH câu hỏi (US-T11 AC-T11.4): nội dung/đáp án chỉ sửa được khi câu hỏi chưa dùng
 * trong bài kiểm tra có lượt làm; độ khó và mức Bloom luôn sửa được.
 * null = giữ nguyên trường đó; objectiveIds null = giữ nguyên danh sách mục tiêu.
 */
public class UpdateQuestionRequest {

    private String type;

    private String difficulty;

    @Size(max = 30, message = "Mức Bloom tối đa 30 ký tự")
    private String bloomLevel;

    private String content;

    private List<String> options;

    private List<Integer> correctOptionIndexes;

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
