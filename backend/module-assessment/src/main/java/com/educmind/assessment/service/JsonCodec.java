package com.educmind.assessment.service;

import com.educmind.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Chuyển đổi dữ liệu JSONB (options / correct_answer / answer_content) ↔ kiểu Java.
 * Dùng ObjectMapper tự tạo (không phụ thuộc cấu hình Jackson của ứng dụng).
 */
@Component
public class JsonCodec {

    private final ObjectMapper mapper = new ObjectMapper();

    public String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException("JSON_ENCODE_ERROR", "Không mã hoá được dữ liệu JSON",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String writeText(String value) {
        return write(value);
    }

    private JsonNode tree(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> readStringList(String json) {
        JsonNode node = tree(json);
        List<String> result = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return result;
        }
        node.forEach(item -> result.add(item.asText()));
        return result;
    }

    public List<Integer> readIntList(String json) {
        JsonNode node = tree(json);
        List<Integer> result = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return result;
        }
        node.forEach(item -> {
            if (item.isInt() || item.isLong()) {
                result.add(item.asInt());
            } else if (item.isTextual()) {
                try {
                    result.add(Integer.parseInt(item.asText().trim()));
                } catch (NumberFormatException ignored) {
                    // bỏ qua phần tử không phải số
                }
            }
        });
        return result;
    }

    public String readText(String json) {
        JsonNode node = tree(json);
        if (node == null) {
            return null;
        }
        return node.isTextual() ? node.asText() : null;
    }
}
