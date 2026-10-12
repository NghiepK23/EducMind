package com.educmind.rag.ai;

import com.educmind.common.port.AIProviderException;
import com.educmind.common.port.EmbeddingProvider;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter gọi Gemini embedding bằng batchEmbedContents.
 * Không tự thử lại khi lỗi: việc thử lại (tối đa 3 lần, chờ tăng dần) do worker xử lý tài liệu quyết định.
 * Mọi vector trả ra đều được chuẩn hóa độ dài về 1.
 */
@Component
public class GeminiEmbeddingProvider implements EmbeddingProvider {

    private static final String PROVIDER_NAME = "Gemini";

    private final GeminiEmbeddingProperties props;
    private final RestClient restClient;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public GeminiEmbeddingProvider(GeminiEmbeddingProperties props) {
        this.props = props;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(props.getTimeoutSeconds()));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public EmbeddingResult embed(List<String> texts, Purpose purpose) {
        if (texts == null) {
            throw new IllegalArgumentException("texts không được null");
        }
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new AIProviderException("AI_AUTH_FAILED",
                    "Chưa cấu hình GEMINI_API_KEY cho embedding");
        }
        if (texts.isEmpty()) {
            return new EmbeddingResult(List.of(), PROVIDER_NAME, props.getModel(), props.getDimensions(), 0);
        }
        for (String t : texts) {
            if (t == null || t.isBlank()) {
                throw new IllegalArgumentException("Không embed được đoạn chữ rỗng");
            }
        }

        List<float[]> all = new ArrayList<>(texts.size());
        long totalLatencyMs = 0;
        int batch = Math.max(1, props.getBatchSize());
        for (int from = 0; from < texts.size(); from += batch) {
            List<String> slice = texts.subList(from, Math.min(from + batch, texts.size()));
            totalLatencyMs += embedBatch(slice, purpose, all);
        }
        return new EmbeddingResult(all, PROVIDER_NAME, props.getModel(), props.getDimensions(), totalLatencyMs);
    }

    private long embedBatch(List<String> slice, Purpose purpose, List<float[]> out) {
        String taskType = purpose == Purpose.QUERY ? "RETRIEVAL_QUERY" : "RETRIEVAL_DOCUMENT";
        String modelRef = "models/" + props.getModel();

        List<Map<String, Object>> requests = new ArrayList<>();
        for (String text : slice) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("model", modelRef);
            r.put("content", Map.of("parts", List.of(Map.of("text", text))));
            r.put("taskType", taskType);
            r.put("outputDimensionality", props.getDimensions());
            requests.add(r);
        }
        Map<String, Object> body = Map.of("requests", requests);
        String url = props.getBaseUrl() + "/models/" + props.getModel() + ":batchEmbedContents";

        long start = System.nanoTime();
        String raw;
        try {
            raw = restClient.post()
                    .uri(url)
                    .header("x-goog-api-key", props.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            throw mapHttpError(e);
        } catch (ResourceAccessException e) {
            throw new AIProviderException("AI_TIMEOUT",
                    "Không kết nối được hoặc Gemini embedding phản hồi quá chậm ("
                            + e.getMostSpecificCause().getClass().getSimpleName() + ": "
                            + e.getMostSpecificCause().getMessage() + ")", e);
        }
        long latencyMs = (System.nanoTime() - start) / 1_000_000;

        if (raw == null || raw.isBlank()) {
            throw new AIProviderException("AI_BAD_RESPONSE", "Gemini embedding trả về phản hồi rỗng");
        }
        Map<String, Object> response;
        try {
            response = jsonMapper.readValue(raw, new TypeReference<Map<String, Object>>() {
            });
        } catch (JacksonException e) {
            throw new AIProviderException("AI_BAD_RESPONSE",
                    "Không đọc được JSON từ Gemini embedding: " + truncate(raw, 200), e);
        }
        out.addAll(parse(response, slice.size()));
        return latencyMs;
    }

    /**
     * Đọc phản hồi theo cả hai dạng có thể có: "embeddings" (một mảng) hoặc "embedding" (một vector).
     * Báo lỗi rõ ràng khi cấu trúc, số lượng hoặc số chiều không khớp.
     */
    private List<float[]> parse(Map<String, Object> response, int expected) {
        List<Object> rawVectors = new ArrayList<>();
        if (response.get("embeddings") instanceof List<?> list) {
            for (Object item : list) {
                rawVectors.add(item instanceof Map<?, ?> entry ? entry.get("values") : null);
            }
        } else if (response.get("embedding") instanceof Map<?, ?> single) {
            rawVectors.add(single.get("values"));
        } else {
            throw new AIProviderException("AI_BAD_RESPONSE",
                    "Phản hồi embedding không có trường embeddings/embedding, các trường nhận được: "
                            + response.keySet());
        }

        if (rawVectors.size() != expected) {
            throw new AIProviderException("AI_BAD_RESPONSE",
                    "Gửi " + expected + " đoạn nhưng nhận " + rawVectors.size() + " vector");
        }

        int dims = props.getDimensions();
        List<float[]> result = new ArrayList<>(expected);
        for (Object raw : rawVectors) {
            if (!(raw instanceof List<?> numbers)) {
                throw new AIProviderException("AI_BAD_RESPONSE", "Một vector không có trường values dạng mảng");
            }
            if (numbers.size() != dims) {
                throw new AIProviderException("AI_BAD_RESPONSE",
                        "Vector có " + numbers.size() + " chiều, cần " + dims
                                + " (có thể tham số outputDimensionality không được nhận)");
            }
            float[] v = new float[dims];
            for (int i = 0; i < dims; i++) {
                if (!(numbers.get(i) instanceof Number n)) {
                    throw new AIProviderException("AI_BAD_RESPONSE", "Vector chứa phần tử không phải số");
                }
                v[i] = n.floatValue();
            }
            normalize(v);
            result.add(v);
        }
        return result;
    }

    private static void normalize(float[] v) {
        double sum = 0;
        for (float x : v) {
            sum += (double) x * x;
        }
        double norm = Math.sqrt(sum);
        if (norm > 0) {
            for (int i = 0; i < v.length; i++) {
                v[i] = (float) (v[i] / norm);
            }
        }
    }

    private AIProviderException mapHttpError(RestClientResponseException e) {
        int status = e.getStatusCode().value();
        String detail = truncate(e.getResponseBodyAsString(), 300);
        if (status == 429) {
            return new AIProviderException("AI_RATE_LIMITED",
                    "Gemini embedding báo vượt hạn mức (429): " + detail, e);
        }
        if (status == 401 || status == 403) {
            return new AIProviderException("AI_AUTH_FAILED",
                    "Gemini embedding từ chối khóa API (" + status + "): " + detail, e);
        }
        if (status >= 500) {
            return new AIProviderException("AI_UNAVAILABLE",
                    "Gemini embedding đang lỗi (" + status + "): " + detail, e);
        }
        return new AIProviderException("AI_BAD_RESPONSE",
                "Gemini embedding từ chối yêu cầu (" + status + "): " + detail, e);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
