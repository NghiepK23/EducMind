package com.educmind.assignment.dto;

/** US-S02: nội dung văn bản và/hoặc tệp đính kèm (AC-S02.3). */
public class SubmitRequest {

    private String textContent;

    private String fileUrl;

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
}
