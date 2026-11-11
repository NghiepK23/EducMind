package com.educmind.assessment.dto;

import java.util.List;

/** US-S03 AC-S03.3: lưu câu trả lời trong quá trình làm bài. */
public class SaveAnswerRequest {

    /** Trắc nghiệm: chỉ số phương án đã chọn. */
    private List<Integer> selectedOptionIndexes;

    /** Tự luận / trả lời ngắn. */
    private String text;

    public List<Integer> getSelectedOptionIndexes() {
        return selectedOptionIndexes;
    }

    public void setSelectedOptionIndexes(List<Integer> selectedOptionIndexes) {
        this.selectedOptionIndexes = selectedOptionIndexes;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
