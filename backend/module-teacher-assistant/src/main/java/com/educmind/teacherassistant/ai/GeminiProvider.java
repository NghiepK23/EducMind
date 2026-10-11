package com.educmind.teacherassistant.ai;

import com.educmind.common.port.AIProvider;
import com.educmind.common.port.AIProviderException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter gọi Gemini bằng generateContent.
 * Key gửi qua header x-goog-api-key (không đặt trên URL), đọc từ cấu hình educmind.ai.gemini.api-key (env GEMINI_API_KEY).
 */
@Component
public class GeminiProvider implements AIProvider {

    private static final String PROVIDER_NAME = "Gemini";

    private final GeminiProperties props;
    private final RestClient restClient;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public GeminiProvider(GeminiProperties props) {
        this.props = props;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(props.getTimeoutSeconds()));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public AIResult generate(AIRequest request) {
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new AIProviderException("AI_AUTH_FAILED",
                    "Chưa cấu hình GEMINI_API_KEY cho ứng dụng");
        }

        Map<String, Object> body = buildBody(request);
        String url = props.getBaseUrl() + "/models/" + props.getModel() + ":generateContent";

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
                    "Không kết nối được hoặc Gemini phản hồi quá chậm", e);
        }
        long latencyMs = (System.nanoTime() - start) / 1_000_000;

        if (raw == null || raw.isBlank()) {
            throw new AIProviderException("AI_BAD_RESPONSE", "Gemini trả về phản hồi rỗng");
        }
        Map<String, Object> response;
        try {
            response = jsonMapper.readValue(raw, new TypeReference<Map<String, Object>>() {
            });
        } catch (JacksonException e) {
            throw new AIProviderException("AI_BAD_RESPONSE",
                    "Không đọc được JSON từ Gemini: " + truncate(raw, 200), e);
        }
        return parse(response, latencyMs);
    }

    private Map<String, Object> buildBody(AIRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();

        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            body.put("systemInstruction",
                    Map.of("parts", List.of(Map.of("text", request.systemPrompt()))));
        }

        body.put("contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", request.userPrompt())))));

        if (request.responseSchema() != null) {
            Map<String, Object> generationConfig = new LinkedHashMap<>();
            generationConfig.put("responseMimeType", "application/json");
            generationConfig.put("responseSchema", request.responseSchema());
            body.put("generationConfig", generationConfig);
        }
        return body;
    }

    private AIProviderException mapHttpError(RestClientResponseException e) {
        int status = e.getStatusCode().value();
        String detail = truncate(e.getResponseBodyAsString(), 300);
        if (status == 429) {
            return new AIProviderException("AI_RATE_LIMITED",
                    "Gemini báo vượt hạn mức (429): " + detail, e);
        }
        if (status == 401 || status == 403) {
            return new AIProviderException("AI_AUTH_FAILED",
                    "Gemini từ chối khóa API (" + status + "): " + detail, e);
        }
        if (status >= 500) {
            return new AIProviderException("AI_UNAVAILABLE",
                    "Gemini đang lỗi (" + status + "): " + detail, e);
        }
        return new AIProviderException("AI_BAD_RESPONSE",
                "Gemini từ chối yêu cầu (" + status + "): " + detail, e);
    }

    @SuppressWarnings("unchecked")
    private AIResult parse(Map<String, Object> response, long latencyMs) {
        StringBuilder text = new StringBuilder();
        String finishReason = null;

        Object candidatesObj = response.get("candidates");
        if (candidatesObj instanceof List<?> candidates && !candidates.isEmpty()
                && candidates.get(0) instanceof Map<?, ?> first) {
            Map<String, Object> candidate = (Map<String, Object>) first;
            Object fr = candidate.get("finishReason");
            finishReason = fr == null ? null : fr.toString();
            Object contentObj = candidate.get("content");
            if (contentObj instanceof Map<?, ?> content
                    && ((Map<String, Object>) content).get("parts") instanceof List<?> parts) {
                for (Object partObj : parts) {
                    if (partObj instanceof Map<?, ?> part && part.get("text") instanceof String t) {
                        text.append(t);
                    }
                }
            }
        }

        if (text.toString().isBlank()) {
            throw new AIProviderException("AI_BAD_RESPONSE",
                    "Gemini không trả nội dung (finishReason=" + finishReason + ")");
        }

        int inputTokens = 0;
        int outputTokens = 0;
        if (response.get("usageMetadata") instanceof Map<?, ?> usage) {
            inputTokens = number(usage.get("promptTokenCount"));
            outputTokens = number(usage.get("candidatesTokenCount")) + number(usage.get("thoughtsTokenCount"));
        }

        Object mv = response.get("modelVersion");
        String model = mv instanceof String s ? s : props.getModel();

        return new AIResult(text.toString(), PROVIDER_NAME, model, inputTokens, outputTokens, latencyMs);
    }

    private static int number(Object o) {
        return o instanceof Number n ? n.intValue() : 0;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
