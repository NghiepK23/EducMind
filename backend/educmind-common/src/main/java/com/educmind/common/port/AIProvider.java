package com.educmind.common.port;

import java.util.Map;

/**
 * Cổng (port) gọi mô hình AI. Module nghiệp vụ chỉ biết interface này,
 * còn chi tiết nhà cung cấp (Gemini, sau này có thể OpenAI) nằm ở adapter.
 *
 * Khi lỗi (hết hạn mức, sai key, nhà cung cấp lỗi, quá thời gian chờ),
 * adapter ném AIProviderException với mã lỗi (AI_RATE_LIMITED, AI_AUTH_FAILED, AI_TIMEOUT...), không để lộ lỗi HTTP thô.
 */
public interface AIProvider {

    /**
     * Gọi AI một lần và nhận kết quả.
     */
    AIResult generate(AIRequest request);

    /**
     * @param systemPrompt   chỉ dẫn vai trò/quy tắc cho AI (có thể null)
     * @param userPrompt     nội dung yêu cầu cụ thể
     * @param responseSchema nếu khác null, AI phải trả JSON đúng schema này
     *                       (kiểu viết hoa: "OBJECT", "STRING", "ARRAY"...); null nghĩa là trả văn bản tự do
     */
    record AIRequest(String systemPrompt, String userPrompt, Map<String, Object> responseSchema) {
    }

    /**
     * @param text         nội dung AI trả về (JSON dạng chuỗi nếu có responseSchema)
     * @param provider     tên nhà cung cấp, ví dụ "Gemini" (ghi vào ai_observability_logs.provider)
     * @param model        tên model thực tế đã chạy (ghi vào ai_observability_logs.model)
     * @param inputTokens  số token đầu vào
     * @param outputTokens số token đầu ra, đã gồm cả token "suy nghĩ" nếu có
     * @param latencyMs    thời gian gọi, tính bằng mili giây
     */
    record AIResult(String text, String provider, String model,
                    int inputTokens, int outputTokens, long latencyMs) {
    }
}
