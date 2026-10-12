package com.educmind.common.port;

import java.util.List;

/**
 * Cổng (port) sinh embedding (vector biểu diễn ngữ nghĩa của văn bản).
 * Tách riêng khỏi AIProvider vì đầu vào/đầu ra khác hẳn: nhận nhiều đoạn chữ, trả về nhiều vector số.
 *
 * Khi lỗi, adapter ném AIProviderException với mã lỗi
 * (AI_RATE_LIMITED, AI_AUTH_FAILED, AI_UNAVAILABLE, AI_BAD_RESPONSE, AI_TIMEOUT), không để lộ lỗi HTTP thô.
 */
public interface EmbeddingProvider {

    /**
     * Sinh embedding cho một danh sách đoạn chữ.
     * Kết quả có cùng số phần tử và cùng thứ tự với texts.
     */
    EmbeddingResult embed(List<String> texts, Purpose purpose);

    /**
     * Mục đích sử dụng vector: model embedding thường tối ưu khác nhau cho tài liệu và câu hỏi.
     * DOCUMENT: đoạn tài liệu sẽ được lưu để tìm kiếm.
     * QUERY: câu hỏi/từ khóa dùng để đi tìm đoạn tài liệu.
     */
    enum Purpose {
        DOCUMENT,
        QUERY
    }

    /**
     * @param vectors    mỗi phần tử là vector của một đoạn chữ, đã chuẩn hóa độ dài về 1
     * @param provider   tên nhà cung cấp, ví dụ "Gemini"
     * @param model      tên model thực tế đã chạy (ghi vào document_chunks.embedding_model)
     * @param dimensions số chiều của mỗi vector
     * @param latencyMs  thời gian gọi, tính bằng mili giây
     */
    record EmbeddingResult(List<float[]> vectors, String provider, String model,
                           int dimensions, long latencyMs) {
    }
}
