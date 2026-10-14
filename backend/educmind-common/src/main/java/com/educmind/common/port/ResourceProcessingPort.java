package com.educmind.common.port;

import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Cổng để module-rag đọc file của một Resource và cập nhật trạng thái xử lý,
 * mà không phụ thuộc trực tiếp vào module-course (phần cài đặt nằm ở module-course).
 *
 * Quy ước: resource không tồn tại hoặc đã bị xóa mềm thì các hàm markXxx không làm gì
 * (worker nền có thể chạy sau khi giáo viên đã xóa tài liệu), còn findResource trả rỗng.
 */
public interface ResourceProcessingPort {

    /** Thông tin cần thiết để xử lý một resource. Không có kết quả nếu resource đã bị xóa mềm. */
    Optional<ResourceInfo> findResource(UUID resourceId);

    /**
     * Mở luồng đọc nội dung file. Người gọi phải đóng luồng (try-with-resources).
     * Ném BusinessException nếu resource không tồn tại hoặc file không còn trên storage.
     */
    InputStream openContent(UUID resourceId);

    /** Trạng thái xử lý hiện tại (PROCESSING, READY, FAILED). Rỗng nếu resource không tồn tại hoặc đã xóa mềm. */
    Optional<String> findProcessingStatus(UUID resourceId);

    /** Đưa resource về PROCESSING (dùng khi xử lý lại). */
    void markProcessing(UUID resourceId);

    /**
     * Đổi sang PROCESSING để bắt đầu xử lý lại, nhưng chỉ khi resource chưa bị xóa và
     * (không đang PROCESSING, hoặc đang PROCESSING quá lâu so với staleAfter - coi như bị kẹt).
     * Thao tác nguyên tử: trả true nếu lần gọi này là lần đổi trạng thái được, false nếu không.
     */
    boolean tryBeginReprocess(UUID resourceId, Duration staleAfter);

    /** Đánh dấu xử lý xong, chunk đã sẵn sàng cho truy xuất. */
    void markReady(UUID resourceId);

    /** Đánh dấu xử lý thất bại. */
    void markFailed(UUID resourceId);

    record ResourceInfo(UUID id, UUID courseId, UUID lessonId,
                        String filename, String mimeType, long sizeBytes) {
    }
}
