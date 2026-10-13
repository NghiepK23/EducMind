package com.educmind.common.port;

import java.util.List;
import java.util.UUID;

/**
 * Cổng truy xuất đoạn tài liệu liên quan cho các module AI (Module 7, 8, 9).
 * Chỉ trả đoạn thuộc đúng courseId (và lessonId nếu có), từ resource chưa bị xóa mềm.
 * Người gọi phải tự kiểm tra quyền truy cập course trước khi gọi.
 */
public interface RetrievalPort {

    /**
     * @param courseId bắt buộc, giới hạn phạm vi truy xuất
     * @param lessonId có thể null; nếu có thì chỉ lấy đoạn của lesson đó
     * @param query    câu hỏi hoặc từ khóa tự nhiên
     * @return danh sách đã sắp theo độ liên quan giảm dần, rỗng nếu không có gì phù hợp
     */
    List<RetrievedChunk> search(UUID courseId, UUID lessonId, String query);

    record RetrievedChunk(UUID chunkId, UUID resourceId, UUID lessonId,
                          int chunkIndex, String content, double score) {
    }
}
