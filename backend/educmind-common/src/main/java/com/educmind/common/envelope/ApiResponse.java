package com.educmind.common.envelope;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Envelope chuẩn cho mọi response của API — {success, data, error, meta}.
 * Dùng xuyên suốt toàn bộ Module 1-10 theo đúng format đã thống nhất trong spec.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorDetail error;
    private final Map<String, Object> meta;

    private ApiResponse(boolean success, T data, ErrorDetail error, Map<String, Object> meta) {
        this.success = success;
        this.data = data;
        this.error = error;
        this.meta = meta;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, new HashMap<>());
    }

    public static <T> ApiResponse<T> success(T data, Map<String, Object> meta) {
        return new ApiResponse<>(true, data, null, meta);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorDetail(code, message), new HashMap<>());
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public ErrorDetail getError() {
        return error;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }
}
