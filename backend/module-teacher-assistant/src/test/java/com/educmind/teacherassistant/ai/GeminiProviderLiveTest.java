package com.educmind.teacherassistant.ai;

import com.educmind.common.port.AIProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test gọi thật Gemini. Chỉ chạy khi máy có biến môi trường GEMINI_API_KEY.
 */
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
class GeminiProviderLiveTest {

    private GeminiProvider newProvider() {
        GeminiProperties props = new GeminiProperties();
        props.setApiKey(System.getenv("GEMINI_API_KEY"));
        return new GeminiProvider(props);
    }

    @Test
    void goiVanBanThuong() {
        AIProvider.AIResult r = newProvider().generate(new AIProvider.AIRequest(
                "Bạn là trợ lý giáo viên Tin học. Trả lời ngắn gọn bằng tiếng Việt.",
                "Biến trong lập trình là gì? Trả lời đúng 1 câu.",
                null));
        System.out.println("[TEXT] model=" + r.model() + " in=" + r.inputTokens()
                + " out=" + r.outputTokens() + " ms=" + r.latencyMs());
        System.out.println("[TEXT] " + r.text());
        assertFalse(r.text().isBlank());
        assertTrue(r.outputTokens() > 0);
    }

    @Test
    void goiJsonTheoSchema() {
        Map<String, Object> schema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "cau_hoi", Map.of("type", "STRING"),
                        "dap_an", Map.of("type", "STRING")),
                "required", List.of("cau_hoi", "dap_an"));
        AIProvider.AIResult r = newProvider().generate(new AIProvider.AIRequest(
                "Bạn là trợ lý giáo viên Tin học.",
                "Viết 1 câu hỏi ngắn về vòng lặp for và đáp án.",
                schema));
        System.out.println("[JSON] " + r.text());
        assertTrue(r.text().trim().startsWith("{"));
        assertTrue(r.text().contains("cau_hoi"));
    }
}
