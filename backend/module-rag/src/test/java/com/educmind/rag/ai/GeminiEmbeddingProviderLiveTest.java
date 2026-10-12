package com.educmind.rag.ai;

import com.educmind.common.port.EmbeddingProvider;
import com.educmind.common.port.EmbeddingProvider.EmbeddingResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test gọi Gemini THẬT (tốn một ít hạn mức). Chỉ chạy khi có biến môi trường GEMINI_API_KEY.
 * Chạy riêng: ./mvnw -pl module-rag test -Dtest=GeminiEmbeddingProviderLiveTest
 */
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
class GeminiEmbeddingProviderLiveTest {

    private GeminiEmbeddingProvider newProvider() {
        GeminiEmbeddingProperties props = new GeminiEmbeddingProperties();
        props.setApiKey(System.getenv("GEMINI_API_KEY"));
        return new GeminiEmbeddingProvider(props);
    }

    private static double dot(float[] a, float[] b) {
        double s = 0;
        for (int i = 0; i < a.length; i++) {
            s += (double) a[i] * b[i];
        }
        return s;
    }

    @Test
    void embedDocuments_returnsNormalizedVectorsWithConfiguredDimensions() {
        EmbeddingResult result = newProvider().embed(List.of(
                "Dữ liệu là các con số, văn bản, hình ảnh, âm thanh được lưu trữ và xử lý trong máy tính.",
                "Hôm nay trời nắng đẹp, em đi chơi ở công viên cùng các bạn."
        ), EmbeddingProvider.Purpose.DOCUMENT);

        assertEquals(2, result.vectors().size());
        assertEquals(768, result.dimensions());
        assertFalse(result.model().isBlank());
        for (float[] v : result.vectors()) {
            assertEquals(768, v.length);
            assertEquals(1.0, Math.sqrt(dot(v, v)), 1e-3);
        }
        System.out.println("[LIVE] model=" + result.model() + ", dims=" + result.dimensions()
                + ", latencyMs=" + result.latencyMs());
    }

    @Test
    void embedQuery_printsSimilarityToRelatedAndUnrelatedDocument() {
        GeminiEmbeddingProvider provider = newProvider();
        EmbeddingResult docs = provider.embed(List.of(
                "Dữ liệu là các con số, văn bản, hình ảnh, âm thanh được lưu trữ và xử lý trong máy tính.",
                "Hôm nay trời nắng đẹp, em đi chơi ở công viên cùng các bạn."
        ), EmbeddingProvider.Purpose.DOCUMENT);
        EmbeddingResult query = provider.embed(List.of("Dữ liệu trong máy tính là gì?"),
                EmbeddingProvider.Purpose.QUERY);

        assertEquals(1, query.vectors().size());
        double related = dot(query.vectors().get(0), docs.vectors().get(0));
        double unrelated = dot(query.vectors().get(0), docs.vectors().get(1));
        System.out.println("[LIVE] similarity related=" + related + ", unrelated=" + unrelated);
        assertTrue(related > -1.0 && related <= 1.0001);
    }
}
