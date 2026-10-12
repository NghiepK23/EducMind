package com.educmind.rag.entity;

/** Các giai đoạn xử lý một tài liệu. Cột stage trong DB không có CHECK, giá trị được kiểm ở code. */
public enum JobStage {
    PARSE,
    CHUNKING,
    EMBEDDING
}
