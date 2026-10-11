package com.educmind.common.port;

/**
 * Lỗi khi gọi AI provider. code gồm: AI_RATE_LIMITED, AI_AUTH_FAILED, AI_UNAVAILABLE, AI_BAD_RESPONSE, AI_TIMEOUT.
 */
public class AIProviderException extends RuntimeException {

    private final String code;

    public AIProviderException(String code, String message) {
        super(message);
        this.code = code;
    }

    public AIProviderException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
